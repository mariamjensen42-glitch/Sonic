package com.cycling.data.api.qqmusic

import com.cycling.data.api.qqmusic.dto.QqLoginCredentialDto
import java.io.IOException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * 扫码状态机的事件。
 *
 * 与领域层的 [com.cycling.domain.model.QQMusicQrStatus] 一一对应，但多一个 [Connected]：
 * 「已授权」在协议层只是拿到了票据，还要再换一次 Cookie 才算登录成功，仓库负责这一步。
 */
internal enum class QqQrEvent {
    Waiting,
    Scanned,
    Connected,
    Expired,
    Rejected
}

/**
 * 一次扫码轮询的原始结果。
 *
 * [uin] / [sigX] 只有 QQ 通道在 [QqQrEvent.Connected] 时才有值，[authorizationCode] 只有
 * 微信通道才有 —— 两条通道的下一步换票方式完全不同。
 */
internal data class QqQrEventResult(
    val event: QqQrEvent,
    val uin: String = "",
    val sigX: String = "",
    val authorizationCode: String = ""
)

/** `ptuiCB('…','…',…)` 里的单引号字符串，允许 `\'` 与 `\\` 转义。 */
private val PTUI_ARG = Regex("""'((?:\\.|[^'])*)'""")
private val PTUI_CALL = Regex("""ptuiCB\((.*)\)""")
private val WECHAT_QR_UUID = Regex("""/connect/qrcode/([A-Za-z0-9_-]+)""")
private val WECHAT_ERRCODE = Regex("""window\.wx_errcode\s*=\s*(\d+)""")
private val WECHAT_CODE = Regex("""window\.wx_code\s*=\s*['"]([^'"]+)['"]""")

/**
 * 解析 `ptlogin2.qq.com/ptqrlogin` 的回调脚本。
 *
 * 服务端把状态塞在一段 JS 里返回，状态码是第一个参数。旧版还会用 405 / 408 / 404 / 402 / 403
 * 这组 HTTP 语义码，和新版的 0 / 66 / 67 / 65 / 68 混着发，所以两组都认。
 */
internal fun parsePtuiCallback(raw: String): QqQrEventResult {
    val body = PTUI_CALL.find(raw.trim())?.groupValues?.get(1)
        ?: throw IOException("无法解析 QQ 扫码状态")
    val args = PTUI_ARG.findAll(body)
        .map { match -> match.groupValues[1].replace("\\'", "'").replace("\\\\", "\\") }
        .toList()
    val code = args.firstOrNull()?.toIntOrNull()
        ?: throw IOException("QQ 扫码状态码无效")

    return when (code) {
        0, 405 -> {
            val callbackUrl = args.getOrNull(2)?.toHttpUrlOrNull()
                ?: throw IOException("QQ 授权地址无效")
            val uin = callbackUrl.queryParameter("uin").orEmpty()
            val sigX = callbackUrl.queryParameter("ptsigx").orEmpty()
            if (uin.isBlank() || sigX.isBlank()) throw IOException("QQ 授权响应缺少账号参数")
            QqQrEventResult(QqQrEvent.Connected, uin = uin, sigX = sigX)
        }

        66, 408 -> QqQrEventResult(QqQrEvent.Waiting)
        67, 404 -> QqQrEventResult(QqQrEvent.Scanned)
        65, 402 -> QqQrEventResult(QqQrEvent.Expired)
        68, 403 -> QqQrEventResult(QqQrEvent.Rejected)
        else -> throw IOException("未知的 QQ 扫码状态：$code")
    }
}

/** 从微信扫码登录页的 HTML 里挖出二维码 uuid。 */
internal fun parseWechatQrUuid(html: String): String {
    val uuid = WECHAT_QR_UUID.find(html)?.groupValues?.getOrNull(1).orEmpty()
    if (uuid.isBlank()) throw IOException("微信登录服务没有返回二维码标识")
    return uuid
}

/**
 * 解析 `long.open.weixin.qq.com/connect/l/qrconnect` 的长轮询脚本。
 *
 * 405 表示已确认授权，此时脚本里带着 `wx_code`，拿它去换 QQ 音乐的票。
 */
internal fun parseWechatPoll(raw: String): QqQrEventResult {
    val status = WECHAT_ERRCODE.find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull()
        ?: throw IOException("无法解析微信扫码状态")
    return when (status) {
        405 -> {
            val code = WECHAT_CODE.find(raw)?.groupValues?.getOrNull(1).orEmpty()
            if (code.isBlank()) throw IOException("微信扫码授权没有返回登录码")
            QqQrEventResult(QqQrEvent.Connected, authorizationCode = code)
        }

        408 -> QqQrEventResult(QqQrEvent.Waiting)
        404 -> QqQrEventResult(QqQrEvent.Scanned)
        403 -> QqQrEventResult(QqQrEvent.Rejected)
        402 -> QqQrEventResult(QqQrEvent.Expired)
        500 -> throw IOException("微信登录服务暂时繁忙，请重新生成二维码")
        else -> throw IOException("未知的微信扫码状态：$status")
    }
}

