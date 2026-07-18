package com.cycling.presentation.artistdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.repository.ArtistRepository
import com.cycling.domain.repository.SongRepository
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
class ArtistDetailViewModel @Inject constructor(
    private val artistRepository: ArtistRepository,
    private val songRepository: SongRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val artistId: Long = savedStateHandle.get<Long>("artistId") ?: -1L

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ArtistDetailEffect>()
    val uiEffect: SharedFlow<ArtistDetailEffect> = _uiEffect.asSharedFlow()

    init {
        loadArtist()
    }

    fun handleIntent(intent: ArtistDetailIntent) {
        when (intent) {
            is ArtistDetailIntent.LoadArtist -> loadArtist()
            is ArtistDetailIntent.SongClick -> onSongClick(intent.song)
        }
    }

    private fun loadArtist() {
        Timber.d("loadArtist: starting artistId=$artistId")
        viewModelScope.launch {
            val artist = artistRepository.getArtistById(artistId)
            _uiState.update { it.copy(artist = artist) }
            
            songRepository.getSongsByArtist(artistId).collect { songs ->
                Timber.d("loadArtist: loaded ${songs.size} songs")
                _uiState.update { it.copy(songs = songs, isLoading = false) }
            }
        }
    }

    private fun onSongClick(song: com.cycling.domain.model.Song) {
        viewModelScope.launch {
            _uiEffect.emit(ArtistDetailEffect.NavigateToPlayer(song.id))
        }
    }
}
