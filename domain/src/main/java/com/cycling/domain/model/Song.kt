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
    val albumArt: String? = null,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val bitrate: Int = 0
)
