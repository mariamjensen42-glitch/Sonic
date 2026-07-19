package com.cycling.domain.usecase

import com.cycling.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchHistoryUseCase @Inject constructor(
    private val repository: SearchHistoryRepository
) {
    fun getAll(): Flow<List<String>> = repository.getAllHistory()

    suspend fun save(query: String) {
        if (query.isNotBlank()) {
            repository.saveSearchQuery(query)
        }
    }

    suspend fun clear() = repository.clearHistory()
}
