package com.cycling.domain.usecase

import com.cycling.domain.model.Lyrics
import com.cycling.domain.model.LyricsSource
import com.cycling.domain.model.SongFixture
import com.cycling.domain.repository.LyricsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SaveLyricsUseCaseTest {

    private lateinit var lyricsRepository: LyricsRepository
    private lateinit var saveLyricsUseCase: SaveLyricsUseCase

    @Before
    fun setup() {
        lyricsRepository = mockk()
        saveLyricsUseCase = SaveLyricsUseCase(lyricsRepository)
    }

    @Test
    fun `typed lyrics are stored as the user's own`() = runTest {
        val song = SongFixture.create()
        val saved = Lyrics(syncedLyrics = null, rawContent = "my line", source = LyricsSource.USER)
        coEvery { lyricsRepository.saveLyrics(song, "my line") } returns saved

        val result = saveLyricsUseCase(song, "my line")

        assertEquals(saved, result)
        coVerify(exactly = 1) { lyricsRepository.saveLyrics(song, "my line") }
        coVerify(exactly = 0) { lyricsRepository.clearLyrics(any()) }
    }

    @Test
    fun `a blank editor clears the override instead of storing an empty one`() = runTest {
        // An empty override would suppress the online lookup for a track the user was only trying
        // to reset, so the row goes and the automatic sources answer again.
        val song = SongFixture.create()
        val automatic = Lyrics(syncedLyrics = null, rawContent = "[00:01.00]downloaded", source = LyricsSource.ONLINE)
        coEvery { lyricsRepository.clearLyrics(song) } returns Unit
        coEvery { lyricsRepository.getLyrics(song) } returns automatic

        val result = saveLyricsUseCase(song, "   ")

        assertEquals(automatic, result)
        coVerify(exactly = 1) { lyricsRepository.clearLyrics(song) }
        coVerify(exactly = 0) { lyricsRepository.saveLyrics(any(), any()) }
    }
}
