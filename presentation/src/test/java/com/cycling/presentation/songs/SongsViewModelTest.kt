package com.cycling.presentation.songs

import com.cycling.domain.model.Playlist
import com.cycling.domain.model.Song
import com.cycling.domain.model.SortOrder
import com.cycling.domain.model.ViewMode
import com.cycling.domain.repository.PlaylistRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.repository.SongsPreferencesRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SongsViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var songRepository: SongRepository
    private lateinit var songsPreferencesRepository: SongsPreferencesRepository
    private lateinit var playlistRepository: PlaylistRepository
    private lateinit var viewModel: SongsViewModel

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

    private fun createPlaylist(
        id: Long = 1L,
        name: String = "Test Playlist",
        numberOfSongs: Int = 0
    ): Playlist = Playlist(
        id = id,
        name = name,
        dateAdded = System.currentTimeMillis(),
        dateModified = System.currentTimeMillis(),
        numberOfSongs = numberOfSongs
    )

    @Before
    fun setup() {
        songRepository = mockk()
        songsPreferencesRepository = mockk()
        playlistRepository = mockk()

        every { songRepository.getAllSongs() } returns flowOf(emptyList())
        every { playlistRepository.getAllPlaylists() } returns flowOf(emptyList())
        coEvery { songsPreferencesRepository.getViewMode() } returns ViewMode.LIST
        coEvery { songsPreferencesRepository.getSortOrder() } returns SortOrder.TITLE
        coEvery { songsPreferencesRepository.getSortAscending() } returns true
    }

    @Test
    fun `init loads songs and preferences`() = runTest {
        val songs = listOf(createSong(id = 1), createSong(id = 2))
        every { songRepository.getAllSongs() } returns flowOf(songs)

        viewModel = SongsViewModel(songRepository, songsPreferencesRepository, playlistRepository)
        advanceUntilIdle()

        assertEquals(songs, viewModel.uiState.value.songs)
        assertEquals(ViewMode.LIST, viewModel.uiState.value.viewMode)
        assertEquals(SortOrder.TITLE, viewModel.uiState.value.sortOrder)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `ToggleViewMode changes view mode from LIST to GRID`() = runTest {
        coEvery { songsPreferencesRepository.saveViewMode(any()) } returns Unit
        viewModel = SongsViewModel(songRepository, songsPreferencesRepository, playlistRepository)
        advanceUntilIdle()

        viewModel.handleIntent(SongsIntent.ToggleViewMode)
        advanceUntilIdle()

        assertEquals(ViewMode.GRID, viewModel.uiState.value.viewMode)
        coVerify { songsPreferencesRepository.saveViewMode(ViewMode.GRID) }
    }

    @Test
    fun `ToggleViewMode changes view mode from GRID to LIST`() = runTest {
        coEvery { songsPreferencesRepository.getViewMode() } returns ViewMode.GRID
        coEvery { songsPreferencesRepository.saveViewMode(any()) } returns Unit
        viewModel = SongsViewModel(songRepository, songsPreferencesRepository, playlistRepository)
        advanceUntilIdle()

        viewModel.handleIntent(SongsIntent.ToggleViewMode)
        advanceUntilIdle()

        assertEquals(ViewMode.LIST, viewModel.uiState.value.viewMode)
        coVerify { songsPreferencesRepository.saveViewMode(ViewMode.LIST) }
    }

    @Test
    fun `ChangeSortOrder with same order toggles ascending`() = runTest {
        coEvery { songsPreferencesRepository.saveSortOrder(any()) } returns Unit
        coEvery { songsPreferencesRepository.saveSortAscending(any()) } returns Unit
        viewModel = SongsViewModel(songRepository, songsPreferencesRepository, playlistRepository)
        advanceUntilIdle()

        viewModel.handleIntent(SongsIntent.ChangeSortOrder(SortOrder.TITLE))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.sortAscending)
        coVerify { songsPreferencesRepository.saveSortAscending(false) }
    }

    @Test
    fun `ChangeSortOrder with different order sets ascending to true`() = runTest {
        coEvery { songsPreferencesRepository.saveSortOrder(any()) } returns Unit
        coEvery { songsPreferencesRepository.saveSortAscending(any()) } returns Unit
        viewModel = SongsViewModel(songRepository, songsPreferencesRepository, playlistRepository)
        advanceUntilIdle()

        viewModel.handleIntent(SongsIntent.ChangeSortOrder(SortOrder.ARTIST))
        advanceUntilIdle()

        assertEquals(SortOrder.ARTIST, viewModel.uiState.value.sortOrder)
        assertTrue(viewModel.uiState.value.sortAscending)
    }

    @Test
    fun `SongClick emits NavigateToPlayer effect`() = runTest {
        val song = createSong(id = 123L)
        viewModel = SongsViewModel(songRepository, songsPreferencesRepository, playlistRepository)
        advanceUntilIdle()

        viewModel.handleIntent(SongsIntent.SongClick(song))
        advanceUntilIdle()

        viewModel.uiEffect.collect { effect ->
            assertEquals(SongsEffect.NavigateToPlayer(123L), effect)
        }
    }

    @Test
    fun `ShowAddToPlaylistDialog updates state with song`() = runTest {
        val song = createSong(id = 1L)
        viewModel = SongsViewModel(songRepository, songsPreferencesRepository, playlistRepository)
        advanceUntilIdle()

        viewModel.handleIntent(SongsIntent.ShowAddToPlaylistDialog(song))
        advanceUntilIdle()

        assertEquals(song, viewModel.uiState.value.songToAdd)
        assertTrue(viewModel.uiState.value.showAddToPlaylistDialog)
    }

    @Test
    fun `DismissAddToPlaylistDialog clears state`() = runTest {
        val song = createSong(id = 1L)
        viewModel = SongsViewModel(songRepository, songsPreferencesRepository, playlistRepository)
        advanceUntilIdle()

        viewModel.handleIntent(SongsIntent.ShowAddToPlaylistDialog(song))
        advanceUntilIdle()
        viewModel.handleIntent(SongsIntent.DismissAddToPlaylistDialog)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.songToAdd == null)
        assertFalse(viewModel.uiState.value.showAddToPlaylistDialog)
    }

    @Test
    fun `AddToPlaylist adds song and dismisses dialog`() = runTest {
        val song = createSong(id = 1L)
        val playlistId = 10L
        coEvery { playlistRepository.addSongToPlaylist(playlistId, song.id) } returns Unit
        viewModel = SongsViewModel(songRepository, songsPreferencesRepository, playlistRepository)
        advanceUntilIdle()

        viewModel.handleIntent(SongsIntent.ShowAddToPlaylistDialog(song))
        advanceUntilIdle()
        viewModel.handleIntent(SongsIntent.AddToPlaylist(playlistId))
        advanceUntilIdle()

        coVerify { playlistRepository.addSongToPlaylist(playlistId, song.id) }
        assertFalse(viewModel.uiState.value.showAddToPlaylistDialog)
    }

    @Test
    fun `CreatePlaylistAndAddSong creates playlist and adds song`() = runTest {
        val song = createSong(id = 1L)
        val playlistName = "New Playlist"
        val playlistId = 100L
        coEvery { playlistRepository.createPlaylist(playlistName) } returns playlistId
        coEvery { playlistRepository.addSongToPlaylist(playlistId, song.id) } returns Unit
        viewModel = SongsViewModel(songRepository, songsPreferencesRepository, playlistRepository)
        advanceUntilIdle()

        viewModel.handleIntent(SongsIntent.ShowAddToPlaylistDialog(song))
        advanceUntilIdle()
        viewModel.handleIntent(SongsIntent.CreatePlaylistAndAddSong(playlistName))
        advanceUntilIdle()

        coVerify { playlistRepository.createPlaylist(playlistName) }
        coVerify { playlistRepository.addSongToPlaylist(playlistId, song.id) }
        assertFalse(viewModel.uiState.value.showAddToPlaylistDialog)
    }

    @Test
    fun `loadPlaylists updates state with playlists`() = runTest {
        val playlists = listOf(createPlaylist(id = 1), createPlaylist(id = 2))
        every { playlistRepository.getAllPlaylists() } returns flowOf(playlists)
        viewModel = SongsViewModel(songRepository, songsPreferencesRepository, playlistRepository)
        advanceUntilIdle()

        assertEquals(playlists, viewModel.uiState.value.playlists)
    }
}
