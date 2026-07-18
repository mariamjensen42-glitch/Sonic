package com.cycling.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.usecase.GetAllAlbumsUseCase
import com.cycling.domain.usecase.GetAllArtistsUseCase
import com.cycling.domain.usecase.GetAllSongsUseCase
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
class HomeViewModel @Inject constructor(
    private val getAllSongsUseCase: GetAllSongsUseCase,
    private val getAllAlbumsUseCase: GetAllAlbumsUseCase,
    private val getAllArtistsUseCase: GetAllArtistsUseCase,
    private val getFavoriteSongsUseCase: GetFavoriteSongsUseCase,
    private val getMostPlayedSongsUseCase: GetMostPlayedSongsUseCase,
    private val getRecentlyPlayedSongsUseCase: GetRecentlyPlayedSongsUseCase,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    val playerState = playerRepository.playerState

    private val _uiEffect = MutableSharedFlow<HomeEffect>()
    val uiEffect: SharedFlow<HomeEffect> = _uiEffect.asSharedFlow()

    private var songsLoaded = false
    private var albumsLoaded = false
    private var artistsLoaded = false
    private var favoritesLoaded = false
    private var mostPlayedLoaded = false
    private var recentlyPlayedLoaded = false

    init {
        loadData()
    }

    fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadData -> {
                Timber.d("handleIntent: LoadData")
                loadData()
            }
            is HomeIntent.SongClick -> {
                Timber.d("handleIntent: SongClick songId=${intent.song.id}")
                handleSongClick(intent.song)
            }
            is HomeIntent.AlbumClick -> {
                Timber.d("handleIntent: AlbumClick albumId=${intent.album.id}")
                handleAlbumClick(intent.album)
            }
            is HomeIntent.ArtistClick -> {
                Timber.d("handleIntent: ArtistClick artistId=${intent.artist.id}")
                handleArtistClick(intent.artist)
            }
            is HomeIntent.NavigateToSongs -> {
                Timber.d("handleIntent: NavigateToSongs")
                navigateTo(HomeEffect.NavigateToSongs)
            }
            is HomeIntent.NavigateToAlbums -> {
                Timber.d("handleIntent: NavigateToAlbums")
                navigateTo(HomeEffect.NavigateToAlbums)
            }
            is HomeIntent.NavigateToArtists -> {
                Timber.d("handleIntent: NavigateToArtists")
                navigateTo(HomeEffect.NavigateToArtists)
            }
            is HomeIntent.NavigateToPlaylists -> {
                Timber.d("handleIntent: NavigateToPlaylists")
                navigateTo(HomeEffect.NavigateToPlaylists)
            }
            is HomeIntent.NavigateToSettings -> {
                Timber.d("handleIntent: NavigateToSettings")
                navigateTo(HomeEffect.NavigateToSettings)
            }
            is HomeIntent.NavigateToMyMusic -> {
                Timber.d("handleIntent: NavigateToMyMusic")
                navigateTo(HomeEffect.NavigateToMyMusic)
            }
            is HomeIntent.NavigateToSearch -> {
                Timber.d("handleIntent: NavigateToSearch")
                navigateTo(HomeEffect.NavigateToSearch)
            }
            is HomeIntent.NavigateToFolders -> {
                Timber.d("handleIntent: NavigateToFolders")
                navigateTo(HomeEffect.NavigateToFolders)
            }
            is HomeIntent.PlayPause -> {
                Timber.d("handleIntent: PlayPause")
                playerRepository.playPause()
            }
            is HomeIntent.MiniPlayerClick -> {
                Timber.d("handleIntent: MiniPlayerClick")
                handleMiniPlayerClick()
            }
        }
    }
    
    private fun handleMiniPlayerClick() {
        viewModelScope.launch {
            playerRepository.getCurrentState().currentSong?.let { song ->
                _uiEffect.emit(HomeEffect.NavigateToPlayer(song.id))
            }
        }
    }

    private fun loadData() {
        Timber.d("loadData: starting")
        songsLoaded = false
        albumsLoaded = false
        artistsLoaded = false
        favoritesLoaded = false
        mostPlayedLoaded = false
        recentlyPlayedLoaded = false
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            launch {
                getAllSongsUseCase().collectLatest { songs ->
                    Timber.d("loadData: loaded ${songs.size} songs")
                    _uiState.update { state ->
                        state.copy(
                            recentlyAdded = songs.sortedByDescending { it.dateAdded }.take(10)
                        )
                    }
                    songsLoaded = true
                    checkAllDataLoaded()
                }
            }
            
            launch {
                getAllAlbumsUseCase().collectLatest { albums ->
                    Timber.d("loadData: loaded ${albums.size} albums")
                    _uiState.update { state ->
                        state.copy(topAlbums = albums.take(10))
                    }
                    albumsLoaded = true
                    checkAllDataLoaded()
                }
            }
            
            launch {
                getAllArtistsUseCase().collectLatest { artists ->
                    Timber.d("loadData: loaded ${artists.size} artists")
                    _uiState.update { state ->
                        state.copy(topArtists = artists.take(10))
                    }
                    artistsLoaded = true
                    checkAllDataLoaded()
                }
            }

            launch {
                getFavoriteSongsUseCase().collectLatest { favorites ->
                    Timber.d("loadData: loaded ${favorites.size} favorites")
                    _uiState.update { state ->
                        state.copy(favoriteSongs = favorites.take(10))
                    }
                    favoritesLoaded = true
                    checkAllDataLoaded()
                }
            }

            launch {
                getMostPlayedSongsUseCase().collectLatest { mostPlayed ->
                    Timber.d("loadData: loaded ${mostPlayed.size} mostPlayed")
                    _uiState.update { state ->
                        state.copy(mostPlayed = mostPlayed.take(10))
                    }
                    mostPlayedLoaded = true
                    checkAllDataLoaded()
                }
            }

            launch {
                getRecentlyPlayedSongsUseCase().collectLatest { recentlyPlayed ->
                    Timber.d("loadData: loaded ${recentlyPlayed.size} recentlyPlayed")
                    _uiState.update { state ->
                        state.copy(recentlyPlayed = recentlyPlayed.take(10))
                    }
                    recentlyPlayedLoaded = true
                    checkAllDataLoaded()
                }
            }
        }
    }
    
    private fun checkAllDataLoaded() {
        if (songsLoaded && albumsLoaded && artistsLoaded && favoritesLoaded && mostPlayedLoaded && recentlyPlayedLoaded) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun handleSongClick(song: com.cycling.domain.model.Song) {
        viewModelScope.launch {
            _uiEffect.emit(HomeEffect.NavigateToPlayer(song.id))
        }
    }

    private fun handleAlbumClick(album: com.cycling.domain.model.Album) {
        viewModelScope.launch {
            _uiEffect.emit(HomeEffect.NavigateToAlbumDetail(album.id))
        }
    }

    private fun handleArtistClick(artist: com.cycling.domain.model.Artist) {
        viewModelScope.launch {
            _uiEffect.emit(HomeEffect.NavigateToArtistDetail(artist.id))
        }
    }

    private fun navigateTo(effect: HomeEffect) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }
}
