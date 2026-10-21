package com.cycling.data.api.lyrics.dto

import kotlinx.serialization.Serializable

/**
 * Shapes for the two Kugou endpoints the lyrics provider uses.
 *
 * The search endpoint answers with a `candidates` array rather than full songs: each entry already
 * carries the lyric id and access key, so no second search is needed before downloading. Every
 * field is defaulted because these are unofficial web endpoints that report errors as HTTP 200.
 */
@Serializable
internal data class KugouLyricSearchResponse(
    val status: Int = 0,
    val candidates: List<KugouLyricCandidateDto> = emptyList()
)

/** One uploaded lyric document for the searched keywords. */
@Serializable
internal data class KugouLyricCandidateDto(
    val id: String = "",
    /** Paired with [id] for the download call; without it the lyric is not served. */
    val accesskey: String = "",
    val song: String = "",
    val singer: String = "",
    /** Length in milliseconds, as the catalogue reports it. */
    val duration: Long = 0
) {
    /** `null` when the entry was not dated, which ranks it last instead of rejecting it. */
    val durationMs: Long? get() = duration.takeIf { it > 0 }
}

@Serializable
internal data class KugouLyricDownloadResponse(
    val status: Int = 0,
    /** Base64-encoded LRC; `null` when the catalogue has no lyric for the id. */
    val content: String? = null
)
