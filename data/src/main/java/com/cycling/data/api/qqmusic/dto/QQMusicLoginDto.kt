package com.cycling.data.api.qqmusic.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * `musicu.fcg` 的扫码登录应答。
 *
 * 两条通道的应答形状不同：QQ 扫码把业务结果放在 `req_0`，微信扫码放在 `req`，内部结构一致，
 * 所以两个字段都接、取值时取先到的那个。所有字段都有默认值 —— 这是非公开接口，
 * 少一个字段不该让整次登录崩掉。
 */
@Serializable
internal data class QqLoginEnvelope(
    val code: Int = -1,
    @SerialName("req_0") val qqModule: QqLoginModule? = null,
    val req: QqLoginModule? = null
) {
    val module: QqLoginModule? get() = qqModule ?: req
}

@Serializable
internal data class QqLoginModule(
    val code: Int = -1,
    val data: QqLoginCredentialDto? = null
)

/**
 * 换票接口给出的播放凭据。
 *
 * [musicid] 在部分边缘节点上是数字、部分上是字符串，用 [JsonElement] 收下再按文本读；
 * 真正稳定的字段是 `str_musicid`。
 */
@Serializable
internal data class QqLoginCredentialDto(
    @SerialName("str_musicid") val musicId: String = "",
    val musicid: JsonElement? = null,
    val musickey: String = "",
    val openid: String = "",
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    val unionid: String = "",
    val encryptUin: String = "",
    val loginType: Int = 0,
    @SerialName("login_type") val loginTypeFallback: Int = 0
)

/**
 * 账号资料页（`fcg_get_profile_homepage.fcg`）。
 *
 * 昵称与头像散落在 `data.creator` / `data.user` / `data.profile` 三处之一，且字段名有
 * `nick` / `nickname` / `name` 三种写法；[QqProfileDto] 把三层都声明出来，取值时依次回落。
 */
@Serializable
internal data class QqProfileEnvelope(
    val code: Int = -1,
    val message: String = "",
    val msg: String = "",
    val data: QqProfileDto? = null
)

@Serializable
internal data class QqProfileDto(
    val creator: QqProfilePersonDto? = null,
    val user: QqProfilePersonDto? = null,
    val profile: QqProfilePersonDto? = null,
    val nick: String = "",
    val nickname: String = "",
    val name: String = "",
    val headpic: String = "",
    val avatar: String = "",
    val avatarUrl: String = ""
) {
    /** 三个可能的位置，按 QQ 的优先顺序。 */
    val people: List<QqProfilePersonDto>
        get() = listOfNotNull(creator, user, profile)
}

@Serializable
internal data class QqProfilePersonDto(
    val nick: String = "",
    val nickname: String = "",
    val name: String = "",
    val headpic: String = "",
    val avatar: String = "",
    val avatarUrl: String = ""
)
