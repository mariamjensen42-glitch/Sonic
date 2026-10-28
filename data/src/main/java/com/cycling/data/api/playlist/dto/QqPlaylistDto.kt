package com.cycling.data.api.playlist.dto

import kotlinx.serialization.Serializable

/**
 * QQ 音乐 `fcg_ucc_getcdinfo_byids_cp.fcg` 的响应。
 *
 * 字段名是这套老接口自己的拼法（`dissname` / `songlist` / `songmid`），不要按语义改名 ——
 * 反序列化按名字匹配。
 */
@Serializable
internal data class QqPlaylistResponse(
    val code: Int = 0,
    val subcode: Int = 0,
    val cdlist: List<QqPlaylistDto> = emptyList()
)

@Serializable
internal data class QqPlaylistDto(
    val disstid: String = "",
    /** 歌单名。 */
    val dissname: String = "",
    /** 封面（`qpic.y.qq.com/music_cover/...`，不带扩展名）。 */
    val logo: String = "",
    /** 歌单简介，多数歌单是空串。 */
    val desc: String = "",
    val songnum: Int = 0,
    val songlist: List<QqPlaylistSongDto> = emptyList()
)

@Serializable
internal data class QqPlaylistSongDto(
    /** 歌曲 mid —— 洛雪 `tx` 源脚本认的 `musicId`，也是搜索接口里用的同一字段。 */
    val songmid: String = "",
    val songname: String = "",
    val albumname: String = "",
    /** 专辑 mid，用来拼封面：`T002R300x300M000{albummid}.jpg`。 */
    val albummid: String = "",
    /** 时长（秒）。 */
    val interval: Int = 0,
    val singer: List<QqPlaylistSingerDto> = emptyList()
)

@Serializable
internal data class QqPlaylistSingerDto(val name: String = "")
