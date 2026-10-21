package com.cycling.data.api.cover.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Kugou's web search endpoint (`song_search_v2`).
 *
 * Unlike the other catalogues this one answers with capitalised keys, and its `Image` field is a
 * template (`http://imge.kugou.com/stdmusic/{size}/...`) rather than a finished URL — the size
 * placeholder has to be filled in before the image can be fetched.
 */
@Serializable
internal data class KugouSearchResponse(
    val status: Int = -1,
    val data: KugouSearchData? = null
)

@Serializable
internal data class KugouSearchData(
    val lists: List<KugouSongDto> = emptyList()
)

@Serializable
internal data class KugouSongDto(
    @SerialName("SongName") val songName: String = "",
    @SerialName("SingerName") val singerName: String = "",
    @SerialName("AlbumName") val albumName: String = "",
    /** Length in seconds. */
    @SerialName("Duration") val durationSeconds: Int = 0,
    @SerialName("Image") val image: String = "",
    /** 取链用的文件 hash：酷狗的 LX 音源要的就是它（`musicInfo.hash`）。 */
    @SerialName("FileHash") val fileHash: String = "",
    @SerialName("AlbumID") val albumId: String = ""
)
