package com.cycling.domain.model

import com.cycling.domain.lyrics.model.SyncedLyrics

data class Lyrics(
    val syncedLyrics: SyncedLyrics?,
    val rawContent: String?,
    val source: LyricsSource
)

enum class LyricsSource {
    EMBEDDED,
    FILE,
    NONE
}
