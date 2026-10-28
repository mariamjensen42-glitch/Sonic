package com.cycling.data.api.qqmusic

/**
 * QQ 音乐登录态。
 *
 * [cookie] 是请求 QQ 音乐接口时要原样回传的那串键值对；[uin] 与 [musicKey] 是其中决定
 * 「是不是有效登录」的两项，解出来单独存，免得每次读登录态都要重新解析 Cookie。
 * [nickname] / [avatarUrl] 只是展示用，登录时从账号资料接口取一次。
 */
data class QQMusicSession(
    val cookie: String,
    val uin: String,
    val musicKey: String,
    val nickname: String = "",
    val avatarUrl: String? = null
) {
    val isLoggedIn: Boolean get() = uin.isNotBlank() && musicKey.isNotBlank()
}
