package com.cycling.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Tag fields the user edited in the app, kept apart from the file itself.
 *
 * File tags can be written for MP3 / FLAC / OGG / M4A / MP4 (see `AudioMetadataHelper`), but even
 * then a write can fail when the app cannot obtain write access to a media file, and other
 * containers have no writer at all; storing the edits here as well means the correction is not lost
 * in either case. They also outlive a rescan, unlike the `songs` row's scan-owned columns.
 *
 * A `null` column means "not overridden" — the value read from the file (when there is one) still
 * wins. The table cascades with its song so an override cannot outlive the track and be served for
 * a different file that later reuses the id.
 */
@Entity(
    tableName = "song_metadata",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SongMetadataEntity(
    @PrimaryKey
    val songId: Long,
    val albumArtist: String? = null,
    val year: String? = null,
    val genre: String? = null,
    val trackNumber: String? = null,
    val discNumber: String? = null,
    val composer: String? = null,
    val publisher: String? = null,
    val copyright: String? = null,
    val comment: String? = null,
    val updatedAt: Long
)
