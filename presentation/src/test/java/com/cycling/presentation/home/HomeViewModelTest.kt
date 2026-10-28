package com.cycling.presentation.home

import com.cycling.domain.model.PlayerState
import com.cycling.domain.model.Song
import com.cycling.domain.repository.PlayerRepository
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
        playerRepository
    )

    @Before
    fun setup() {
        songQueryUseCase = mockk()
        playerRepository = mockk()

        every { playerRepository.playerState } returns flowOf(PlayerState())
        every { songQueryUseCase.all() } returns flowOf(emptyList())
    }

    @Test
    fun `delivers every song as soon as the source emits`() = runTest {
        // 首页只有一份数据源：来了就画，不等任何其它查询。
        every { songQueryUseCase.all() } returns flowOf(listOf(createSong(1L), createSong(2L)))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.songs.size)
    }

    @Test
    fun `stays in the loading state while the source has not delivered`() = runTest {
        // 还没交付时不能显示空态：那会是"暂无歌曲"闪一下再变列表。
        every { songQueryUseCase.all() } returns MutableSharedFlow<List<Song>>()

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `an empty library stops loading`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.songs.isEmpty())
    }

    @Test
    fun `a failing source stops loading and reports it once`() = runTest {
        every { songQueryUseCase.all() } returns flow<List<Song>> {
            throw IllegalStateException("db is gone")
        }

        val viewModel = createViewModel()
        val effects = mutableListOf<HomeEffect>()
        val collectJob = launch { viewModel.uiEffect.collect { effects += it } }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, effects.count { it is HomeEffect.ShowToast })
        collectJob.cancel()
    }
}
