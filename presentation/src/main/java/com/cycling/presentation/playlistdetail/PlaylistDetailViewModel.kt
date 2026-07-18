package com.cycling.presentation.playlistdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: Long = savedStateHandle.get<Long>("playlistId") ?: -1L

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PlaylistDetailEffect>()
    val uiEffect: SharedFlow<PlaylistDetailEffect> = _uiEffect.asSharedFlow()

    init {
        loadPlaylist()
    }

    fun handleIntent(intent: PlaylistDetailIntent) {
        when (intent) {
            is PlaylistDetailIntent.SongClick -> {
                Timber.d("handleIntent: SongClick songId=${intent.song.id}")
                onSongClick(intent.song)
            }
            is PlaylistDetailIntent.RemoveSong -> {
                Timber.d("handleIntent: RemoveSong songId=${intent.songId}")
                removeSong(intent.songId)
            }
        }
    }

    private fun loadPlaylist() {
        Timber.d("loadPlaylist: starting playlistId=$playlistId")
        viewModelScope.launch {
            val playlist = playlistRepository.getPlaylistById(playlistId)
            _uiState.update { it.copy(playlist = playlist) }

            playlistRepository.getSongsInPlaylist(playlistId).collect { songs ->
                Timber.d("loadPlaylist: loaded ${songs.size} songs")
                _uiState.update { it.copy(songs = songs, isLoading = false) }
            }
        }
    }

    private fun onSongClick(song: com.cycling.domain.model.Song) {
        viewModelScope.launch {
            _uiEffect.emit(PlaylistDetailEffect.NavigateToPlayer(song.id))
        }
    }

    private fun removeSong(songId: Long) {
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(playlistId, songId)
            _uiEffect.emit(PlaylistDetailEffect.ShowToast("已从播放列表移除"))
        }
    }
}
