package com.cycling.presentation.mymusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.Song
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.usecase.GetFavoriteSongsUseCase
import com.cycling.domain.usecase.GetMostPlayedSongsUseCase
import com.cycling.domain.usecase.GetRecentlyPlayedSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MyMusicViewModel @Inject constructor(
    private val getFavoriteSongsUseCase: GetFavoriteSongsUseCase,
    private val getRecentlyPlayedSongsUseCase: GetRecentlyPlayedSongsUseCase,
    private val getMostPlayedSongsUseCase: GetMostPlayedSongsUseCase,
    private val songRepository: SongRepository,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyMusicUiState())
    val uiState: StateFlow<MyMusicUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<MyMusicEffect>()
    val uiEffect: SharedFlow<MyMusicEffect> = _uiEffect.asSharedFlow()

    init {
        loadData()
    }

    fun handleIntent(intent: MyMusicIntent) {
        when (intent) {
            is MyMusicIntent.SelectTab -> selectTab(intent.tab)
            is MyMusicIntent.SongClick -> handleSongClick(intent.song)
            is MyMusicIntent.PlayAll -> playAll()
            is MyMusicIntent.ShuffleAll -> shuffleAll()
            is MyMusicIntent.ToggleFavorite -> toggleFavorite(intent.song)
        }
    }

    private fun loadData() {
        Timber.d("loadData: starting")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            launch {
                getFavoriteSongsUseCase().collectLatest { songs ->
                    Timber.d("loadData: loaded ${songs.size} favorites")
                    _uiState.update { it.copy(favoriteSongs = songs, isLoading = false) }
                }
            }
            
            launch {
                getRecentlyPlayedSongsUseCase().collectLatest { songs ->
                    Timber.d("loadData: loaded ${songs.size} recently played")
                    _uiState.update { it.copy(recentlyPlayedSongs = songs) }
                }
            }
            
            launch {
                getMostPlayedSongsUseCase().collectLatest { songs ->
                    Timber.d("loadData: loaded ${songs.size} most played")
                    _uiState.update { it.copy(mostPlayedSongs = songs) }
                }
            }
        }
    }

    private fun selectTab(tab: MyMusicTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    private fun handleSongClick(song: Song) {
        viewModelScope.launch {
            val currentSongs = getCurrentSongs()
            playerRepository.playSong(song, currentSongs)
            _uiEffect.emit(MyMusicEffect.NavigateToPlayer(song.id))
        }
    }

    private fun playAll() {
        val songs = getCurrentSongs()
        if (songs.isNotEmpty()) {
            viewModelScope.launch {
                playerRepository.playSong(songs.first(), songs)
            }
        }
    }

    private fun shuffleAll() {
        val songs = getCurrentSongs().shuffled()
        if (songs.isNotEmpty()) {
            viewModelScope.launch {
                playerRepository.playSong(songs.first(), songs)
            }
        }
    }

    private fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            songRepository.toggleFavorite(song.id)
            _uiEffect.emit(MyMusicEffect.ShowToast("已从喜欢移除"))
        }
    }

    private fun getCurrentSongs(): List<Song> {
        return when (_uiState.value.currentTab) {
            MyMusicTab.FAVORITES -> _uiState.value.favoriteSongs
            MyMusicTab.RECENTLY_PLAYED -> _uiState.value.recentlyPlayedSongs
            MyMusicTab.MOST_PLAYED -> _uiState.value.mostPlayedSongs
        }
    }
}
