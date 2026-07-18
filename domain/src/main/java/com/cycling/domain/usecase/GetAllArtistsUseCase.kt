package com.cycling.domain.usecase

import com.cycling.domain.model.Artist
import com.cycling.domain.repository.ArtistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllArtistsUseCase @Inject constructor(
    private val repository: ArtistRepository
) {
    operator fun invoke(): Flow<List<Artist>> {
        return repository.getAllArtists()
    }
}
