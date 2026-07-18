package com.cycling.presentation.player

import com.cycling.domain.lyrics.model.SyncedLyrics
import com.cycling.domain.model.PlayerState
import com.cycling.domain.model.RepeatMode
import com.cycling.domain.model.Song

enum class PlayerViewMode {
    COVER,
    LYRICS
}

data class PlayerUiState(
    val currentSong: Song? = null,
    val playbackPosition: Long = 0,
    val duration: Long = 0,
    val isPlaying: Boolean = false,
    val playbackQueue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleMode: Boolean = false,
    val showQueue: Boolean = false,
    val isFavorite: Boolean = false,
    val viewMode: PlayerViewMode = PlayerViewMode.COVER,
    val lyrics: SyncedLyrics? = null,
    val isLoadingLyrics: Boolean = false,
    val hasLyrics: Boolean = false
)

sealed interface PlayerIntent {
    data class PlaySong(val song: Song, val queue: List<Song> = emptyList()) : PlayerIntent
    data class PlayFromQueue(val index: Int) : PlayerIntent
    data object PlayPause : PlayerIntent
    data class SeekTo(val position: Long) : PlayerIntent
    data object SkipToNext : PlayerIntent
    data object SkipToPrevious : PlayerIntent
    data object ToggleRepeatMode : PlayerIntent
    data object ToggleShuffleMode : PlayerIntent
    data class AddToQueue(val song: Song) : PlayerIntent
    data class PlayNext(val song: Song) : PlayerIntent
    data class MoveQueueItem(val fromIndex: Int, val toIndex: Int) : PlayerIntent
    data class RemoveFromQueue(val index: Int) : PlayerIntent
    data object ClearQueue : PlayerIntent
    data object ToggleQueue : PlayerIntent
    data object UpdateProgress : PlayerIntent
    data object ToggleFavorite : PlayerIntent
    data object ToggleViewMode : PlayerIntent
}

sealed interface PlayerEffect {
    data class ShowToast(val message: String) : PlayerEffect
}

fun PlayerState.toUiState(): PlayerUiState {
    return PlayerUiState(
        currentSong = currentSong,
        playbackPosition = playbackPosition,
        duration = duration,
        isPlaying = isPlaying,
        playbackQueue = playbackQueue,
        queueIndex = queueIndex,
        repeatMode = repeatMode,
        shuffleMode = shuffleMode
    )
}
