package com.cycling.data.api.lyrics.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A track as LRCLIB describes it.
 *
 * One shape covers both calls: `/api/get` returns a single object and `/api/search` an array of
 * the same objects, so a second DTO would only duplicate this one.
 */
@Serializable
internal data class LrclibTrackDto(
    @SerialName("trackName") val trackName: String = "",
    @SerialName("artistName") val artistName: String = "",
    @SerialName("albumName") val albumName: String = "",
    /** Seconds, not milliseconds — the one field LRCLIB does not express the way the app does. */
    val duration: Double? = null,
    val instrumental: Boolean = false,
    @SerialName("plainLyrics") val plainLyrics: String? = null,
    @SerialName("syncedLyrics") val syncedLyrics: String? = null
) {
    val durationMs: Long? get() = duration?.let { (it * 1000).toLong() }
}
