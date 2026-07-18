package com.cycling.domain.usecase

import com.cycling.domain.repository.LyricsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetLyricsUseCaseTest {

    private lateinit var lyricsRepository: LyricsRepository
    private lateinit var getLyricsUseCase: GetLyricsUseCase

    @Before
    fun setup() {
        lyricsRepository = mockk()
        getLyricsUseCase = GetLyricsUseCase(lyricsRepository)
    }

    @Test
    fun `invoke returns lyrics from repository`() = runTest {
        val songPath = "/storage/emulated/0/Music/test.mp3"
        val expectedLyrics = mockk<com.cycling.domain.model.Lyrics>()
        
        coEvery { lyricsRepository.getLyrics(songPath) } returns expectedLyrics

        val result = getLyricsUseCase(songPath)

        assertEquals(expectedLyrics, result)
        coVerify { lyricsRepository.getLyrics(songPath) }
    }

    @Test
    fun `invoke calls repository with correct path`() = runTest {
        val songPath = "/storage/emulated/0/Music/another_song.mp3"
        val expectedLyrics = mockk<com.cycling.domain.model.Lyrics>()
        
        coEvery { lyricsRepository.getLyrics(songPath) } returns expectedLyrics

        getLyricsUseCase(songPath)

        coVerify(exactly = 1) { lyricsRepository.getLyrics(songPath) }
    }
}