/**
 * 微信换票请求体。
 *
 * 形状与 QQ 通道不同：模块名是 `music.login.LoginServer`，参数是 `strAppid` + `code`，
 * 且外层 `comm` 要标 `tmeLoginType = 1`（微信）。
 */
internal fun buildWechatLoginPayload(code: String, wechatAppId: String): JsonObject = buildJsonObject {
    put(
        "comm",
        buildJsonObject {
            put("tmeAppID", "qqmusic")
            put("tmeLoginType", "1")
            put("g_tk", 5381)
            put("platform", "yqq")
            put("ct", 24)
            put("cv", 0)
        }
    )
    put(
        "req",
        buildJsonObject {
            put("module", "music.login.LoginServer")
            put("method", "Login")
            put(
                "param",
                buildJsonObject {
                    put("strAppid", wechatAppId)
                    put("code", code)
                }
            )
        }
    )
}

/**
 * QQ 登录协议里的 `hash33`：`h = h * 33 + byte`，逐字节累加后取低 31 位。
 *
 * 两个用途共用一份实现：二维码轮询的 `ptqrtoken`（种子 0）与请求签名 `g_tk`（种子 5381）。
 * `g_tk` 只对 ASCII 的 musicKey 求值，因此按字节与按字符遍历结果一致。
 */
internal fun qqHash33(value: String, seed: Int): Int {
    var hash = seed.toLong()
    value.toByteArray(Charsets.UTF_8).forEach { byte ->
        hash += (hash shl 5) + (byte.toInt() and 0xff)
    }
    return (hash and 0x7fffffff).toInt()
}

/**
 * 把换票结果拼成 QQ 音乐认的 Cookie 串。
 *
 * 只写有值的项：空值写成 `key=` 会让服务端把它当成一次覆盖，反而把别的接口带下来的值清掉。
 * 含 `;`、CR、LF 的值直接丢弃 —— 拼进 Cookie 头就是一次请求头注入。
 */
internal fun buildCredentialCookie(credential: QqLoginCredentialDto): String {
    val uin = credential.musicIdOrNull().filter(Char::isDigit).trimStart('0')
    if (uin.isBlank() || credential.musickey.isBlank()) {
        throw IOException("QQ 音乐登录凭据不完整")
    }
    val declaredType = credential.loginType.takeIf { it != 0 } ?: credential.loginTypeFallback
    // 微信通道的 musicKey 以 W_X 开头；声明值缺失时据此兜底。
    val loginType = declaredType.takeIf { it != 0 }
        ?: if (credential.musickey.startsWith("W_X")) 1 else 2

    return listOf(
        "uin" to uin,
        "qqmusic_uin" to uin,
        "qm_keyst" to credential.musickey,
        "qqmusic_key" to credential.musickey,
        "music_key" to credential.musickey,
        "login_type" to loginType.toString(),
        "psrf_qqopenid" to credential.openid,
        "psrf_qqaccess_token" to credential.accessToken,
        "psrf_qqrefresh_token" to credential.refreshToken,
        "psrf_qqunionid" to credential.unionid,
        "euin" to credential.encryptUin
    ).filter { (_, value) ->
        value.isNotBlank() && value.none { it == ';' || it == '\r' || it == '\n' }
    }.joinToString("; ") { (key, value) -> "$key=$value" }
}

/**
 * 从 Cookie 串里解出账号标识与音乐密钥。
 *
 * `login_type=2` 的账号（QQ 号登录）把账号放在 `uin`，微信登录放在 `wxuin`；两者都可能缺失，
 * 所以按「显式声明 → 两个候选 → 放弃」的顺序取。缺任意一项就返回 `null`：半个登录态
 * 比没有更糟，后续请求只会收到一堆无法解释的错误码。
 */
internal fun parseSessionCookie(cookie: String): QQMusicSession? {
    val values = cookieValues(cookie)
    val rawUin = values["qqmusic_uin"]
        .orEmpty()
        .ifBlank { if (values["login_type"] == "2") values["wxuin"].orEmpty() else values["uin"].orEmpty() }
        .ifBlank { values["wxuin"].orEmpty() }
    val uin = rawUin.filter(Char::isDigit)
    val musicKey = values["qm_keyst"].orEmpty().ifBlank { values["qqmusic_key"].orEmpty() }
    if (uin.isBlank() || musicKey.isBlank()) return null

    return QQMusicSession(
        cookie = cookie.trim(),
        uin = uin,
        musicKey = musicKey
    )
}

/** Cookie 串 → 键值表。值里允许出现 `=`（token 常见），所以只按第一个 `=` 切。 */
private fun cookieValues(cookie: String): Map<String, String> = cookie
    .split(';')
    .map(String::trim)
    .mapNotNull { entry ->
        val separator = entry.indexOf('=')
        if (separator <= 0) null else entry.substring(0, separator).trim() to entry.substring(separator + 1).trim()
    }
    .filter { (key, _) -> key.isNotBlank() }
    .toMap()

/** `musicid` 可能是数字也可能是字符串，统一按文本读。 */
internal fun QqLoginCredentialDto.musicIdOrNull(): String {
    if (musicId.isNotBlank()) return musicId
    val primitive = musicid as? JsonPrimitive ?: return ""
    return primitive.content.takeIf { it != "null" }.orEmpty()
}
