package com.cycling.data.repository

import com.cycling.data.local.mediastore.MediaStoreHelper
import com.cycling.domain.model.Artist
import com.cycling.domain.repository.ArtistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistRepositoryImpl @Inject constructor(
    private val mediaStoreHelper: MediaStoreHelper
) : ArtistRepository {

    private var cachedArtists: List<Artist> = emptyList()

    override fun getAllArtists(): Flow<List<Artist>> = flow {
        if (cachedArtists.isEmpty()) {
            cachedArtists = mediaStoreHelper.queryAllArtists()
        }
        emit(cachedArtists)
    }.flowOn(Dispatchers.IO)

    override suspend fun getArtistById(id: Long): Artist? = withContext(Dispatchers.IO) {
        if (cachedArtists.isEmpty()) {
            cachedArtists = mediaStoreHelper.queryAllArtists()
        }
        cachedArtists.find { it.id == id }
    }

    override suspend fun getArtistCount(): Int = withContext(Dispatchers.IO) {
        if (cachedArtists.isEmpty()) {
            cachedArtists = mediaStoreHelper.queryAllArtists()
        }
        cachedArtists.size
    }

    override fun searchArtists(query: String): Flow<List<Artist>> = flow {
        if (cachedArtists.isEmpty()) {
            cachedArtists = mediaStoreHelper.queryAllArtists()
        }
        val lowerCaseQuery = query.lowercase()
        emit(cachedArtists.filter { artist ->
            artist.name.lowercase().contains(lowerCaseQuery)
        })
    }.flowOn(Dispatchers.IO)
}
