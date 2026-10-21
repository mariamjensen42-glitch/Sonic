package com.cycling.presentation.player

import com.cycling.domain.model.HiResBadge
import com.cycling.domain.model.PlayerState
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SettingsRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.usecase.GetLyricsUseCase
import com.cycling.presentation.util.TestDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.cycling.domain.repository.AudioReactiveRepository

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelOpenPlayerOnPlayTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    @Test
    fun `openPlayerOnPlay defaults to true and tracks the repository`() = runTest {
        val openPlayerOnPlay = MutableStateFlow(true)
        val viewModel = createViewModel(openPlayerOnPlay)
        advanceUntilIdle()
        assertTrue(viewModel.openPlayerOnPlay.value)

        openPlayerOnPlay.value = false
        advanceUntilIdle()
        assertFalse(viewModel.openPlayerOnPlay.value)
    }

    private fun createViewModel(openPlayerOnPlay: MutableStateFlow<Boolean>): PlayerViewModel {
        val playerRepository = mockk<PlayerRepository>()
        every { playerRepository.playerState } returns flowOf(PlayerState())
        every { playerRepository.events } returns emptyFlow()
        every { playerRepository.sleepTimerRemainingMs } returns flowOf(null)
        val settingsRepository = mockk<SettingsRepository>()
        every { settingsRepository.playbackSpeed } returns flowOf(1.0f)
        every { settingsRepository.sleepTimerPresetMinutes } returns flowOf(30)
        every { settingsRepository.playerSpectrumEnabled } returns flowOf(true)
        every { settingsRepository.hiResBadge } returns flowOf(HiResBadge.OFF)
        every { settingsRepository.hiResBadgeCustomUri } returns flowOf(null)
        every { settingsRepository.lyricsTextSizeSp } returns flowOf(28)
        every { settingsRepository.lyricsAlignCenter } returns flowOf(false)
        every { settingsRepository.lyricsGlowEnabled } returns flowOf(false)
        every { settingsRepository.lyricsBlurEnabled } returns flowOf(true)
        every { settingsRepository.openPlayerOnPlay } returns openPlayerOnPlay
        return PlayerViewModel(
            playerRepository = playerRepository,
            songRepository = mockk(relaxed = true),
            settingsRepository = settingsRepository,
            getLyricsUseCase = mockk(relaxed = true),
            audioReactiveRepository = mockk { every { beat } returns emptyFlow() }
        )
    }
}
