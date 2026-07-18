package com.cycling.data.repository

import com.cycling.data.local.dao.SearchHistoryDao
import com.cycling.data.local.entity.SearchHistoryEntity
import com.cycling.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryRepositoryImpl @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao
) : SearchHistoryRepository {

    override fun getAllHistory(): Flow<List<String>> {
        Timber.d("getAllHistory: called")
        return searchHistoryDao.getAllHistory().map { entities ->
            val history = entities.map { it.query }.distinct()
            Timber.d("getAllHistory: found ${history.size} unique queries")
            history
        }
    }

    override suspend fun saveSearchQuery(query: String) {
        Timber.d("saveSearchQuery: query=$query")
        searchHistoryDao.insertHistory(
            SearchHistoryEntity(query = query.trim())
        )
    }

    override suspend fun clearHistory() {
        Timber.d("clearHistory: called")
        searchHistoryDao.clearAllHistory()
    }

    override fun getSuggestions(query: String): Flow<List<String>> {
        Timber.d("getSuggestions: query=$query")
        return searchHistoryDao.getSuggestions(query)
    }
}
