package com.cycling.domain.model

/**
 * 一个已登录的 QQ 音乐账号。
 *
 * 只带展示所需的字段：登录凭据（Cookie）留在 `:data` 的会话存储里，不进入领域层 ——
 * 界面没有任何理由读得到它，越界一次就会有人在页面里拼请求。
 */
data class QQMusicAccount(
    /** QQ 音乐账号标识（纯数字的 uin）。 */
    val uin: String,
    val nickname: String,
    val avatarUrl: String?
)

/**
 * 登录通道。
 *
 * [QQ] / [WECHAT] 走扫码，[PHONE] 走手机号验证码 —— 三条路的协议完全不同，但落盘后的
 * 会话形状一致，所以界面上只是三种「怎么登」，登录成功之后没有任何区别。
 */
enum class QQMusicLoginMethod {
    QQ,
    WECHAT,
    PHONE
}

/**
 * 一张待扫描的登录二维码。
 *
 * [imageBytes] 是服务端直接下发的图片，不做二次编码 —— QQ 通道给 PNG，微信通道给 JPEG，
 * 用各自的 [imageMimeType] 交给图片解码器即可。
 *
 * 用普通类而不是 `data class`：`ByteArray` 的 equals 是引用比较，自动生成的
 * `equals` 会给人「两张图内容相同」的错觉。
 */
class QQMusicQrTicket(
    /** 仓库内部句柄，用来找回这次登录的 Cookie 罐；对界面无意义。 */
    val id: String,
    val method: QQMusicLoginMethod,
    val imageBytes: ByteArray,
    val imageMimeType: String,
    val expiresAtMillis: Long
)

/**
 * 扫码登录的轮询结果。
 *
 * [Authorized] 表示**凭据已经落盘并校验通过**，界面可以直接切到已登录态；
 * 校验失败会以异常形式抛出，不会走到这里。
 */
sealed interface QQMusicQrStatus {
    data object Waiting : QQMusicQrStatus
    data object Scanned : QQMusicQrStatus
    data object Expired : QQMusicQrStatus
    data object Rejected : QQMusicQrStatus
    data object Authorized : QQMusicQrStatus
}
