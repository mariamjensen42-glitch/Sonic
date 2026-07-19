package com.cycling.presentation.playlistdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.presentation.R
import com.cycling.domain.repository.PlaylistRepository
import com.cycling.domain.repository.PlayerRepository
import com.cycling.presentation.navigation.LibraryDestination
import com.cycling.core.ui.theme.ArtColorExtractor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltViewModel(assistedFactory = PlaylistDetailViewModel.Factory::class)
class PlaylistDetailViewModel @AssistedInject constructor(
    private val playlistRepository: PlaylistRepository,
    private val playerRepository: PlayerRepository,
    @Assisted private val route: LibraryDestination.PlaylistDetail
) : ViewModel() {

    private val playlistId: Long = route.playlistId

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PlaylistDetailEffect>()
    val uiEffect: SharedFlow<PlaylistDetailEffect> = _uiEffect.asSharedFlow()

    init {
        loadPlaylist()
        observePlayerState()
    }

    private fun observePlayerState() {
        viewModelScope.launch {
            playerRepository.playerState.collect { state ->
                _uiState.update { it.copy(currentSongId = state.currentSong?.id) }
            }
        }
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
                // Extract accent color from first song in the background
                var accentColor = _uiState.value.accentColor
                val firstSong = songs.firstOrNull()
                if (firstSong != null) {
                    accentColor = withContext(Dispatchers.IO) {
                        ArtColorExtractor.extractColorFromMusicFile(firstSong.path, accentColor)
                    }
                }
                _uiState.update { it.copy(songs = songs, isLoading = false, accentColor = accentColor) }
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
            _uiEffect.emit(PlaylistDetailEffect.ShowToast(R.string.playlist_detail_removed_toast))
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(route: LibraryDestination.PlaylistDetail): PlaylistDetailViewModel
    }
}
