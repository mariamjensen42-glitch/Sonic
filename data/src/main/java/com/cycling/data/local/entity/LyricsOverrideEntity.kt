package com.cycling.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lyrics the user wrote for one song, overriding every automatic source.
 *
 * Keyed by song id rather than by the `title|artist|duration` fingerprint `lyrics_cache` uses:
 * the user edits lyrics *and* tags on the same page, and correcting the title must not silently
 * detach the lyrics from the track. The row cascades with `songs`, so it cannot outlive the file
 * it describes and be picked up by a different track that later reuses the id.
 *
 * [rawContent] is kept verbatim — including LRC timings — because it is the text the editor shows
 * back and the text the player re-parses; normalising it here would lose the timings.
 */
@Entity(tableName = "lyrics_override")
data class LyricsOverrideEntity(
    @PrimaryKey
    val songId: Long,
    val rawContent: String,
    val updatedAt: Long
)
