package com.cycling.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.usecase.ClearSearchHistoryUseCase
import com.cycling.domain.usecase.GetSearchHistoryUseCase
import com.cycling.domain.usecase.GetSearchSuggestionsUseCase
import com.cycling.domain.usecase.SaveSearchHistoryUseCase
import com.cycling.domain.usecase.SearchAllUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
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
    private val getSearchHistoryUseCase: GetSearchHistoryUseCase,
    private val saveSearchHistoryUseCase: SaveSearchHistoryUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase,
    private val getSearchSuggestionsUseCase: GetSearchSuggestionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SearchEffect>()
    val uiEffect: SharedFlow<SearchEffect> = _uiEffect.asSharedFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        loadSearchHistory()
        observeSearchQuery()
    }

    private fun loadSearchHistory() {
        getSearchHistoryUseCase()
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
                        it.copy(songs = emptyList(), albums = emptyList(), artists = emptyList()) 
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
            is SearchIntent.AlbumClicked -> handleAlbumClick(intent.album)
            is SearchIntent.ArtistClicked -> handleArtistClick(intent.artist)
            is SearchIntent.ClearSearch -> handleClearSearch()
        }
    }

    private fun handleQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQuery.value = query
        
        if (query.isNotBlank()) {
            getSearchSuggestionsUseCase(query)
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
                saveSearchHistoryUseCase(query)
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
            clearSearchHistoryUseCase()
        }
    }

    private fun handleTabSelected(tab: SearchTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun handleSongClick(song: com.cycling.domain.model.Song) {
        viewModelScope.launch {
            saveSearchHistoryUseCase(_uiState.value.searchQuery)
            _uiEffect.emit(SearchEffect.NavigateToPlayer(song.id))
        }
    }

    private fun handleAlbumClick(album: com.cycling.domain.model.Album) {
        viewModelScope.launch {
            saveSearchHistoryUseCase(_uiState.value.searchQuery)
            _uiEffect.emit(SearchEffect.NavigateToAlbumDetail(album.id))
        }
    }

    private fun handleArtistClick(artist: com.cycling.domain.model.Artist) {
        viewModelScope.launch {
            saveSearchHistoryUseCase(_uiState.value.searchQuery)
            _uiEffect.emit(SearchEffect.NavigateToArtistDetail(artist.id))
        }
    }

    private fun handleClearSearch() {
        _uiState.update { 
            SearchUiState(searchHistory = _uiState.value.searchHistory) 
        }
        searchQuery.value = ""
    }

    private fun performSearch(query: String) {
        Timber.d("performSearch: query=$query")
        _uiState.update { it.copy(isSearching = true, error = null) }
        
        searchAllUseCase(query)
            .onEach { result ->
                Timber.d("performSearch: found ${result.songs.size} songs, ${result.albums.size} albums, ${result.artists.size} artists")
                _uiState.update { 
                    it.copy(
                        songs = result.songs,
                        albums = result.albums,
                        artists = result.artists,
                        isSearching = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
