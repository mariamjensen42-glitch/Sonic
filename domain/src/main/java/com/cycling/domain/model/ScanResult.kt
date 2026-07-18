package com.cycling.domain.model

data class ScanResult(
    val songsFound: Int,
    val albumsFound: Int,
    val artistsFound: Int,
    val duration: Long,
    val timestamp: Long
)
