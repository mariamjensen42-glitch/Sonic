package com.cycling.presentation.playlistdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.repository.PlaylistRepository
import com.cycling.presentation.navigation.LibraryDestination
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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

@HiltViewModel(assistedFactory = PlaylistDetailViewModel.Factory::class)
class PlaylistDetailViewModel @AssistedInject constructor(
    private val playlistRepository: PlaylistRepository,
    @Assisted private val route: LibraryDestination.PlaylistDetail
) : ViewModel() {

    private val playlistId: Long = route.playlistId

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

    @AssistedFactory
    interface Factory {
        fun create(route: LibraryDestination.PlaylistDetail): PlaylistDetailViewModel
    }
}
