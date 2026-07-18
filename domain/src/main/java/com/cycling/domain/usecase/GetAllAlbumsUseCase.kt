package com.cycling.domain.usecase

import com.cycling.domain.model.Album
import com.cycling.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllAlbumsUseCase @Inject constructor(
    private val repository: AlbumRepository
) {
    operator fun invoke(): Flow<List<Album>> {
        return repository.getAllAlbums()
    }
}
