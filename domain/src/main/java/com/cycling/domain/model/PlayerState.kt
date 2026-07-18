package com.cycling.domain.model

data class PlayerState(
    val currentSong: Song? = null,
    val playbackPosition: Long = 0,
    val duration: Long = 0,
    val isPlaying: Boolean = false,
    val playbackQueue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleMode: Boolean = false
)

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}
