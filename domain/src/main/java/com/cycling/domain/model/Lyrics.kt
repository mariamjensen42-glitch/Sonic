package com.cycling.domain.model

import com.cycling.domain.lyrics.model.SyncedLyrics

data class Lyrics(
    val syncedLyrics: SyncedLyrics?,
    val rawContent: String?,
    val source: LyricsSource
)

enum class LyricsSource {
    /**
     * Written by the user for this file, and therefore authoritative.
     *
     * Ranks above every other source — a sidecar file and an embedded tag included — because an
     * edit is an explicit statement about *this* recording, while everything below it is a guess
     * made on the user's behalf.
     */
    USER,
    EMBEDDED,
    FILE,
    /** Downloaded from an [LyricsProvider] and persisted in `lyrics_cache`. */
    ONLINE,
    NONE
}
