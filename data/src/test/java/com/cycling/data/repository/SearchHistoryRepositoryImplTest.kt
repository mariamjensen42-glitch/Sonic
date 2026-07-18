package com.cycling.data.repository

import com.cycling.data.local.dao.SearchHistoryDao
import com.cycling.data.local.entity.SearchHistoryEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchHistoryRepositoryImplTest {

    private lateinit var searchHistoryDao: SearchHistoryDao
    private lateinit var searchHistoryRepository: SearchHistoryRepositoryImpl

    @Before
    fun setup() {
        searchHistoryDao = mockk()
        searchHistoryRepository = SearchHistoryRepositoryImpl(searchHistoryDao)
    }

    @Test
    fun `getAllHistory returns distinct queries`() = runTest {
        val entities = listOf(
            SearchHistoryEntity(id = 1, query = "test"),
            SearchHistoryEntity(id = 2, query = "test"),
            SearchHistoryEntity(id = 3, query = "song")
        )
        every { searchHistoryDao.getAllHistory() } returns flowOf(entities)

        val result = searchHistoryRepository.getAllHistory().first()

        assertEquals(2, result.size)
        assertTrue(result.contains("test"))
        assertTrue(result.contains("song"))
    }

    @Test
    fun `getAllHistory returns empty list when no history`() = runTest {
        every { searchHistoryDao.getAllHistory() } returns flowOf(emptyList())

        val result = searchHistoryRepository.getAllHistory().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `saveSearchQuery trims and saves query`() = runTest {
        val query = "  test query  "
        coEvery { searchHistoryDao.insertHistory(any()) } returns Unit

        searchHistoryRepository.saveSearchQuery(query)

        coVerify { searchHistoryDao.insertHistory(match { it.query == "test query" }) }
    }

    @Test
    fun `clearHistory calls dao clearAllHistory`() = runTest {
        coEvery { searchHistoryDao.clearAllHistory() } returns Unit

        searchHistoryRepository.clearHistory()

        coVerify { searchHistoryDao.clearAllHistory() }
    }

    @Test
    fun `getSuggestions returns matching queries`() = runTest {
        val query = "test"
        val suggestions = listOf("test song", "test artist")
        every { searchHistoryDao.getSuggestions(query) } returns flowOf(suggestions)

        val result = searchHistoryRepository.getSuggestions(query).first()

        assertEquals(suggestions, result)
    }
}
