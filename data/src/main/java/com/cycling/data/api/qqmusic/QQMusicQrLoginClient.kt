package com.cycling.data.api.qqmusic

import com.cycling.data.api.qqmusic.dto.QqLoginCredentialDto
import com.cycling.data.api.qqmusic.dto.QqLoginEnvelope
import com.cycling.domain.model.QQMusicLoginMethod
import com.cycling.domain.model.QQMusicQrStatus
import java.io.IOException
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * QQ 音乐的原生扫码登录（QQ / 微信双通道），全程不经过 WebView。
 *
 * 两条通道共用「先拿二维码、再轮询状态」的骨架，但授权协议不同：
 * - **QQ**：`ptlogin2.qq.com` 发二维码 → 轮询 `ptqrlogin` 拿到 `uin` + `ptsigx` →
 *   `check_sig` 换出 `p_skey` → `graph.qq.com/oauth2.0/authorize` 拿到 `code` →
 *   `musicu.fcg` 换成音乐密钥。
 * - **微信**：`open.weixin.qq.com` 的开放平台二维码 → 长轮询 `l/qrconnect` 拿到 `code` →
 *   `musicu.fcg` 换成音乐密钥。
 *
 * 整条链靠 Cookie 串联，所以每个登录会话都有自己的 [QQMusicLoginCookieJar]，且**不跟随重定向**：
 * `authorize` 那一步的 `code` 就在 `Location` 头里，跟过去反而会被吞掉。
 */
