package com.cycling.presentation.mymusic

import com.cycling.domain.model.PlayerState
import com.cycling.domain.model.Song
import com.cycling.domain.online.OnlinePlaylistRepository
import com.cycling.domain.repository.AiRepository
import com.cycling.domain.repository.PlaylistRepository
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.usecase.ImportOnlinePlaylistUseCase
import com.cycling.domain.usecase.SongQueryUseCase
import com.cycling.presentation.util.TestDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyMusicViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    @Test
    fun `favorite description is generated once and reused after reopening`() = runTest {
        val songs = listOf(createSong())
        val favoriteSongs = MutableStateFlow(songs)
        val songQueryUseCase = mockk<SongQueryUseCase>()
        val songRepository = mockk<SongRepository>(relaxed = true)
        val playerRepository = mockk<PlayerRepository>()
        val playlistRepository = mockk<PlaylistRepository>()
        val aiRepository = mockk<AiRepository>()
        val generatedDescription = "收藏歌曲的情绪脉络在此交汇。"
        val cachedDescription = "从本地缓存恢复的收藏介绍。"

        every { songQueryUseCase.favorites() } returns favoriteSongs
        every { playlistRepository.getAllPlaylists() } returns flowOf(emptyList())
        every { playerRepository.playerState } returns flowOf(PlayerState())
        coEvery { aiRepository.getCachedFavoritePlaylistDescription(songs) } returnsMany listOf(
            null,
            cachedDescription
        )
        coEvery {
            aiRepository.generatePlaylistDescription("我喜欢", "收藏的歌曲", songs)
        } returns Result.success(generatedDescription)
        coEvery { aiRepository.cacheFavoritePlaylistDescription(songs, generatedDescription) } returns Unit

        val viewModel = MyMusicViewModel(
            songQueryUseCase = songQueryUseCase,
            songRepository = songRepository,
            playerRepository = playerRepository,
            playlistRepository = playlistRepository,
            aiRepository = aiRepository,
            onlinePlaylistRepository = mockk<OnlinePlaylistRepository>(relaxed = true),
            importOnlinePlaylistUseCase = mockk<ImportOnlinePlaylistUseCase>(relaxed = true)
        )
        advanceUntilIdle()

        viewModel.handleIntent(MyMusicIntent.FavoriteSongsClick)
        advanceUntilIdle()
        assertEquals(generatedDescription, viewModel.uiState.value.favoriteDescription)

        viewModel.handleIntent(MyMusicIntent.BackToPlaylists)
        viewModel.handleIntent(MyMusicIntent.FavoriteSongsClick)
        advanceUntilIdle()

        assertEquals(cachedDescription, viewModel.uiState.value.favoriteDescription)
        coVerify(exactly = 1) {
            aiRepository.generatePlaylistDescription("我喜欢", "收藏的歌曲", songs)
        }
        coVerify(exactly = 1) {
            aiRepository.cacheFavoritePlaylistDescription(songs, generatedDescription)
        }
    }

    private fun createSong() = Song(
        id = 42L,
        title = "夜曲",
        artist = "周杰伦",
        album = "十一月的萧邦",
        duration = 180_000L,
        path = "/music/night-song.mp3",
        albumId = 1L,
        artistId = 1L,
        dateAdded = 0L,
        dateModified = 0L,
        size = 5_000_000L,
        mimeType = "audio/mpeg",
        isFavorite = true
    )
}
