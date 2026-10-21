package com.cycling.data.api.cover.dto

import kotlinx.serialization.Serializable

/**
 * Apple's public iTunes Search API — the one source here that is documented and key-less.
 *
 * `artworkUrl100` embeds the requested pixel size in the path (`.../100x100bb.jpg`), which is how
 * the provider asks for a larger copy.
 */
@Serializable
internal data class ITunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ITunesTrackDto> = emptyList()
)

@Serializable
internal data class ITunesTrackDto(
    val trackName: String = "",
    val artistName: String = "",
    val collectionName: String = "",
    val artworkUrl100: String = "",
    val trackTimeMillis: Long = 0,
    /** ISO-8601 release timestamp, e.g. `1969-09-26T07:00:00Z`. */
    val releaseDate: String = "",
    val primaryGenreName: String = "",
    val trackNumber: Int = 0,
    val trackCount: Int = 0,
    val discNumber: Int = 0,
    val discCount: Int = 0
)
