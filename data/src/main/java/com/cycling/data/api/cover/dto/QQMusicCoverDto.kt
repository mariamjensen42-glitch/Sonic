package com.cycling.data.api.cover.dto

import kotlinx.serialization.Serializable

/**
 * QQ Music's web search endpoint (`client_search_cp`).
 *
 * The album picture is addressed by `albummid`, so no second request is needed — the CDN URL can
 * be built from the mid directly. Fields are all defaulted because this is an unofficial endpoint.
 */
@Serializable
internal data class QqSearchResponse(
    val code: Int = -1,
    val data: QqSearchData? = null
)

@Serializable
internal data class QqSearchData(val song: QqSongListDto? = null)

@Serializable
internal data class QqSongListDto(val list: List<QqSongDto> = emptyList())

@Serializable
internal data class QqSongDto(
    val songname: String = "",
    val albumname: String = "",
    val albummid: String = "",
    /** QQ's per-track id; the lyric endpoint addresses a song by it. */
    val songmid: String = "",
    /** Length in seconds. */
    val interval: Int = 0,
    /** Release timestamp in seconds; `0` when the endpoint omitted it. */
    val pubtime: Long = 0,
    val singer: List<QqSingerDto> = emptyList()
)

@Serializable
internal data class QqSingerDto(val name: String = "")
