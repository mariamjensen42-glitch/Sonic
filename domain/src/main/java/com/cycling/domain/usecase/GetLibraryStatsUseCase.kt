package com.cycling.domain.usecase

import com.cycling.domain.model.LibraryStats
import com.cycling.domain.repository.SongRepository
import javax.inject.Inject

class GetLibraryStatsUseCase @Inject constructor(
    private val repository: SongRepository
) {
    suspend operator fun invoke(): LibraryStats {
        return repository.getLibraryStats()
    }
}
