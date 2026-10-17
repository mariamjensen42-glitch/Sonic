package com.cycling.data.api.lyrics.dto

import kotlinx.serialization.Serializable

/**
 * Shapes for the two NetEase Cloud Music endpoints the provider uses.
 *
 * Every field is defaulted and every container nullable because these are unofficial endpoints:
 * an error is often reported as `200 OK` with `code`/`result` absent, and a version bump has
 * previously renamed `duration` to `dt`. Decoding must survive that rather than throw.
 */
@Serializable
internal data class NetEaseSearchResponse(
    val result: NetEaseSearchResult? = null
)

@Serializable
internal data class NetEaseSearchResult(
    val songs: List<NetEaseSongDto> = emptyList()
)

@Serializable
internal data class NetEaseSongDto(
    val id: Long = 0,
    val name: String = "",
    val artists: List<NetEaseArtistDto> = emptyList(),
    val album: NetEaseAlbumDto? = null,
    /** Track length in milliseconds. */
    val duration: Long = 0,
    /** The same value under its newer name; whichever is populated wins. */
    val dt: Long = 0
) {
    val durationMs: Long get() = duration.takeIf { it > 0 } ?: dt

    val artistNames: String get() = artists.joinToString(", ") { it.name }
}

@Serializable
internal data class NetEaseArtistDto(val name: String = "")

@Serializable
internal data class NetEaseAlbumDto(val name: String = "")

@Serializable
internal data class NetEaseLyricResponse(
    val lrc: NetEaseLyricBody? = null,
    /** Word-by-word lyrics. Only some tracks have them, and the field is absent when they do not. */
    val yrc: NetEaseLyricBody? = null,
    /** Translation, timed to the `lrc` lines. */
    val tlyric: NetEaseLyricBody? = null,
    /** Translation, timed to the `yrc` lines. Not the same values: it is a separate submission. */
    val ytlrc: NetEaseLyricBody? = null
)

@Serializable
internal data class NetEaseLyricBody(
    val lyric: String? = null
)
