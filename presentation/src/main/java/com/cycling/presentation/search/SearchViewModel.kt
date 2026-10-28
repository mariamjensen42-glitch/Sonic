package com.cycling.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.model.MusicQuality
import com.cycling.domain.online.model.OnlineSource
import com.cycling.domain.online.model.PlaySourceResolution
import com.cycling.domain.online.model.toSong
import com.cycling.domain.online.usecase.ResolveOnlinePlaySourceUseCase
import com.cycling.domain.online.usecase.SearchOnlineMusicUseCase
import com.cycling.domain.usecase.SearchHistoryUseCase
import com.cycling.domain.usecase.GetSearchSuggestionsUseCase
import com.cycling.domain.usecase.SearchAllUseCase
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchAllUseCase: SearchAllUseCase,
    private val searchHistoryUseCase: SearchHistoryUseCase,
    private val getSearchSuggestionsUseCase: GetSearchSuggestionsUseCase,
    private val playerRepository: PlayerRepository,
    private val songRepository: SongRepository,
    private val searchOnlineMusic: SearchOnlineMusicUseCase,
    private val resolveOnlinePlaySource: ResolveOnlinePlaySourceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SearchEffect>()
    val uiEffect: SharedFlow<SearchEffect> = _uiEffect.asSharedFlow()

    private val searchQuery = MutableStateFlow("")
    private var suggestionsJob: Job? = null
    private var onlineJob: Job? = null

    init {
        loadSearchHistory()
        observeSearchQuery()
    }

    private fun loadSearchHistory() {
        searchHistoryUseCase.getAll()
            .onEach { history ->
                _uiState.update { it.copy(searchHistory = history) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeSearchQuery() {
        searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isNotBlank()) {
                    performSearch(query)
                } else {
                    onlineJob?.cancel()
                    _uiState.update {
                        it.copy(
                            songs = emptyList(),
                            albums = emptyList(),
                            artists = emptyList(),
                            lyricsMatches = emptyList(),
                            onlineResults = emptyList(),
                            onlineSearching = false,
                            onlinePage = 1,
                            onlineHasMore = false,
                            onlineLoadingMore = false,
                            selectedOnlineSource = null
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.SearchQueryChanged -> handleQueryChanged(intent.query)
            is SearchIntent.SearchSubmitted -> handleSearchSubmitted(intent.query)
            is SearchIntent.HistoryItemClicked -> handleHistoryClick(intent.query)
            is SearchIntent.ClearHistory -> handleClearHistory()
            is SearchIntent.TabSelected -> handleTabSelected(intent.tab)
            is SearchIntent.SongClicked -> handleSongClick(intent.song)
            is SearchIntent.ToggleFavorite -> handleToggleFavorite(intent.song)
            is SearchIntent.AlbumClicked -> handleAlbumClick(intent.album)
            is SearchIntent.ArtistClicked -> handleArtistClick(intent.artist)
            is SearchIntent.OnlineTrackClicked -> playOnline(intent.track)
            is SearchIntent.OnlineSourceSelected -> handleOnlineSourceSelected(intent.source)
            is SearchIntent.LoadMoreOnline -> loadMoreOnline()
            is SearchIntent.ClearSearch -> handleClearSearch()
        }
    }

    private fun handleQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQuery.value = query
        
        if (query.isNotBlank()) {
            suggestionsJob?.cancel()
            suggestionsJob = getSearchSuggestionsUseCase(query)
                .onEach { suggestions ->
                    _uiState.update { it.copy(suggestions = suggestions) }
                }
                .launchIn(viewModelScope)
        } else {
            _uiState.update { it.copy(suggestions = emptyList()) }
        }
    }

    private fun handleSearchSubmitted(query: String) {
        if (query.isNotBlank()) {
            viewModelScope.launch {
                searchHistoryUseCase.save(query)
            }
            performSearch(query)
        }
    }

    private fun handleHistoryClick(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQuery.value = query
        performSearch(query)
    }

    private fun handleClearHistory() {
        viewModelScope.launch {
            searchHistoryUseCase.clear()
        }
    }

    private fun handleTabSelected(tab: SearchTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun handleOnlineSourceSelected(source: OnlineSource?) {
        _uiState.update { it.copy(selectedOnlineSource = source) }
    }

    private fun handleSongClick(song: com.cycling.domain.model.Song) {
        viewModelScope.launch {
            playerRepository.playSong(song)
            searchHistoryUseCase.save(_uiState.value.searchQuery)
            _uiEffect.emit(SearchEffect.NavigateToPlayer(song.id))
        }
    }

    private fun handleAlbumClick(album: com.cycling.domain.model.Album) {
        viewModelScope.launch {
            searchHistoryUseCase.save(_uiState.value.searchQuery)
            _uiEffect.emit(SearchEffect.NavigateToAlbumDetail(album.id))
        }
    }

    private fun handleArtistClick(artist: com.cycling.domain.model.Artist) {
        viewModelScope.launch {
            searchHistoryUseCase.save(_uiState.value.searchQuery)
            _uiEffect.emit(SearchEffect.NavigateToArtistDetail(artist.id))
        }
    }

    private fun handleClearSearch() {
        _uiState.update {
            SearchUiState(searchHistory = _uiState.value.searchHistory)
        }
        searchQuery.value = ""
    }

    /**
     * 只把新状态写回结果列表，不重新跑一次搜索：结果里每一行的 Song 都来自同一次查询，
     * 重查会让列表闪一下，而收藏位与查询条件本来就无关。
     */
    private fun handleToggleFavorite(song: com.cycling.domain.model.Song) {
        viewModelScope.launch {
            val isFavorite = songRepository.toggleFavorite(song.id)
            _uiState.update { state ->
                state.copy(
                    songs = state.songs.map { if (it.id == song.id) it.copy(isFavorite = isFavorite) else it }
                )
            }
        }
    }

    private fun performSearch(query: String) {
        Timber.d("performSearch: query=$query")
        _uiState.update { it.copy(isSearching = true, error = null) }
        
        searchAllUseCase(query)
            .onEach { result ->
                Timber.d(
                    "performSearch: ${result.songs.size} songs, ${result.albums.size} albums, " +
                        "${result.artists.size} artists, ${result.lyricsMatches.size} lyrics"
                )
                _uiState.update {
                    it.copy(
                        songs = result.songs,
                        albums = result.albums,
                        artists = result.artists,
                        lyricsMatches = result.lyricsMatches,
                        isSearching = false
                    )
                }
            }
            .launchIn(viewModelScope)

        searchOnline(query)
    }

    /**
     * 在线结果与本地结果并行跑：本地是 Room，毫秒级；在线要打三个平台的接口，
     * 串行会让本地结果陪着一起等。失败只把在线区块留空，不影响本地搜索。
     */
    private fun searchOnline(query: String) {
        onlineJob?.cancel()
        onlineJob = viewModelScope.launch {
            _uiState.update { it.copy(onlineSearching = true, onlinePage = 1, selectedOnlineSource = null) }
            // 不能用 runCatching：它会把取消也吞掉，于是「刚被新关键词取消的旧请求」
            // 反而用空结果覆盖掉新请求的状态。
            val outcome = try {
                searchOnlineMusic(query, page = 1)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "searchOnline failed: $query")
                null
            }
            _uiState.update {
                it.copy(
                    onlineResults = outcome?.tracks.orEmpty(),
                    onlineHasSources = outcome?.hasSources ?: it.onlineHasSources,
                    onlineSearching = false,
                    onlineHasMore = !outcome?.tracks.isNullOrEmpty()
                )
            }
        }
    }

    /**
     * 翻下一页：各平台每页 10 条，5 个平台就是 50 条。
     *
     * 追加而不是替换，并按 `平台:id` 去重 —— 翻页时接口偶尔会重复给出同一条
     * （尤其是关键词很短的时候），重复 key 会让 LazyColumn 直接崩。
     */
    private fun loadMoreOnline() {
        val state = _uiState.value
        val query = state.searchQuery
        if (query.isBlank() || state.onlineSearching || state.onlineLoadingMore) return
        if (!state.onlineHasMore) return
        onlineJob = viewModelScope.launch {
            _uiState.update { it.copy(onlineLoadingMore = true) }
            val nextPage = state.onlinePage + 1
            val outcome = try {
                searchOnlineMusic(query, page = nextPage)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "loadMoreOnline failed: $query page=$nextPage")
                null
            }
            val known = state.onlineResults.map { it.onlineKey }.toSet()
            val appended = outcome?.tracks.orEmpty().filter { it.onlineKey !in known }
            _uiState.update {
                it.copy(
                    onlineResults = it.onlineResults + appended,
                    onlinePage = nextPage,
                    onlineLoadingMore = false,
                    onlineHasMore = !outcome?.tracks.isNullOrEmpty()
                )
            }
        }
    }

    /**
     * 在线歌曲播放：先用平台歌曲 id 让音源脚本取链，再交给既有播放器。
     *
     * 档位固定 320k（`EXHIGH`）：插件会自己沿档位向下降级，而更高档位大多要脚本侧的付费授权，
     * 一上来就试无损只会把「能播」变成「报错」。
     */
    private fun playOnline(track: MusicInfo) {
        if (_uiState.value.resolvingOnlineKey != null) return
        _uiState.update { it.copy(resolvingOnlineKey = track.onlineKey) }
        viewModelScope.launch {
            val resolution = try {
                resolveOnlinePlaySource(track, MusicQuality.EXHIGH)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PlaySourceResolution.Failure(e.message ?: "未知错误")
            }
            when (resolution) {
                is PlaySourceResolution.Success -> {
                    val song = track.toSong(resolution.source)
                    // 指针入库：在线曲目才能被收藏、进歌单，下次播放时由 PlayerManager 重新取链。
                    // 取链地址本身不写库 —— 它会过期。
                    songRepository.upsertOnlineSong(song)
                    playerRepository.playSong(song, listOf(song))
                    searchHistoryUseCase.save(_uiState.value.searchQuery)
                    _uiEffect.emit(SearchEffect.NavigateToPlayer(song.id))
                }

                is PlaySourceResolution.Failure -> _uiEffect.emit(
                    SearchEffect.ShowToast(R.string.search_online_failed, resolution.message)
                )

                PlaySourceResolution.NotConfigured -> _uiEffect.emit(
                    SearchEffect.ShowToast(R.string.search_online_not_configured)
                )
            }
            _uiState.update { it.copy(resolvingOnlineKey = null) }
        }
    }
}
