package com.cycling.presentation.artistdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.repository.ArtistRepository
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SongRepository
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

@HiltViewModel(assistedFactory = ArtistDetailViewModel.Factory::class)
class ArtistDetailViewModel @AssistedInject constructor(
    private val artistRepository: ArtistRepository,
    private val songRepository: SongRepository,
    private val playerRepository: PlayerRepository,
    @Assisted private val route: LibraryDestination.ArtistDetail
) : ViewModel() {

    private val artistId: Long = route.artistId

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ArtistDetailEffect>()
    val uiEffect: SharedFlow<ArtistDetailEffect> = _uiEffect.asSharedFlow()

    init {
        loadArtist()
        observePlayerState()
    }

    fun handleIntent(intent: ArtistDetailIntent) {
        when (intent) {
            is ArtistDetailIntent.LoadArtist -> loadArtist()
            is ArtistDetailIntent.SongClick -> onSongClick(intent.song)
            is ArtistDetailIntent.ToggleFavorite -> toggleFavorite(intent.songId)
        }
    }

    private fun loadArtist() {
        Timber.d("loadArtist: starting artistId=$artistId")
        viewModelScope.launch {
            val artist = artistRepository.getArtistById(artistId)
            _uiState.update { it.copy(artist = artist) }

            songRepository.getSongsByArtist(artistId).collect { songs ->
                Timber.d("loadArtist: loaded ${songs.size} songs")
                val sortedSongs = songs.sortedBy { it.title }
                val likedIds = songs.filter { it.isFavorite }.map { it.id }.toSet()
                _uiState.update { it.copy(songs = sortedSongs, isLoading = false, likedSongIds = likedIds) }
            }
        }
    }

    private fun observePlayerState() {
        viewModelScope.launch {
            playerRepository.playerState.collect { state ->
                _uiState.update {
                    it.copy(
                        currentSongId = state.currentSong?.id,
                        isPlaying = state.isPlaying
                    )
                }
            }
        }
    }

    private fun onSongClick(song: com.cycling.domain.model.Song) {
        viewModelScope.launch {
            _uiEffect.emit(ArtistDetailEffect.NavigateToPlayer(song.id))
        }
    }

    private fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            val newState = songRepository.toggleFavorite(songId)
            val currentLiked = _uiState.value.likedSongIds.toMutableSet()
            if (newState) {
                currentLiked.add(songId)
            } else {
                currentLiked.remove(songId)
            }
            _uiState.update { it.copy(likedSongIds = currentLiked) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(route: LibraryDestination.ArtistDetail): ArtistDetailViewModel
    }
}
