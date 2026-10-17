package com.cycling.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Searchable plain text of a song's lyrics.
 *
 * Separate from `lyrics_cache` because that table is keyed by a `title|artist|duration`
 * fingerprint and only holds what the network returned. This one is keyed by song id and holds
 * whatever the player actually resolved — a sidecar `.lrc`, an embedded tag, the user's own text,
 * or a download — so "我记得这句歌词" works regardless of where the lyrics came from.
 *
 * Cascades with `songs`: a deleted/re-imported track can never inherit another file's lyrics.
 */
@Entity(
    tableName = "lyrics_index",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LyricsIndexEntity(
    @PrimaryKey
    val songId: Long,
    /** Timings stripped, lines separated by spaces; see `LyricsRepositoryImpl.plainText`. */
    val content: String,
    val updatedAt: Long
)
