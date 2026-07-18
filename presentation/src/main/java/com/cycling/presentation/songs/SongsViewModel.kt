package com.cycling.presentation.songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.Song
import com.cycling.domain.model.SortOrder
import com.cycling.domain.model.ViewMode
import com.cycling.domain.repository.PlaylistRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.repository.SongsPreferencesRepository
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
class SongsViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val songsPreferencesRepository: SongsPreferencesRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongsUiState())
    val uiState: StateFlow<SongsUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SongsEffect>()
    val uiEffect: SharedFlow<SongsEffect> = _uiEffect.asSharedFlow()

    init {
        loadPreferences()
        loadSongs()
        loadPlaylists()
    }

    fun handleIntent(intent: SongsIntent) {
        when (intent) {
            is SongsIntent.LoadSongs -> loadSongs()
            is SongsIntent.ToggleViewMode -> toggleViewMode()
            is SongsIntent.ChangeSortOrder -> changeSortOrder(intent.sortOrder)
            is SongsIntent.SongClick -> onSongClick(intent.song)
            is SongsIntent.ShowAddToPlaylistDialog -> showAddToPlaylistDialog(intent.song)
            is SongsIntent.AddToPlaylist -> addToPlaylist(intent.playlistId)
            is SongsIntent.DismissAddToPlaylistDialog -> dismissAddToPlaylistDialog()
            is SongsIntent.CreatePlaylistAndAddSong -> createPlaylistAndAddSong(intent.name)
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            val viewMode = songsPreferencesRepository.getViewMode()
            val sortOrder = songsPreferencesRepository.getSortOrder()
            val sortAscending = songsPreferencesRepository.getSortAscending()
            _uiState.update { it.copy(viewMode = viewMode, sortOrder = sortOrder, sortAscending = sortAscending) }
        }
    }

    private fun loadSongs() {
        Timber.d("loadSongs: starting")
        viewModelScope.launch {
            songRepository.getAllSongs().collect { songs ->
                Timber.d("loadSongs: loaded ${songs.size} songs")
                _uiState.update { it.copy(songs = songs, isLoading = false) }
            }
        }
    }

    private fun toggleViewMode() {
        viewModelScope.launch {
            val newViewMode = if (_uiState.value.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
            songsPreferencesRepository.saveViewMode(newViewMode)
            _uiState.update { it.copy(viewMode = newViewMode) }
        }
    }

    private fun changeSortOrder(newSortOrder: SortOrder) {
        viewModelScope.launch {
            val currentSortOrder = _uiState.value.sortOrder
            val newSortAscending = if (currentSortOrder == newSortOrder) {
                !_uiState.value.sortAscending
            } else {
                true
            }
            songsPreferencesRepository.saveSortOrder(newSortOrder)
            songsPreferencesRepository.saveSortAscending(newSortAscending)
            _uiState.update { it.copy(sortOrder = newSortOrder, sortAscending = newSortAscending) }
        }
    }

    private fun onSongClick(song: Song) {
        viewModelScope.launch {
            _uiEffect.emit(SongsEffect.NavigateToPlayer(song.id))
        }
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            playlistRepository.getAllPlaylists().collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
    }

    private fun showAddToPlaylistDialog(song: Song) {
        _uiState.update { it.copy(songToAdd = song, showAddToPlaylistDialog = true) }
    }

    private fun dismissAddToPlaylistDialog() {
        _uiState.update { it.copy(songToAdd = null, showAddToPlaylistDialog = false) }
    }

    private fun addToPlaylist(playlistId: Long) {
        val song = _uiState.value.songToAdd ?: return
        viewModelScope.launch {
            playlistRepository.addSongToPlaylist(playlistId, song.id)
            _uiEffect.emit(SongsEffect.ShowToast("已添加到播放列表"))
            dismissAddToPlaylistDialog()
        }
    }

    private fun createPlaylistAndAddSong(name: String) {
        val song = _uiState.value.songToAdd ?: return
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name)
            playlistRepository.addSongToPlaylist(playlistId, song.id)
            _uiEffect.emit(SongsEffect.ShowToast("已创建播放列表并添加歌曲"))
            dismissAddToPlaylistDialog()
        }
    }
}
