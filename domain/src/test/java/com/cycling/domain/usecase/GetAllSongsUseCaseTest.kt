package com.cycling.domain.usecase

import com.cycling.domain.model.SongFixture
import com.cycling.domain.repository.SongRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetAllSongsUseCaseTest {

    private lateinit var songRepository: SongRepository
    private lateinit var getAllSongsUseCase: GetAllSongsUseCase

    @Before
    fun setup() {
        songRepository = mockk()
        getAllSongsUseCase = GetAllSongsUseCase(songRepository)
    }

    @Test
    fun `invoke returns all songs from repository`() = runTest {
        val expectedSongs = SongFixture.createList(5)
        every { songRepository.getAllSongs() } returns flowOf(expectedSongs)

        val result = getAllSongsUseCase().toList()

        assertEquals(1, result.size)
        assertEquals(expectedSongs, result.first())
        verify { songRepository.getAllSongs() }
    }

    @Test
    fun `invoke returns empty list when no songs`() = runTest {
        every { songRepository.getAllSongs() } returns flowOf(emptyList())

        val result = getAllSongsUseCase().toList()

        assertEquals(1, result.size)
        assertEquals(emptyList<com.cycling.domain.model.Song>(), result.first())
    }
}
