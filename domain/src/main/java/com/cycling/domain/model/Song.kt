package com.cycling.domain.model

data class Song(
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
    /** Effective artwork: the user's own cover when set, otherwise MediaStore's. */
    val albumArt: String? = null,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val bitrate: Int = 0,
    /** Star rating, 0 = unrated, 1..5. */
    val rating: Int = 0,
    /** True when [albumArt] came from the user rather than MediaStore. */
    val hasCustomAlbumArt: Boolean = false
)
