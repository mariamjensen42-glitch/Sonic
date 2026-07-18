package com.cycling.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val dateAdded: Long,
    val dateModified: Long,
    val numberOfSongs: Int
)
