package com.cycling.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Per-song state that changes often or is only needed for one screen.
 *
 * Deliberately a side table rather than columns on [SongEntity]:
 * - [resumePositionMs] is written roughly every 5 seconds during playback, and
 * - the whole app only ever reads these for the song currently on screen,
 *
 * so keeping them out of `songs` avoids bloating every list query. Rows cascade away with their
 * song; nothing has to clean them up.
 */
@Entity(
    tableName = "song_state",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SongStateEntity(
    @PrimaryKey
    val songId: Long,
    /** Where to resume playback, in ms. 0 means "start from the beginning". */
    @ColumnInfo(defaultValue = "0")
    val resumePositionMs: Long = 0,
    /** Per-song lyrics timing correction, in ms. Positive shifts the lyrics later. */
    @ColumnInfo(defaultValue = "0")
    val lyricsOffsetMs: Long = 0,
    val updatedAt: Long = 0
)
