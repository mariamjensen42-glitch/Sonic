package com.cycling.presentation.home

import com.cycling.domain.model.Album
import com.cycling.domain.model.Artist
import com.cycling.domain.model.PlayerState
import com.cycling.domain.model.Song
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.usecase.GetAllAlbumsUseCase
import com.cycling.domain.usecase.GetAllArtistsUseCase
import com.cycling.domain.usecase.SongQueryUseCase
import com.cycling.presentation.util.TestDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var songQueryUseCase: SongQueryUseCase
    private lateinit var getAllAlbumsUseCase: GetAllAlbumsUseCase
    private lateinit var getAllArtistsUseCase: GetAllArtistsUseCase
    private lateinit var playerRepository: PlayerRepository

    private fun createSong(id: Long = 1L): Song = Song(
        id = id,
        title = "Test Song $id",
        artist = "Test Artist",
        album = "Test Album",
        duration = 180_000L,
        path = "/storage/emulated/0/Music/test$id.mp3",
        albumId = 1L,
        artistId = 1L,
        dateAdded = 1_000L * id,
        dateModified = 1_000L * id,
        size = 5_000_000L,
        mimeType = "audio/mpeg"
    )

    private fun createViewModel(): HomeViewModel = HomeViewModel(
        songQueryUseCase,
        getAllAlbumsUseCase,
        getAllArtistsUseCase,
        playerRepository
    )

    @Before
    fun setup() {
        songQueryUseCase = mockk()
        getAllAlbumsUseCase = mockk()
        getAllArtistsUseCase = mockk()
        playerRepository = mockk()

        every { playerRepository.playerState } returns flowOf(PlayerState())
        every { songQueryUseCase.all() } returns flowOf(emptyList())
        every { songQueryUseCase.favorites() } returns flowOf(emptyList())
        every { songQueryUseCase.mostPlayed() } returns flowOf(emptyList())
        every { songQueryUseCase.recentlyPlayed() } returns flowOf(emptyList())
        every { getAllAlbumsUseCase() } returns flowOf(emptyList())
        every { getAllArtistsUseCase() } returns flowOf(emptyList())
    }

    @Test
    fun `leaves the loading state without waiting for the album and artist sources`() = runTest {
        // 这两个源在真机上是 MediaStore 的全量查询，也是首页里最慢的两个；它们一个都不发射，
        // 首页也必须先把 Room 里的内容画出来。旧实现是六源齐了才放行，这个测试会挂在那里。
        every { getAllAlbumsUseCase() } returns MutableSharedFlow<List<Album>>()
        every { getAllArtistsUseCase() } returns MutableSharedFlow<List<Artist>>()
        every { songQueryUseCase.all() } returns flowOf(listOf(createSong()))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.recentlyAdded.size)
    }

    @Test
    fun `stays in the loading state while no source has delivered`() = runTest {
        // 一个源都还没交付时不能显示空态：那会是"还没有歌曲"闪一下再变列表。
        every { songQueryUseCase.all() } returns MutableSharedFlow<List<Song>>()
        every { songQueryUseCase.favorites() } returns MutableSharedFlow<List<Song>>()
        every { songQueryUseCase.mostPlayed() } returns MutableSharedFlow<List<Song>>()
        every { songQueryUseCase.recentlyPlayed() } returns MutableSharedFlow<List<Song>>()
        every { getAllAlbumsUseCase() } returns MutableSharedFlow<List<Album>>()
        every { getAllArtistsUseCase() } returns MutableSharedFlow<List<Artist>>()

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `an empty library stops loading once every source has reported`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.recentlyAdded.isEmpty())
    }

    @Test
    fun `a failing source stops loading and reports it once`() = runTest {
        every { songQueryUseCase.all() } returns flow<List<Song>> { throw IllegalStateException("db is gone") }

        val viewModel = createViewModel()
        val effects = mutableListOf<HomeEffect>()
        val collectJob = launch { viewModel.uiEffect.collect { effects += it } }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, effects.count { it is HomeEffect.ShowToast })
        collectJob.cancel()
    }
}
