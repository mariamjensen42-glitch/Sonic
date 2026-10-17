package com.cycling.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.usecase.SearchHistoryUseCase
import com.cycling.domain.usecase.GetSearchSuggestionsUseCase
import com.cycling.domain.usecase.SearchAllUseCase
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val songRepository: SongRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SearchEffect>()
    val uiEffect: SharedFlow<SearchEffect> = _uiEffect.asSharedFlow()

    private val searchQuery = MutableStateFlow("")
    private var suggestionsJob: Job? = null

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
                    _uiState.update {
                        it.copy(
                            songs = emptyList(),
                            albums = emptyList(),
                            artists = emptyList(),
                            lyricsMatches = emptyList()
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
    }
}
