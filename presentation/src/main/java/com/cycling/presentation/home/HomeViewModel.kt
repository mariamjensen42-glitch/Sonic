package com.cycling.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.usecase.GetAllAlbumsUseCase
import com.cycling.domain.usecase.GetAllArtistsUseCase
import com.cycling.domain.usecase.SongQueryUseCase
import com.cycling.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songQueryUseCase: SongQueryUseCase,
    private val getAllAlbumsUseCase: GetAllAlbumsUseCase,
    private val getAllArtistsUseCase: GetAllArtistsUseCase,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    val playerState = playerRepository.playerState

    private val _uiEffect = MutableSharedFlow<HomeEffect>()
    val uiEffect: SharedFlow<HomeEffect> = _uiEffect.asSharedFlow()

    private var loadJob: Job? = null

    /** 已经交付过的数据源。谁先到谁先画，不等其它源。 */
    private val deliveredSources = mutableSetOf<String>()

    /** 一轮载入最多报一次失败：六个源同时挂掉也只弹一条提示。 */
    private var failureReported = false

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
        }
    }

    private fun loadData() {
        Timber.d("loadData: starting")
        loadJob?.cancel()
        deliveredSources.clear()
        failureReported = false

        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(error = null).withLoadingFlag() }

            collectSource(SOURCE_SONGS, songQueryUseCase.all()) { state, songs ->
                state.copy(recentlyAdded = songs.sortedByDescending { it.dateAdded }.take(PREVIEW_SIZE))
            }

            collectSource(SOURCE_ALBUMS, getAllAlbumsUseCase()) { state, albums ->
                state.copy(topAlbums = albums.take(PREVIEW_SIZE))
            }

            collectSource(SOURCE_ARTISTS, getAllArtistsUseCase()) { state, artists ->
                state.copy(topArtists = artists.take(PREVIEW_SIZE))
            }

            collectSource(SOURCE_FAVORITES, songQueryUseCase.favorites()) { state, favorites ->
                state.copy(favoriteSongs = favorites.take(PREVIEW_SIZE))
            }

            collectSource(SOURCE_MOST_PLAYED, songQueryUseCase.mostPlayed()) { state, mostPlayed ->
                state.copy(mostPlayed = mostPlayed.take(PREVIEW_SIZE))
            }

            collectSource(SOURCE_RECENTLY_PLAYED, songQueryUseCase.recentlyPlayed()) { state, recentlyPlayed ->
                state.copy(recentlyPlayed = recentlyPlayed.take(PREVIEW_SIZE))
            }
        }
    }

    /**
     * 一个数据源：来了就画，挂了就记一笔并同样算作"已交付"，这样载入态不会永远挂着。
     *
     * 定义成 [CoroutineScope] 的扩展是为了让六个源都是 [loadJob] 的子协程：重新载入时
     * 上一轮的收集器会一起取消，不会越叠越多。
     */
    private fun <T> CoroutineScope.collectSource(
        name: String,
        flow: Flow<List<T>>,
        merge: (HomeUiState, List<T>) -> HomeUiState
    ) {
        launch {
            flow
                .catch { error -> onSourceFailed(name, error) }
                .collectLatest { items ->
                    deliveredSources += name
                    _uiState.update { state -> merge(state, items).withLoadingFlag() }
                }
        }
    }

    private suspend fun onSourceFailed(name: String, error: Throwable) {
        Timber.e(error, "loadData: %s failed", name)
        deliveredSources += name
        _uiState.update { it.withLoadingFlag() }
        if (!failureReported) {
            failureReported = true
            _uiEffect.emit(HomeEffect.ShowToast(R.string.home_vm_load_failed))
        }
    }

    /**
     * 载入态只表示"眼下没有任何内容可看"：有内容就立刻放行，哪怕专辑、歌手还在路上；
     * 六个源全部到齐而且仍然全空，才算真的空库。
     *
     * 旧实现要等六个源齐了才放行，首页的最短可见时间因此等于两次 MediaStore 查询里慢的那个。
     */
    private fun HomeUiState.withLoadingFlag(): HomeUiState {
        val hasContent = recentlyPlayed.isNotEmpty() || recentlyAdded.isNotEmpty() ||
            topAlbums.isNotEmpty() || topArtists.isNotEmpty() ||
            mostPlayed.isNotEmpty() || favoriteSongs.isNotEmpty()
        return copy(isLoading = !hasContent && deliveredSources.size < SOURCE_COUNT)
    }

    private fun handleSongClick(song: com.cycling.domain.model.Song) {
        viewModelScope.launch {
            playerRepository.playSong(song)
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

    private companion object {
        /** 首页每个分区最多露出的条目数。 */
        const val PREVIEW_SIZE = 10

        const val SOURCE_SONGS = "songs"
        const val SOURCE_ALBUMS = "albums"
        const val SOURCE_ARTISTS = "artists"
        const val SOURCE_FAVORITES = "favorites"
        const val SOURCE_MOST_PLAYED = "mostPlayed"
        const val SOURCE_RECENTLY_PLAYED = "recentlyPlayed"

        /** 上面这组的数量。全部交付仍无内容，才算空库。 */
        const val SOURCE_COUNT = 6
    }
}
