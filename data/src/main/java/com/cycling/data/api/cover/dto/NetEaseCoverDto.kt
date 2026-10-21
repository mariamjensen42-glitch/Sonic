package com.cycling.data.api.cover.dto

import kotlinx.serialization.Serializable

/**
 * `/api/song/detail` — the endpoint that actually carries the cover URL.
 *
 * The search endpoint returns only a `picId`, and the CDN path is derived from an encrypted hash,
 * so it cannot be reconstructed locally; one extra request per search buys a working image URL.
 */
@Serializable
internal data class NetEaseDetailResponse(
    val songs: List<NetEaseDetailSongDto> = emptyList()
)

@Serializable
internal data class NetEaseDetailSongDto(
    val id: Long = 0,
    val name: String = "",
    /** Track length in milliseconds. */
    val duration: Long = 0,
    val artists: List<NetEaseDetailArtistDto> = emptyList(),
    val album: NetEaseDetailAlbumDto? = null,
    /** Position on its disc; 0 when the endpoint did not say. */
    val no: Int = 0,
    /** Disc number as text, sometimes zero-padded (`"01"`). */
    val disc: String = ""
)

@Serializable
internal data class NetEaseDetailArtistDto(val name: String = "")

@Serializable
internal data class NetEaseDetailAlbumDto(
    val name: String = "",
    val picUrl: String? = null,
    /** Release timestamp in milliseconds. */
    val publishTime: Long = 0,
    /** The album's own artist, which is not always the performing artist. */
    val artist: NetEaseDetailArtistDto? = null
)
