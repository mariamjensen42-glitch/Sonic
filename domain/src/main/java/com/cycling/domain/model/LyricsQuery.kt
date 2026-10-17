package com.cycling.domain.model

/**
 * What an online provider needs to identify a track.
 *
 * Deliberately not a [Song]: the file path, id and artwork are meaningless to a remote service,
 * and keeping them out means a cached result stays valid when the file moves.
 */
data class LyricsQuery(
    val title: String,
    val artist: String,
    val album: String,
    /** Track length in milliseconds, as MediaStore reports it. */
    val durationMs: Long
) {
    /**
     * Stable identity for the `lyrics_cache` row.
     *
     * Normalised because the same track is described slightly differently across sources — casing
     * and padding vary freely, and duration is quantised to whole seconds so that a one- or
     * two-second difference between a streamed and a local copy still hits the same row.
     */
    val cacheKey: String
        get() = buildString {
            append(title.normalised())
            append('|')
            append(artist.normalised())
            append('|')
            append(durationMs / 1000)
        }

    private fun String.normalised(): String = lowercase().trim()

    companion object {
        fun of(song: Song): LyricsQuery = LyricsQuery(
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.duration
        )
    }
}
