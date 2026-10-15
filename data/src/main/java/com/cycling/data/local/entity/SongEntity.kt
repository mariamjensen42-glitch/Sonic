package com.cycling.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val albumId: Long,
    val artistId: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
    val mimeType: String,
    /** Album art URI published by MediaStore. Never user-editable. */
    val albumArt: String? = null,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val bitrate: Int = 0,
    /** Star rating, 0 = unrated, 1..5. */
    @ColumnInfo(defaultValue = "0")
    val rating: Int = 0,
    /**
     * User-supplied cover, stored as a URI into app-private storage.
     *
     * Kept separate from [albumArt] rather than overwriting it: MediaStore owns [albumArt] and
     * will reset it on the next scan, whereas this column is preserved across scans by
     * [com.cycling.data.local.scanner.MusicScanner]. The two are merged by
     * [com.cycling.data.mapper.toDomain], which prefers this one.
     */
    val customAlbumArt: String? = null
)
