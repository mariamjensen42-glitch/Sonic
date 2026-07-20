package com.cycling.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.RepeatMode
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.usecase.GetLyricsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.cycling.presentation.R
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
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
            is PlayerIntent.PlaySong -> {
                Timber.d("handleIntent: PlaySong - ${intent.song.title}")
                playSong(intent.song, intent.queue)
            }
            is PlayerIntent.PlayFromQueue -> {
                Timber.d("handleIntent: PlayFromQueue - index=${intent.index}")
                playFromQueue(intent.index)
            }
            is PlayerIntent.PlayPause -> {
                Timber.d("handleIntent: PlayPause")
                playPause()
            }
            is PlayerIntent.SeekTo -> {
                Timber.d("handleIntent: SeekTo - position=${intent.position}")
                seekTo(intent.position)
            }
            is PlayerIntent.SkipToNext -> {
                Timber.d("handleIntent: SkipToNext")
                skipToNext()
            }
            is PlayerIntent.SkipToPrevious -> {
                Timber.d("handleIntent: SkipToPrevious")
                skipToPrevious()
            }
            is PlayerIntent.ToggleRepeatMode -> {
                Timber.d("handleIntent: ToggleRepeatMode")
                toggleRepeatMode()
            }
            is PlayerIntent.ToggleShuffleMode -> {
                Timber.d("handleIntent: ToggleShuffleMode")
                toggleShuffleMode()
            }
            is PlayerIntent.AddToQueue -> {
                Timber.d("handleIntent: AddToQueue - ${intent.song.title}")
                addToQueue(intent.song)
            }
            is PlayerIntent.PlayNext -> {
                Timber.d("handleIntent: PlayNext - ${intent.song.title}")
                playNext(intent.song)
            }
            is PlayerIntent.MoveQueueItem -> {
                Timber.d("handleIntent: MoveQueueItem - from=${intent.fromIndex} to=${intent.toIndex}")
                moveQueueItem(intent.fromIndex, intent.toIndex)
            }
            is PlayerIntent.RemoveFromQueue -> {
                Timber.d("handleIntent: RemoveFromQueue - index=${intent.index}")
                removeFromQueue(intent.index)
            }
            is PlayerIntent.ClearQueue -> {
                Timber.d("handleIntent: ClearQueue")
                clearQueue()
            }
            is PlayerIntent.ToggleQueue -> {
                Timber.d("handleIntent: ToggleQueue")
                toggleQueue()
            }
            is PlayerIntent.UpdateProgress -> { }
            is PlayerIntent.ToggleFavorite -> {
                Timber.d("handleIntent: ToggleFavorite")
                toggleFavorite()
            }
            is PlayerIntent.ToggleViewMode -> {
                Timber.d("handleIntent: ToggleViewMode")
                toggleViewMode()
            }
        }
    }

    private fun playSong(song: com.cycling.domain.model.Song, queue: List<com.cycling.domain.model.Song>) {
        playerRepository.playSong(song, queue)
    }

    private fun playFromQueue(index: Int) {
        val queue = _uiState.value.playbackQueue
        if (index in queue.indices) {
            playerRepository.playSong(queue[index], queue)
        }
    }

    private fun playPause() {
        playerRepository.playPause()
    }

    private fun seekTo(position: Long) {
        playerRepository.seekTo(position)
    }

    private fun skipToNext() {
        playerRepository.skipToNext()
    }

    private fun skipToPrevious() {
        playerRepository.skipToPrevious()
    }

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
        val newShuffleMode = !_uiState.value.shuffleMode
        playerRepository.setShuffleMode(newShuffleMode)
    }

    private fun addToQueue(song: com.cycling.domain.model.Song) {
        playerRepository.addToQueue(song)
        viewModelScope.launch {
            _uiEffect.emit(PlayerEffect.ShowToast(R.string.player_added_to_queue))
        }
    }

    private fun playNext(song: com.cycling.domain.model.Song) {
        playerRepository.playNext(song)
        viewModelScope.launch {
            _uiEffect.emit(PlayerEffect.ShowToast(R.string.player_play_next))
        }
    }

    private fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        playerRepository.moveQueueItem(fromIndex, toIndex)
    }

    private fun removeFromQueue(index: Int) {
        playerRepository.removeFromQueue(index)
    }

    private fun clearQueue() {
        playerRepository.clearQueue()
    }

    private fun toggleQueue() {
        _uiState.update { it.copy(showQueue = !it.showQueue) }
    }

    private fun toggleFavorite() {
        val currentSong = _uiState.value.currentSong ?: return
        viewModelScope.launch {
            val newFavoriteStatus = songRepository.toggleFavorite(currentSong.id)
            playerRepository.setSongFavorite(currentSong.id, newFavoriteStatus)
            _uiState.update { it.copy(isFavorite = newFavoriteStatus) }
            val message = if (newFavoriteStatus) R.string.player_added_to_favorites else R.string.player_removed_from_favorites
            _uiEffect.emit(PlayerEffect.ShowToast(message))
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
