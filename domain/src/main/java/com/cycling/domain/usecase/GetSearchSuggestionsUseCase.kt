package com.cycling.domain.usecase

import com.cycling.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSearchSuggestionsUseCase @Inject constructor(
    private val repository: SearchHistoryRepository
) {
    operator fun invoke(query: String): Flow<List<String>> {
        return repository.getSuggestions(query)
    }
}
