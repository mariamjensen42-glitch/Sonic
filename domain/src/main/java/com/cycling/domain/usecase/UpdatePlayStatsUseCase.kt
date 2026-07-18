package com.cycling.domain.usecase

import com.cycling.domain.repository.SongRepository
import javax.inject.Inject

class UpdatePlayStatsUseCase @Inject constructor(
    private val repository: SongRepository
) {
    suspend operator fun invoke(songId: Long, incrementCount: Boolean = true) {
        if (incrementCount) {
            repository.incrementPlayCount(songId)
        } else {
            repository.updateLastPlayedAt(songId)
        }
    }
}