@Singleton
class QQMusicQrLoginClient @Inject constructor(
    sharedClient: OkHttpClient
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val baseClient: OkHttpClient = sharedClient.newBuilder()
        .callTimeout(LOGIN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(LOGIN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // 登录链路是「同一 Cookie 罐里的一串请求」，命中磁盘缓存会把轮询结果冻在第一次的
        // "等待扫码"上；这点流量也不值得缓存。
        .cache(null)
        .withQqWebUserAgent()
        .build()

    /**
     * 申请一张二维码。
     *
     * @throws IOException 二维码服务不可达，或没有下发图片/标识。
     */
    internal suspend fun createSession(method: QQMusicLoginMethod): QQMusicQrSession =
        withContext(Dispatchers.IO) {
            val cookieJar = QQMusicLoginCookieJar()
            val client = baseClient.newBuilder()
                .cookieJar(cookieJar)
                .followRedirects(false)
                .followSslRedirects(false)
                .build()
            when (method) {
                QQMusicLoginMethod.QQ -> createQqSession(client, cookieJar)
                QQMusicLoginMethod.WECHAT -> createWechatSession(client, cookieJar)
                // 手机号登录没有二维码，走 QQMusicPhoneAuthClient；这里只是把「不该走到这条分支」
                // 变成一句能看懂的错误，而不是一个 when 穷举编译错误。
                QQMusicLoginMethod.PHONE ->
                    throw IOException("手机号登录不需要二维码，请使用验证码登录")
            }
        }

    /**
     * 查询一次扫码进度。
     *
     * 返回 `Authorized` 时 [QQMusicQrCheck.cookie] 一定有值。
     */
    internal suspend fun checkSession(session: QQMusicQrSession): QQMusicQrCheck = withContext(Dispatchers.IO) {
        if (System.currentTimeMillis() >= session.expiresAtMillis) {
            return@withContext QQMusicQrCheck(QQMusicQrStatus.Expired)
        }
        val result = when (session.method) {
            QQMusicLoginMethod.QQ -> pollQq(session)
            QQMusicLoginMethod.WECHAT -> pollWechat(session)
            // 扫码会话只可能是这两条通道；走到这里说明会话被构造错了。
            QQMusicLoginMethod.PHONE -> throw IOException("手机号登录没有扫码会话")
        }
        when (result.event) {
            QqQrEvent.Waiting -> QQMusicQrCheck(QQMusicQrStatus.Waiting)
            QqQrEvent.Scanned -> QQMusicQrCheck(QQMusicQrStatus.Scanned)
            QqQrEvent.Expired -> QQMusicQrCheck(QQMusicQrStatus.Expired)
            QqQrEvent.Rejected -> QQMusicQrCheck(QQMusicQrStatus.Rejected)
            QqQrEvent.Connected -> {
                val credential = when (session.method) {
                    QQMusicLoginMethod.QQ -> authorizeQq(session, result.uin, result.sigX)
                    QQMusicLoginMethod.WECHAT -> exchangeWechatCode(session.httpClient, result.authorizationCode)
                    QQMusicLoginMethod.PHONE -> throw IOException("手机号登录没有扫码会话")
                }
                QQMusicQrCheck(
                    status = QQMusicQrStatus.Authorized,
                    cookie = buildCredentialCookie(credential)
                )
            }
        }
    }

    private fun createQqSession(
        client: OkHttpClient,
        cookieJar: QQMusicLoginCookieJar
    ): QQMusicQrSession {
        val url = QQ_QR_SHOW.toHttpUrl().newBuilder()
            .addQueryParameter("appid", QQ_QR_APP_ID)
            .addQueryParameter("e", "2")
            .addQueryParameter("l", "M")
            .addQueryParameter("s", "3")
            .addQueryParameter("d", "72")
            .addQueryParameter("v", "4")
            .addQueryParameter("t", (System.currentTimeMillis() * 1_000_000L).toString())
            .addQueryParameter("daid", QQ_LOGIN_DAID)
            .addQueryParameter("pt_3rd_aid", QQ_MUSIC_OAUTH_APP_ID)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Referer", PTLOGIN_REFERER)
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("获取 QQ 登录二维码返回 ${response.code}")
            // 二维码标识在 Set-Cookie 里，不在响应体里。
            val qrSignature = cookieJar.loadForRequest(url)
                .firstOrNull { it.name == "qrsig" }
                ?.value
                ?: throw IOException("QQ 登录服务没有返回二维码标识")
            val image = response.body?.bytes()
            if (image == null || image.isEmpty()) throw IOException("读取 QQ 登录二维码失败")
            QQMusicQrSession(
                method = QQMusicLoginMethod.QQ,
                imageBytes = image,
                imageMimeType = "image/png",
                qrToken = qrSignature,
                expiresAtMillis = System.currentTimeMillis() + QQ_QR_LIFETIME_MS,
                httpClient = client,
                cookieJar = cookieJar
            )
        }
    }

    private fun createWechatSession(
        client: OkHttpClient,
        cookieJar: QQMusicLoginCookieJar
    ): QQMusicQrSession {
        val loginUrl = WECHAT_QR_CONNECT.toHttpUrl().newBuilder()
            .addQueryParameter("appid", QQ_MUSIC_WECHAT_APP_ID)
            .addQueryParameter("redirect_uri", QQ_MUSIC_WECHAT_REDIRECT_URL)
            .addQueryParameter("response_type", "code")
            .addQueryParameter("scope", "snsapi_login")
            .addQueryParameter("state", "STATE")
            .build()
        val uuid = client.newCall(Request.Builder().url(loginUrl).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("获取微信登录二维码返回 ${response.code}")
            parseWechatQrUuid(response.body?.string().orEmpty())
        }
        val imageRequest = Request.Builder()
            .url("$WECHAT_QR_IMAGE_BASE/$uuid".toHttpUrl())
            .header("Referer", loginUrl.toString())
            .build()
        return client.newCall(imageRequest).execute().use { response ->
            if (!response.isSuccessful) throw IOException("读取微信登录二维码返回 ${response.code}")
            val image = response.body?.bytes()
            if (image == null || image.isEmpty()) throw IOException("读取微信登录二维码失败")
            QQMusicQrSession(
                method = QQMusicLoginMethod.WECHAT,
                imageBytes = image,
                imageMimeType = "image/jpeg",
                qrToken = uuid,
                expiresAtMillis = System.currentTimeMillis() + WECHAT_QR_LIFETIME_MS,
                httpClient = client,
                cookieJar = cookieJar
            )
        }
    }

    private fun pollQq(session: QQMusicQrSession): QqQrEventResult {
        val url = QQ_QR_POLL.toHttpUrl().newBuilder()
            .addQueryParameter("u1", GRAPH_LOGIN_JUMP)
            .addQueryParameter("ptqrtoken", qqHash33(session.qrToken, 0).toString())
            .addQueryParameter("ptredirect", "0")
            .addQueryParameter("h", "1")
            .addQueryParameter("t", "1")
            .addQueryParameter("g", "1")
            .addQueryParameter("from_ui", "1")
            .addQueryParameter("ptlang", "2052")
            .addQueryParameter("action", "0-0-${System.currentTimeMillis()}")
            .addQueryParameter("js_ver", "20102616")
            .addQueryParameter("js_type", "1")
            .addQueryParameter("pt_uistyle", "40")
            .addQueryParameter("aid", QQ_QR_APP_ID)
            .addQueryParameter("daid", QQ_LOGIN_DAID)
            .addQueryParameter("pt_3rd_aid", QQ_MUSIC_OAUTH_APP_ID)
            .addQueryParameter("has_onekey", "1")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Referer", PTLOGIN_REFERER)
            .build()
        return session.httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("查询 QQ 扫码状态返回 ${response.code}")
            parsePtuiCallback(response.body?.string().orEmpty())
        }
    }

    private fun pollWechat(session: QQMusicQrSession): QqQrEventResult {
        val urlBuilder = WECHAT_QR_POLL.toHttpUrl().newBuilder()
            .addQueryParameter("uuid", session.qrToken)
            .addQueryParameter("_", System.currentTimeMillis().toString())
        session.lastWechatStatus?.let { urlBuilder.addQueryParameter("last", it.toString()) }
        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("Referer", "https://open.weixin.qq.com/")
            .build()
        return try {
            session.httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("查询微信扫码状态返回 ${response.code}")
                parseWechatPoll(response.body?.string().orEmpty()).also { result ->
                    // 已扫码之后要带 last=404 继续长轮询，否则服务端会一直回「未扫码」。
                    session.lastWechatStatus = if (result.event == QqQrEvent.Scanned) 404 else null
                }
            }
        } catch (_: SocketTimeoutException) {
            // 长轮询超时就是「还没扫」，不该当成错误弹给用户。
            QqQrEventResult(QqQrEvent.Waiting)
        }
    }

    /** QQ 通道的第二步：`check_sig` 换 `p_skey`，再走 OAuth 授权拿 `code`，最后换票。 */
    private fun authorizeQq(
        session: QQMusicQrSession,
        uin: String,
        sigX: String
    ): QqLoginCredentialDto {
        val checkUrl = QQ_CHECK_SIG.toHttpUrl().newBuilder()
            .addQueryParameter("uin", uin)
            .addQueryParameter("pttype", "1")
            .addQueryParameter("service", "ptqrlogin")
            .addQueryParameter("nodirect", "0")
            .addQueryParameter("ptsigx", sigX)
            .addQueryParameter("s_url", GRAPH_LOGIN_JUMP)
            .addQueryParameter("ptlang", "2052")
            .addQueryParameter("ptredirect", "100")
            .addQueryParameter("aid", QQ_QR_APP_ID)
            .addQueryParameter("daid", QQ_LOGIN_DAID)
            .addQueryParameter("j_later", "0")
            .addQueryParameter("low_login_hour", "0")
            .addQueryParameter("regmaster", "0")
            .addQueryParameter("pt_login_type", "3")
            .addQueryParameter("pt_aid", "0")
            .addQueryParameter("pt_aaid", "16")
            .addQueryParameter("pt_light", "0")
            .addQueryParameter("pt_3rd_aid", QQ_MUSIC_OAUTH_APP_ID)
            .build()
        session.httpClient.newCall(
            Request.Builder().url(checkUrl).header("Referer", PTLOGIN_REFERER).build()
        ).execute().use { response ->
            if (response.code >= 400) throw IOException("QQ 扫码凭据校验返回 ${response.code}")
        }

        val pSKey = session.cookieJar.loadForRequest(GRAPH_HOME.toHttpUrl())
            .firstOrNull { it.name == "p_skey" }
            ?.value
            .orEmpty()
        if (pSKey.isBlank()) throw IOException("QQ 扫码校验没有返回授权票据")

        val form = FormBody.Builder()
            .add("response_type", "code")
            .add("client_id", QQ_MUSIC_OAUTH_APP_ID)
            .add("redirect_uri", QQ_MUSIC_OAUTH_REDIRECT)
            .add("scope", "get_user_info,get_app_friends")
            .add("state", "state")
            .add("switch", "")
            .add("from_ptlogin", "1")
            .add("src", "1")
            .add("update_auth", "1")
            .add("openapi", "1010_1030")
            .add("g_tk", qqHash33(pSKey, 5381).toString())
            .add("auth_time", (System.currentTimeMillis() / 1_000L * 1_000L).toString())
            .add("ui", randomHex(16))
            .build()
        val code = session.httpClient.newCall(
            Request.Builder()
                .url(QQ_OAUTH_AUTHORIZE)
                .post(form)
                .header("Referer", GRAPH_LOGIN_JUMP)
                .build()
        ).execute().use { response ->
            val location = response.header("Location")
                ?: throw IOException("QQ 音乐授权没有返回登录码")
            location.toHttpUrlOrNull()?.queryParameter("code")?.takeIf(String::isNotBlank)
                ?: throw IOException("QQ 音乐授权没有返回登录码")
        }
        return exchangeQqCode(session.httpClient, code)
    }

    private fun exchangeQqCode(client: OkHttpClient, code: String): QqLoginCredentialDto {
        val payload = buildJsonObject {
            put(
                "comm",
                buildJsonObject {
                    put("ct", 24)
                    put("cv", 4_747_474)
                    put("platform", "yqq.json")
                    put("chid", "0")
                    put("uin", 0)
                    put("g_tk", 5381)
                    put("g_tk_new_20200303", 5381)
                    put("format", "json")
                    put("inCharset", "utf-8")
                    put("outCharset", "utf-8")
                    put("notice", 0)
                    put("needNewCode", 1)
                    put("tmeLoginType", 2)
                }
            )
            put(
                "req_0",
                buildJsonObject {
                    put("module", "QQConnectLogin.LoginServer")
                    put("method", "QQLogin")
                    put("param", buildJsonObject { put("code", code) })
                }
            )
        }
        return postMusicu(client, payload.toString(), MUSIC_WEB_REFERER, JSON_MEDIA_TYPE)
    }

    private fun exchangeWechatCode(client: OkHttpClient, code: String): QqLoginCredentialDto {
        if (code.isBlank()) throw IOException("微信扫码授权没有返回登录码")
        return postMusicu(
            client = client,
            body = buildWechatLoginPayload(code, QQ_MUSIC_WECHAT_APP_ID).toString(),
            referer = QQ_MUSIC_WECHAT_REDIRECT_URL,
            // 微信这条换票请求，官方客户端就是按表单类型发的 JSON 体；照抄，别"顺手修正"。
            mediaType = FORM_MEDIA_TYPE
        )
    }

    private fun postMusicu(
        client: OkHttpClient,
        body: String,
        referer: String,
        mediaType: String
    ): QqLoginCredentialDto {
        val request = Request.Builder()
            .url(MUSICU_URL)
            .post(body.toRequestBody(mediaType.toMediaType()))
            .header("Accept", "*/*")
            .header("Referer", referer)
            .build()
        return client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val envelope = runCatching { json.decodeFromString<QqLoginEnvelope>(text) }
                .getOrElse { throw IOException("QQ 音乐登录响应解析失败", it) }
            val module = envelope.module ?: throw IOException("QQ 音乐登录响应缺少结果")
            if (!response.isSuccessful || envelope.code != 0 || module.code != 0) {
                throw IOException("QQ 音乐拒绝了扫码登录（错误码 ${module.code}）")
            }
            module.data ?: throw IOException("QQ 音乐登录成功但没有返回可用播放凭据")
        }
    }

    private companion object {
        const val QQ_QR_SHOW = "https://ssl.ptlogin2.qq.com/ptqrshow"
        const val QQ_QR_POLL = "https://ssl.ptlogin2.qq.com/ptqrlogin"
        const val QQ_CHECK_SIG = "https://ssl.ptlogin2.graph.qq.com/check_sig"
        const val QQ_OAUTH_AUTHORIZE = "https://graph.qq.com/oauth2.0/authorize"
        const val GRAPH_HOME = "https://graph.qq.com/"
        const val GRAPH_LOGIN_JUMP = "https://graph.qq.com/oauth2.0/login_jump"
        const val PTLOGIN_REFERER = "https://xui.ptlogin2.qq.com/"
        const val WECHAT_QR_CONNECT = "https://open.weixin.qq.com/connect/qrconnect"
        const val WECHAT_QR_IMAGE_BASE = "https://open.weixin.qq.com/connect/qrcode"
        const val WECHAT_QR_POLL = "https://long.open.weixin.qq.com/connect/l/qrconnect"
        const val MUSICU_URL = "https://u.y.qq.com/cgi-bin/musicu.fcg"
        const val MUSIC_WEB_REFERER = "https://y.qq.com/"

        /** QQ 换票走 JSON，微信换票走表单类型（官方客户端如此），两者不能混用。 */
        const val JSON_MEDIA_TYPE = "application/json"
        const val FORM_MEDIA_TYPE = "application/x-www-form-urlencoded"

        /** 第三方接入 QQ 互联的应用 id，扫码登录页固定用它。 */
        const val QQ_QR_APP_ID = "716027609"

        /** QQ 音乐在 QQ 互联上的应用 id。 */
        const val QQ_MUSIC_OAUTH_APP_ID = "100497308"
        const val QQ_LOGIN_DAID = "383"
        const val QQ_MUSIC_WECHAT_APP_ID = "wx48db31d50e334801"
        const val QQ_MUSIC_WECHAT_REDIRECT_URL =
            "https://y.qq.com/portal/wx_redirect.html?login_type=2&surl=https://y.qq.com/"
        const val QQ_MUSIC_OAUTH_REDIRECT =
            "https://y.qq.com/portal/wx_redirect.html?login_type=1&surl=https://y.qq.com/"

        /** 二维码有效期：QQ 2 分钟、微信 5 分钟，与各自的协议一致。 */
        const val QQ_QR_LIFETIME_MS = 2 * 60 * 1_000L
        const val WECHAT_QR_LIFETIME_MS = 5 * 60 * 1_000L
        const val LOGIN_TIMEOUT_SECONDS = 35L
    }
}

