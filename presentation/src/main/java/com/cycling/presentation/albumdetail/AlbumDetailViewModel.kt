package com.cycling.presentation.albumdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.repository.AlbumRepository
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
class AlbumDetailViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val songRepository: SongRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val albumId: Long = savedStateHandle.get<Long>("albumId") ?: -1L

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AlbumDetailEffect>()
    val uiEffect: SharedFlow<AlbumDetailEffect> = _uiEffect.asSharedFlow()

    init {
        loadAlbum()
    }

    fun handleIntent(intent: AlbumDetailIntent) {
        when (intent) {
            is AlbumDetailIntent.LoadAlbum -> {
                Timber.d("handleIntent: LoadAlbum")
                loadAlbum()
            }
            is AlbumDetailIntent.SongClick -> {
                Timber.d("handleIntent: SongClick songId=${intent.song.id}")
                onSongClick(intent.song)
            }
        }
    }

    private fun loadAlbum() {
        Timber.d("loadAlbum: starting albumId=$albumId")
        viewModelScope.launch {
            val album = albumRepository.getAlbumById(albumId)
            _uiState.update { it.copy(album = album) }
            
            songRepository.getSongsByAlbum(albumId).collect { songs ->
                Timber.d("loadAlbum: loaded ${songs.size} songs")
                _uiState.update { it.copy(songs = songs, isLoading = false) }
            }
        }
    }

    private fun onSongClick(song: com.cycling.domain.model.Song) {
        viewModelScope.launch {
            _uiEffect.emit(AlbumDetailEffect.NavigateToPlayer(song.id))
        }
    }
}
