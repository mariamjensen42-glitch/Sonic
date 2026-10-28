package com.cycling.domain.model

/**
 * 登录账号名下的一张 QQ 音乐歌单。
 *
 * 只带「选哪张」需要的字段：id 用来拉曲目，其余是列表展示。曲目本身不在这里 ——
 * 一张歌单可能有几百首，列表页没必要为了显示标题把它们全拉下来。
 */
data class QQMusicPlaylistSummary(
    /** 歌单 id（QQ 的 `disstid` / `tid`），导入时按它拉曲目。 */
    val id: String,
    val title: String,
    val coverUrl: String? = null,
    val trackCount: Int = 0,
    val creatorName: String? = null
)
