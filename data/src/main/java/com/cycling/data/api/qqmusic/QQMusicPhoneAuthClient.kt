package com.cycling.data.api.qqmusic

import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

/**
 * QQ 音乐的手机号验证码登录（`music.login.LoginServer`）。
 *
 * 扫码之外的第二条登录通道。它比扫码短：发码 → 提交验证码 → 拿到 `musicKey`，
 * 中间没有 OAuth 跳转，也不需要 WebView。
 *
 * 两个协议要点：
 *
 * 1. **Cookie 要串起来**：`SendPhoneAuthCode` 会在 `Set-Cookie` 里下发会话票据，
 *    `Login` 必须带着它，否则服务端认为是一次陌生的登录尝试。所以本类自己维护一份
 *    `requestCookie`，而不是每次现拼。
 * 2. **风控是一种「成功响应」**：账号异常时接口仍回 HTTP 200，业务码 `20276`（或带
 *    `securityURL`），意思是「去网页做安全验证」。这时不能当成网络错误，要让界面把
 *    验证地址交出去（见 [QQMusicSecurityChallengeException]）。
 *
 * 同一时间只允许一次登录流程（[cookieMutex]）：`requestCookie` 是共享状态，
 * 两个并发登录会互相覆盖。
 */
@Singleton
class QQMusicPhoneAuthClient @Inject constructor(
    sharedClient: OkHttpClient
) {

    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // 登录链路是「同一 Cookie 罐里的一串请求」，命中磁盘缓存会把状态冻在上一次的结果上。
        .cache(null)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private val cookieMutex = Mutex()
    private var requestCookie = ""

    /**
     * 给 [phone] 发一条验证码。
     *
     * @throws IOException 服务端拒绝（号码格式、频控、风控）或网络失败。
     */
    suspend fun sendCode(phone: String) {
        requireMainlandPhone(phone)
        cookieMutex.withLock {
            withContext(Dispatchers.IO) {
                val response = post(buildSendCodePayload(phone))
                requestCookie = mergeCookies(requestCookie, response.setCookieHeaders)
                // 发码不要求会话：外层成功即可，服务端把票据放在 Set-Cookie 里。
                requestCookie = parseAuthResponse(
                    response = response.body,
                    existingCookie = requestCookie,
                    requireSession = false
                )
            }
        }
    }

    /**
     * 用验证码换登录态。
     *
     * @return 可直接落盘的会话（Cookie 已含 `qqmusic_uin` 与 `qm_keyst`）。
     * @throws QQMusicSecurityChallengeException 账号被要求先完成网页安全验证。
     */
    suspend fun login(phone: String, code: String): QQMusicSession {
        requireMainlandPhone(phone)
        if (code.isBlank()) throw IOException("请输入短信验证码")
        return cookieMutex.withLock {
            withContext(Dispatchers.IO) {
                val response = post(buildLoginPayload(phone, code))
                requestCookie = mergeCookies(requestCookie, response.setCookieHeaders)
                val cookie = parseAuthResponse(
                    response = response.body,
                    existingCookie = requestCookie,
                    requireSession = true
                )
                requestCookie = cookie
                parseSessionCookie(cookie)
                    ?: throw IOException("QQ 音乐登录凭据不完整，请重新获取验证码")
            }
        }
    }

    /** 换号 / 重新登录前清掉上一条会话的票据，避免带着别人的 Cookie 发码。 */
    suspend fun reset() {
        cookieMutex.withLock { requestCookie = "" }
    }

    private fun post(payload: JsonObject): PhoneHttpResponse {
        val request = Request.Builder()
            .url(MUSICU_URL)
            .header("User-Agent", "QQMusic $CLIENT_VERSION(android 15)")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .apply { requestCookie.takeIf(String::isNotBlank)?.let { header("Cookie", it) } }
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()

        return client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("QQ 音乐登录请求失败：HTTP ${response.code}")
            val body = runCatching { json.parseToJsonElement(text).jsonObject }
                .getOrElse { throw IOException("QQ 音乐登录返回了无法解析的响应", it) }
            PhoneHttpResponse(body, response.headers.values("Set-Cookie"))
        }
    }

    /**
     * 解析一次登录应答。
     *
     * @param requireSession `false` 时只要求外层成功（发码），`true` 时还要求拿到账号与密钥。
     * @return 合并后的 Cookie 串。
     */
    private fun parseAuthResponse(
        response: JsonObject,
        existingCookie: String,
        requireSession: Boolean
    ): String {
        val outerCode = response.int("code") ?: throw IOException("QQ 音乐响应缺少状态码")
        if (outerCode != 0) throw IOException(response.errorMessage("QQ 音乐请求失败（$outerCode）"))

        val module = response.findLoginModule() ?: throw IOException("QQ 音乐响应缺少登录结果")
        val businessCode = module.int("code") ?: throw IOException("QQ 音乐登录结果缺少状态码")
        val data = module.obj("data") ?: JsonObject(emptyMap())
        if (businessCode != 0) {
            val securityUrl = data.string("securityURL").ifBlank { module.string("securityURL") }
            if (businessCode == SECURITY_CHALLENGE_CODE || securityUrl.isNotBlank()) {
                // 只放行 https：这个地址要交给浏览器打开，http 版本会被系统的明文策略挡掉。
                val safeUrl = securityUrl.takeIf { it.startsWith("https://", ignoreCase = true) }
                if (safeUrl != null) throw QQMusicSecurityChallengeException(safeUrl)
                throw IOException("QQ 音乐要求安全验证，请改用扫码登录")
            }
            throw IOException(module.errorMessage(response.errorMessage("QQ 音乐请求失败（$businessCode）"), data))
        }
        if (!requireSession) return existingCookie

        // 凭据可能只在响应体里（没有对应 Cookie），也可能只在 Cookie 里，两边都查。
        val credentialObjects = buildList {
            add(data)
            listOf("data", "result").forEach { name -> data.obj(name)?.let { add(it) } }
        }
        val cookies = cookieValues(existingCookie).toMutableMap()
        val uin = cookies.firstMatchingValue(::isUsableUin, "qqmusic_uin", "uin")
            ?: credentialObjects.firstMatchingValue(::isUsableUin, "musicid", "str_musicid", "encryptUin")
            ?: throw IOException("登录响应缺少有效 QQ 音乐账号标识，请改用扫码登录")
        val musicKey = cookies.valueFor("qm_keyst", "qqmusic_key", "musickey")
            ?.takeIf(String::isNotBlank)
            ?: credentialObjects.firstValue("musickey", "qm_keyst", "qqmusic_key").takeIf(String::isNotBlank)
            ?: throw IOException("登录响应缺少 QQ 音乐音乐密钥，请改用扫码登录")

        cookies.putCanonical("qqmusic_uin", uin)
        cookies.putCanonical("qm_keyst", musicKey)
        // 手机号登录走的是 QQ 号体系（login_type=2），与微信扫码（1）不同。
        cookies.putCanonical("login_type", "2")
        listOf("token", "refresh_key").forEach { name ->
            credentialObjects.firstValue(name).takeIf(String::isNotBlank)
                ?.let { cookies.putCanonical(name, it) }
        }
        Timber.d("QQMusicPhoneAuth: 登录成功 uin=%s", uin)
        return normalizeCookieHeader(cookies)
    }

    /**
     * 在几层包装里找 `music.login.LoginServer` 模块。
     *
     * 服务端有时把它放在顶层，有时包进 `data` / `response` / `result`；只认一层会漏。
     */
    private fun JsonObject.findLoginModule(): JsonObject? {
        // 这里不能用 `mapNotNull(::obj)`：`obj` 既是成员又是扩展，Kotlin 不允许对这类元素取函数引用。
        val firstLevel = listOf(this) +
            listOf("data", "response", "result").mapNotNull { name -> obj(name) }
        firstLevel.forEach { root -> root.obj(LOGIN_MODULE)?.let { return it } }
        firstLevel.drop(1).forEach { wrapper ->
            listOf("data", "response", "result").forEach { name ->
                wrapper.obj(name)?.obj(LOGIN_MODULE)?.let { return it }
            }
        }
        return null
    }

    private fun buildSendCodePayload(phone: String): JsonObject = buildPhonePayload(
        method = "SendPhoneAuthCode",
        param = buildJsonObject {
            put("tmeAppid", "qqmusic")
            put("phoneNo", phone)
            put("areaCode", AREA_CODE)
        }
    )

    private fun buildLoginPayload(phone: String, code: String): JsonObject = buildPhonePayload(
        method = "Login",
        param = buildJsonObject {
            put("code", code)
            put("phoneNo", phone)
            put("loginMode", 1)
        }
    )

    private fun buildPhonePayload(method: String, param: JsonObject): JsonObject = buildJsonObject {
        put(
            "comm",
            buildJsonObject {
                put("ct", 24)
                put("cv", CLIENT_VERSION)
                put("v", CLIENT_VERSION)
                put("format", "json")
            }
        )
        put(
            LOGIN_MODULE,
            buildJsonObject {
                put("module", LOGIN_MODULE)
                put("method", method)
                put("param", param)
            }
        )
    }

    /** 只支持中国大陆号码：接口的 `areaCode` 是硬编码的 `86`，别的区号发出去也收不到码。 */
    private fun requireMainlandPhone(phone: String) {
        require(phone.length == MAINLAND_PHONE_LENGTH && phone.all(Char::isDigit)) {
            "请输入 11 位中国大陆手机号"
        }
    }

    /** `Set-Cookie` → Cookie 串：过期项要删掉，同名项以服务端最后一次下发的为准。 */
    private fun mergeCookies(cookieHeader: String, setCookieHeaders: List<String>): String {
        val values = cookieValues(cookieHeader).toMutableMap()
        val url = MUSICU_URL.toHttpUrl()
        setCookieHeaders.forEach { header ->
            val firstPair = header.substringBefore(';').split('=', limit = 2)
            if (firstPair.size != 2 || firstPair[0].isBlank()) return@forEach
            val name = firstPair[0].trim()
            val parsed = Cookie.parse(url, header)
            if (parsed == null || parsed.value.isEmpty() || parsed.expiresAt <= System.currentTimeMillis()) {
                values.removeCaseInsensitive(name)
            } else {
                values.putCanonical(parsed.name, parsed.value)
            }
        }
        return normalizeCookieHeader(values)
    }

    private fun cookieValues(cookie: String): Map<String, String> = buildMap {
        cookie.split(';').forEach { item ->
            val pair = item.trim().split('=', limit = 2)
            if (pair.size == 2 && pair[0].isNotBlank() && pair[1].isNotBlank()) {
                put(pair[0].trim(), pair[1].trim())
            }
        }
    }

    private fun normalizeCookieHeader(values: Map<String, String>): String = values
        .filterValues(String::isNotBlank)
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        .entries
        .joinToString("; ") { (key, value) -> "$key=$value" }

    private fun JsonObject.string(key: String): String {
        val actual = keys.firstOrNull { it.equals(key, ignoreCase = true) } ?: return ""
        val value = this[actual]
        if (value == null || value is JsonObject) return ""
        return (value as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
    }

    private fun JsonObject.int(key: String): Int? = string(key).toIntOrNull()

    private fun JsonObject.obj(key: String): JsonObject? {
        val actual = keys.firstOrNull { it.equals(key, ignoreCase = true) } ?: return null
        return this[actual] as? JsonObject
    }

    private fun JsonObject.errorMessage(fallback: String, extra: JsonObject? = null): String =
        listOfNotNull(extra, this).firstValue("errMsg", "errTip", "message").ifBlank { fallback }

    private fun List<JsonObject>.firstValue(vararg names: String): String {
        for (value in this) {
            for (name in names) value.string(name).takeIf(String::isNotBlank)?.let { return it }
        }
        return ""
    }

    private fun List<JsonObject>.firstMatchingValue(
        predicate: (String) -> Boolean,
        vararg names: String
    ): String? {
        for (value in this) {
            for (name in names) value.string(name).takeIf(predicate)?.let { return it }
        }
        return null
    }

    private fun Map<String, String>.valueFor(vararg names: String): String? {
        for (name in names) {
            entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.let { return it }
        }
        return null
    }

    private fun Map<String, String>.firstMatchingValue(
        predicate: (String) -> Boolean,
        vararg names: String
    ): String? {
        for (name in names) {
            entries.firstOrNull { it.key.equals(name, ignoreCase = true) && predicate(it.value) }
                ?.value?.let { return it }
        }
        return null
    }

    private fun MutableMap<String, String>.putCanonical(name: String, value: String) {
        removeCaseInsensitive(name)
        put(name, value)
    }

    private fun MutableMap<String, String>.removeCaseInsensitive(name: String) {
        keys.firstOrNull { it.equals(name, ignoreCase = true) }?.let(::remove)
    }

    /** 纯数字且有非零位：`0` / `000000` 这类占位值不能当账号。 */
    private fun isUsableUin(value: String): Boolean =
        value.isNotBlank() && value.all(Char::isDigit) && value.any { it != '0' }

    private data class PhoneHttpResponse(
        val body: JsonObject,
        val setCookieHeaders: List<String>
    )

    private companion object {
        const val MUSICU_URL = "https://u.y.qq.com/cgi-bin/musicu.fcg"
        const val LOGIN_MODULE = "music.login.LoginServer"
        const val AREA_CODE = "86"
        const val MAINLAND_PHONE_LENGTH = 11
        const val CLIENT_VERSION = 20_050_009
        const val SECURITY_CHALLENGE_CODE = 20276
        const val CALL_TIMEOUT_SECONDS = 20L
        const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
    }
}

/**
 * 账号被风控要求先做网页安全验证。
 *
 * 单独一个异常类型是因为界面要拿到 [securityUrl] 才能引导用户去验证 ——
 * 当成普通失败处理的话，用户只会看到「登录失败」，不知道下一步该做什么。
 * 消息里带上地址，是因为这条通道没有内置浏览器，用户得自己复制出去打开。
 */
class QQMusicSecurityChallengeException(
    val securityUrl: String
) : IOException("QQ 音乐要求先完成安全验证，请在浏览器打开 $securityUrl 验证后重试，或改用扫码登录")
