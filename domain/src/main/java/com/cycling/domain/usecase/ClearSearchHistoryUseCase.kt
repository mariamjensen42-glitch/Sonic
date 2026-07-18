package com.cycling.domain.usecase

import com.cycling.domain.repository.SearchHistoryRepository
import javax.inject.Inject

class ClearSearchHistoryUseCase @Inject constructor(
    private val repository: SearchHistoryRepository
) {
    suspend operator fun invoke() {
        repository.clearHistory()
    }
}
