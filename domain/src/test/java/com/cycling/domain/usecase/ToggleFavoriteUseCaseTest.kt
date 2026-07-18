package com.cycling.domain.usecase

import com.cycling.domain.repository.SongRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToggleFavoriteUseCaseTest {

    private lateinit var songRepository: SongRepository
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase

    @Before
    fun setup() {
        songRepository = mockk()
        toggleFavoriteUseCase = ToggleFavoriteUseCase(songRepository)
    }

    @Test
    fun `invoke toggles favorite to true`() = runTest {
        val songId = 1L
        coEvery { songRepository.toggleFavorite(songId) } returns true

        val result = toggleFavoriteUseCase(songId)

        assertTrue(result)
        coVerify { songRepository.toggleFavorite(songId) }
    }

    @Test
    fun `invoke toggles favorite to false`() = runTest {
        val songId = 2L
        coEvery { songRepository.toggleFavorite(songId) } returns false

        val result = toggleFavoriteUseCase(songId)

        assertFalse(result)
        coVerify { songRepository.toggleFavorite(songId) }
    }

    @Test
    fun `invoke calls repository with correct songId`() = runTest {
        val songId = 123L
        coEvery { songRepository.toggleFavorite(songId) } returns true

        toggleFavoriteUseCase(songId)

        coVerify(exactly = 1) { songRepository.toggleFavorite(songId) }
    }
}
