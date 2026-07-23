package com.cycling.sonic.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.sonic.domain.model.RepeatMode
import com.cycling.sonic.domain.model.Song
import com.cycling.sonic.domain.repository.PlayerRepository
import com.cycling.sonic.domain.repository.SongRepository
import com.cycling.sonic.domain.usecase.GetLyricsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val playerRepository: PlayerRepository,
    private val songRepository: SongRepository,
    private val getLyricsUseCase: GetLyricsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PlayerEffect>()
    val uiEffect: SharedFlow<PlayerEffect> = _uiEffect.asSharedFlow()

    private var lastLyricsSongPath: String? = null

    init {
        observePlayerState()
    }

    private fun observePlayerState() {
        viewModelScope.launch {
            playerRepository.playerState.collect { state ->
                val currentSong = state.currentSong
                val songChanged = currentSong?.path != null && currentSong.path != lastLyricsSongPath
                _uiState.update { prev ->
                    state.toUiState().copy(
                        showQueue = prev.showQueue,
                        viewMode = prev.viewMode,
                        lyrics = if (songChanged) null else prev.lyrics,
                        isLoadingLyrics = if (songChanged) false else prev.isLoadingLyrics,
                        hasLyrics = if (songChanged) false else prev.hasLyrics,
                        isFavorite = if (currentSong?.id != null && currentSong.id == prev.currentSong?.id) prev.isFavorite else currentSong?.isFavorite ?: false
                    )
                }

                if (currentSong != null && currentSong.path != lastLyricsSongPath) {
                    lastLyricsSongPath = currentSong.path
                    loadLyrics(currentSong.path)
                }
            }
        }
    }

    private fun loadLyrics(songPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLyrics = true) }

            val result = getLyricsUseCase(songPath)
            val syncedLyrics = result.syncedLyrics

            _uiState.update {
                it.copy(
                    lyrics = syncedLyrics,
                    isLoadingLyrics = false,
                    hasLyrics = syncedLyrics != null && syncedLyrics.lines.isNotEmpty()
                )
            }
        }
    }

    fun handleIntent(intent: PlayerIntent) {
        when (intent) {
            is PlayerIntent.PlaySong -> playSong(intent.song, intent.queue)
            is PlayerIntent.PlayFromQueue -> playFromQueue(intent.index)
            is PlayerIntent.PlayPause -> playPause()
            is PlayerIntent.SeekTo -> seekTo(intent.position)
            is PlayerIntent.SkipToNext -> skipToNext()
            is PlayerIntent.SkipToPrevious -> skipToPrevious()
            is PlayerIntent.ToggleRepeatMode -> toggleRepeatMode()
            is PlayerIntent.ToggleShuffleMode -> toggleShuffleMode()
            is PlayerIntent.AddToQueue -> addToQueue(intent.song)
            is PlayerIntent.PlayNext -> playNext(intent.song)
            is PlayerIntent.MoveQueueItem -> moveQueueItem(intent.fromIndex, intent.toIndex)
            is PlayerIntent.RemoveFromQueue -> removeFromQueue(intent.index)
            is PlayerIntent.ClearQueue -> clearQueue()
            is PlayerIntent.ToggleQueue -> toggleQueue()
            is PlayerIntent.UpdateProgress -> { }
            is PlayerIntent.ToggleFavorite -> toggleFavorite()
            is PlayerIntent.ToggleViewMode -> toggleViewMode()
        }
    }

    private fun playSong(song: Song, queue: List<Song>) {
        playerRepository.playSong(song, queue)
    }

    private fun playFromQueue(index: Int) {
        val queue = _uiState.value.playbackQueue
        if (index in queue.indices) {
            playerRepository.playSong(queue[index], queue)
        }
    }

    private fun playPause() { playerRepository.playPause() }
    private fun seekTo(position: Long) { playerRepository.seekTo(position) }
    private fun skipToNext() { playerRepository.skipToNext() }
    private fun skipToPrevious() { playerRepository.skipToPrevious() }

    private fun toggleRepeatMode() {
        val currentMode = _uiState.value.repeatMode
        val newMode = when (currentMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        playerRepository.setRepeatMode(newMode)
    }

    private fun toggleShuffleMode() {
        playerRepository.setShuffleMode(!_uiState.value.shuffleMode)
    }

    private fun addToQueue(song: Song) {
        playerRepository.addToQueue(song)
        viewModelScope.launch { _uiEffect.emit(PlayerEffect.ShowToast("已添加到队列")) }
    }

    private fun playNext(song: Song) {
        playerRepository.playNext(song)
        viewModelScope.launch { _uiEffect.emit(PlayerEffect.ShowToast("下一首播放")) }
    }

    private fun moveQueueItem(fromIndex: Int, toIndex: Int) { playerRepository.moveQueueItem(fromIndex, toIndex) }
    private fun removeFromQueue(index: Int) { playerRepository.removeFromQueue(index) }
    private fun clearQueue() { playerRepository.clearQueue() }

    private fun toggleQueue() {
        _uiState.update { it.copy(showQueue = !it.showQueue) }
    }

    private fun toggleFavorite() {
        val currentSong = _uiState.value.currentSong ?: return
        viewModelScope.launch {
            val newFavoriteStatus = songRepository.toggleFavorite(currentSong.id)
            playerRepository.setSongFavorite(currentSong.id, newFavoriteStatus)
            _uiState.update { it.copy(isFavorite = newFavoriteStatus) }
            _uiEffect.emit(
                PlayerEffect.ShowToast(if (newFavoriteStatus) "已添加到收藏" else "已取消收藏")
            )
        }
    }

    private fun toggleViewMode() {
        _uiState.update { state ->
            val newViewMode = when (state.viewMode) {
                PlayerViewMode.COVER -> PlayerViewMode.LYRICS
                PlayerViewMode.LYRICS -> PlayerViewMode.COVER
            }
            state.copy(viewMode = newViewMode)
        }
    }
}
