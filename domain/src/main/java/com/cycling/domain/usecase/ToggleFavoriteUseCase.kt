package com.cycling.domain.usecase

import com.cycling.domain.repository.SongRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: SongRepository
) {
    suspend operator fun invoke(songId: Long): Boolean {
        return repository.toggleFavorite(songId)
    }
}