/**
 * 一次扫码登录的运行时状态。
 *
 * 每个会话一份 Cookie 罐：QQ 通道的 `qrsig` → `p_skey` → OAuth `code` 全靠它传递，
 * 用共享罐会让两次登录互相污染。
 */
internal class QQMusicQrSession(
    val method: QQMusicLoginMethod,
    val imageBytes: ByteArray,
    val imageMimeType: String,
    internal val qrToken: String,
    internal val expiresAtMillis: Long,
    internal val httpClient: OkHttpClient,
    internal val cookieJar: QQMusicLoginCookieJar
) {
    /** 微信长轮询的 `last` 参数：已扫码后必须带 404 才能继续等到确认。 */
    internal var lastWechatStatus: Int? = null
}

/** 一次轮询的结果；只有 `Authorized` 时 [cookie] 有值。 */
internal data class QQMusicQrCheck(
    val status: QQMusicQrStatus,
    val cookie: String? = null
)

/** 进程内 Cookie 罐：按 name+domain+path 覆盖，过期即清。 */
internal class QQMusicLoginCookieJar : CookieJar {

    private val cookies = linkedMapOf<String, Cookie>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { cookie -> this.cookies[cookieKey(cookie)] = cookie }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        cookies.entries.removeAll { (_, cookie) -> cookie.expiresAt < now }
        return cookies.values.filter { it.matches(url) }
    }

    private fun cookieKey(cookie: Cookie): String = "${cookie.name}|${cookie.domain}|${cookie.path}"
}

private fun randomHex(bytes: Int): String {
    val buffer = ByteArray(bytes)
    SecureRandom().nextBytes(buffer)
    return buffer.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
