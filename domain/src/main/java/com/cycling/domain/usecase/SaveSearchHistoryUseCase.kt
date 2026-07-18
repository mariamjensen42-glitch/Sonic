package com.cycling.domain.usecase

import com.cycling.domain.repository.SearchHistoryRepository
import javax.inject.Inject

class SaveSearchHistoryUseCase @Inject constructor(
    private val repository: SearchHistoryRepository
) {
    suspend operator fun invoke(query: String) {
        if (query.isNotBlank()) {
            repository.saveSearchQuery(query)
        }
    }
}
