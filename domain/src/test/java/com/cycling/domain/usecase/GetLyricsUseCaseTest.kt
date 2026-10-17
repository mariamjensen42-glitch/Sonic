package com.cycling.domain.usecase

import com.cycling.domain.model.Lyrics
import com.cycling.domain.model.SongFixture
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
        val song = SongFixture.create()
        val expectedLyrics = mockk<Lyrics>()

        coEvery { lyricsRepository.getLyrics(song) } returns expectedLyrics

        val result = getLyricsUseCase(song)

        assertEquals(expectedLyrics, result)
        coVerify { lyricsRepository.getLyrics(song) }
    }

    @Test
    fun `invoke passes the whole song so online providers can match on metadata`() = runTest {
        val song = SongFixture.create(title = "Another Song", artist = "Someone Else")
        coEvery { lyricsRepository.getLyrics(song) } returns mockk()

        getLyricsUseCase(song)

        coVerify(exactly = 1) { lyricsRepository.getLyrics(song) }
    }
}
