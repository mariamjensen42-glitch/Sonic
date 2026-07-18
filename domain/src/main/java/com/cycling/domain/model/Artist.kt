package com.cycling.domain.model

data class Artist(
    val id: Long,
    val name: String,
    val numberOfAlbums: Int,
    val numberOfTracks: Int,
    val artistArt: String? = null
)
