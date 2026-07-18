package com.cycling.domain.model

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val albumArt: String?,
    val numberOfSongs: Int,
    val firstYear: Int,
    val lastYear: Int
)
