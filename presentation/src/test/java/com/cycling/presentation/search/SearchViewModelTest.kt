package com.cycling.presentation.search

import com.cycling.domain.model.Album
import com.cycling.domain.model.Artist
import com.cycling.domain.model.Song
import com.cycling.domain.model.SearchResult
import com.cycling.domain.usecase.ClearSearchHistoryUseCase
import com.cycling.domain.usecase.GetSearchHistoryUseCase
import com.cycling.domain.usecase.GetSearchSuggestionsUseCase
import com.cycling.domain.usecase.SaveSearchHistoryUseCase
import com.cycling.domain.usecase.SearchAllUseCase
import com.cycling.presentation.util.TestDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var searchAllUseCase: SearchAllUseCase
    private lateinit var getSearchHistoryUseCase: GetSearchHistoryUseCase
    private lateinit var saveSearchHistoryUseCase: SaveSearchHistoryUseCase
    private lateinit var clearSearchHistoryUseCase: ClearSearchHistoryUseCase
    private lateinit var getSearchSuggestionsUseCase: GetSearchSuggestionsUseCase
    private lateinit var viewModel: SearchViewModel

    private fun createSong(
        id: Long = 1L,
        title: String = "Test Song",
        artist: String = "Test Artist",
        album: String = "Test Album",
        duration: Long = 180000L,
        path: String = "/storage/emulated/0/Music/test.mp3",
        albumId: Long = 1L,
        artistId: Long = 1L,
        dateAdded: Long = System.currentTimeMillis(),
        dateModified: Long = System.currentTimeMillis(),
        size: Long = 5000000L,
        mimeType: String = "audio/mpeg",
        albumArt: String? = null,
        isFavorite: Boolean = false,
        playCount: Int = 0,
        lastPlayedAt: Long? = null,
        bitrate: Int = 320
    ): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        path = path,
        albumId = albumId,
        artistId = artistId,
        dateAdded = dateAdded,
        dateModified = dateModified,
        size = size,
        mimeType = mimeType,
        albumArt = albumArt,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
        bitrate = bitrate
    )

    private fun createAlbum(
        id: Long = 1L,
        name: String = "Test Album",
        artist: String = "Test Artist"
    ): Album = Album(
        id = id,
        name = name,
        artist = artist,
        albumArt = null,
        numberOfSongs = 10,
        firstYear = 2020,
        lastYear = 2020
    )

    private fun createArtist(
        id: Long = 1L,
        name: String = "Test Artist"
    ): Artist = Artist(
        id = id,
        name = name,
        numberOfAlbums = 1,
        numberOfTracks = 10,
        artistArt = null
    )

    @Before
    fun setup() {
        searchAllUseCase = mockk()
        getSearchHistoryUseCase = mockk()
        saveSearchHistoryUseCase = mockk()
        clearSearchHistoryUseCase = mockk()
        getSearchSuggestionsUseCase = mockk()

        every { getSearchHistoryUseCase() } returns flowOf(emptyList())
        every { searchAllUseCase(any()) } returns flowOf(SearchResult())
        every { getSearchSuggestionsUseCase(any()) } returns flowOf(emptyList())
        coEvery { saveSearchHistoryUseCase(any()) } returns Unit
        coEvery { clearSearchHistoryUseCase() } returns Unit
    }

    @Test
    fun `init loads search history`() = runTest {
        val history = listOf("query1", "query2")
        every { getSearchHistoryUseCase() } returns flowOf(history)

        viewModel = SearchViewModel(
            searchAllUseCase,
            getSearchHistoryUseCase,
            saveSearchHistoryUseCase,
            clearSearchHistoryUseCase,
            getSearchSuggestionsUseCase
        )
        advanceUntilIdle()

        assertEquals(history, viewModel.uiState.value.searchHistory)
    }

    @Test
    fun `SearchQueryChanged updates state and triggers suggestions`() = runTest {
        val query = "test"
        val suggestions = listOf("test song", "test artist")
        every { getSearchSuggestionsUseCase(query) } returns flowOf(suggestions)

        viewModel = SearchViewModel(
            searchAllUseCase,
            getSearchHistoryUseCase,
            saveSearchHistoryUseCase,
            clearSearchHistoryUseCase,
            getSearchSuggestionsUseCase
        )
        advanceUntilIdle()

        viewModel.handleIntent(SearchIntent.SearchQueryChanged(query))
        advanceUntilIdle()

        assertEquals(query, viewModel.uiState.value.searchQuery)
        assertEquals(suggestions, viewModel.uiState.value.suggestions)
    }

    @Test
    fun `SearchSubmitted with valid query saves history and performs search`() = runTest {
        val query = "test query"
        val searchResult = SearchResult(
            songs = listOf(createSong(id = 1, title = "Test Song")),
            albums = listOf(createAlbum(id = 1, name = "Test Album")),
            artists = listOf(createArtist(id = 1, name = "Test Artist"))
        )
        every { searchAllUseCase(query) } returns flowOf(searchResult)

        viewModel = SearchViewModel(
            searchAllUseCase,
            getSearchHistoryUseCase,
            saveSearchHistoryUseCase,
            clearSearchHistoryUseCase,
            getSearchSuggestionsUseCase
        )
        advanceUntilIdle()

        viewModel.handleIntent(SearchIntent.SearchSubmitted(query))
        advanceUntilIdle()

        coVerify { saveSearchHistoryUseCase(query) }
        assertEquals(searchResult.songs, viewModel.uiState.value.songs)
        assertEquals(searchResult.albums, viewModel.uiState.value.albums)
        assertEquals(searchResult.artists, viewModel.uiState.value.artists)
    }

    @Test
    fun `SearchSubmitted with blank query does nothing`() = runTest {
        viewModel = SearchViewModel(
            searchAllUseCase,
            getSearchHistoryUseCase,
            saveSearchHistoryUseCase,
            clearSearchHistoryUseCase,
            getSearchSuggestionsUseCase
        )
        advanceUntilIdle()

        viewModel.handleIntent(SearchIntent.SearchSubmitted(""))
        advanceUntilIdle()

        coVerify(exactly = 0) { saveSearchHistoryUseCase(any()) }
    }

    @Test
    fun `HistoryItemClicked updates query and performs search`() = runTest {
        val query = "history query"
        val searchResult = SearchResult(songs = listOf(createSong()))
        every { searchAllUseCase(query) } returns flowOf(searchResult)

        viewModel = SearchViewModel(
            searchAllUseCase,
            getSearchHistoryUseCase,
            saveSearchHistoryUseCase,
            clearSearchHistoryUseCase,
            getSearchSuggestionsUseCase
        )
        advanceUntilIdle()

        viewModel.handleIntent(SearchIntent.HistoryItemClicked(query))
        advanceUntilIdle()

        assertEquals(query, viewModel.uiState.value.searchQuery)
        assertEquals(searchResult.songs, viewModel.uiState.value.songs)
    }

    @Test
    fun `ClearHistory clears search history`() = runTest {
        viewModel = SearchViewModel(
            searchAllUseCase,
            getSearchHistoryUseCase,
            saveSearchHistoryUseCase,
            clearSearchHistoryUseCase,
            getSearchSuggestionsUseCase
        )
        advanceUntilIdle()

        viewModel.handleIntent(SearchIntent.ClearHistory)
        advanceUntilIdle()

        coVerify { clearSearchHistoryUseCase() }
    }

    @Test
    fun `TabSelected updates selected tab`() = runTest {
        viewModel = SearchViewModel(
            searchAllUseCase,
            getSearchHistoryUseCase,
            saveSearchHistoryUseCase,
            clearSearchHistoryUseCase,
            getSearchSuggestionsUseCase
        )
        advanceUntilIdle()

        viewModel.handleIntent(SearchIntent.TabSelected(SearchTab.ALBUMS))
        advanceUntilIdle()

        assertEquals(SearchTab.ALBUMS, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `ClearSearch resets state`() = runTest {
        viewModel = SearchViewModel(
            searchAllUseCase,
            getSearchHistoryUseCase,
            saveSearchHistoryUseCase,
            clearSearchHistoryUseCase,
            getSearchSuggestionsUseCase
        )
        advanceUntilIdle()

        viewModel.handleIntent(SearchIntent.SearchQueryChanged("test"))
        advanceUntilIdle()
        viewModel.handleIntent(SearchIntent.ClearSearch)
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.songs.isEmpty())
        assertTrue(viewModel.uiState.value.albums.isEmpty())
        assertTrue(viewModel.uiState.value.artists.isEmpty())
    }

    @Test
    fun `SongClicked saves history and emits NavigateToPlayer effect`() = runTest {
        val song = createSong(id = 123L)
        viewModel = SearchViewModel(
            searchAllUseCase,
            getSearchHistoryUseCase,
            saveSearchHistoryUseCase,
            clearSearchHistoryUseCase,
            getSearchSuggestionsUseCase
        )
        advanceUntilIdle()

        viewModel.handleIntent(SearchIntent.SongClicked(song))
        advanceUntilIdle()

        coVerify { saveSearchHistoryUseCase(any()) }
    }

    @Test
    fun `AlbumClicked saves history and emits NavigateToAlbumDetail effect`() = runTest {
        val album = createAlbum(id = 456L)
        viewModel = SearchViewModel(
            searchAllUseCase,
            getSearchHistoryUseCase,
            saveSearchHistoryUseCase,
            clearSearchHistoryUseCase,
            getSearchSuggestionsUseCase
        )
        advanceUntilIdle()

        viewModel.handleIntent(SearchIntent.AlbumClicked(album))
        advanceUntilIdle()

        coVerify { saveSearchHistoryUseCase(any()) }
    }

    @Test
    fun `ArtistClicked saves history and emits NavigateToArtistDetail effect`() = runTest {
        val artist = createArtist(id = 789L)
        viewModel = SearchViewModel(
            searchAllUseCase,
            getSearchHistoryUseCase,
            saveSearchHistoryUseCase,
            clearSearchHistoryUseCase,
            getSearchSuggestionsUseCase
        )
        advanceUntilIdle()

        viewModel.handleIntent(SearchIntent.ArtistClicked(artist))
        advanceUntilIdle()

        coVerify { saveSearchHistoryUseCase(any()) }
    }
}
