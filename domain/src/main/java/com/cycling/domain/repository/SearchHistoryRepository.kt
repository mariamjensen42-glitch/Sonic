package com.cycling.domain.repository

import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    fun getAllHistory(): Flow<List<String>>
    suspend fun saveSearchQuery(query: String)
    suspend fun clearHistory()
    fun getSuggestions(query: String): Flow<List<String>>
}
