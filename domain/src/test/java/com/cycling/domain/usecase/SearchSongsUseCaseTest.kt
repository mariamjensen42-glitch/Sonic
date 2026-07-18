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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchSongsUseCaseTest {

    private lateinit var songRepository: SongRepository
    private lateinit var searchSongsUseCase: SearchSongsUseCase

    @Before
    fun setup() {
        songRepository = mockk()
        searchSongsUseCase = SearchSongsUseCase(songRepository)
    }

    @Test
    fun `invoke with query returns matching songs`() = runTest {
        val query = "test"
        val expectedSongs = SongFixture.createList(3)
        every { songRepository.searchSongs(query) } returns flowOf(expectedSongs)

        val result = searchSongsUseCase(query).toList()

        assertEquals(1, result.size)
        assertEquals(expectedSongs, result.first())
        verify { songRepository.searchSongs(query) }
    }

    @Test
    fun `invoke with empty query returns empty list`() = runTest {
        val query = ""
        every { songRepository.searchSongs(query) } returns flowOf(emptyList())

        val result = searchSongsUseCase(query).toList()

        assertEquals(1, result.size)
        assertTrue(result.first().isEmpty())
    }

    @Test
    fun `invoke calls repository with correct query`() = runTest {
        val query = "artist name"
        every { songRepository.searchSongs(query) } returns flowOf(emptyList())

        searchSongsUseCase(query).toList()

        verify(exactly = 1) { songRepository.searchSongs(query) }
    }
}
