package com.cycling.data.repository

import com.cycling.data.local.mediastore.MediaStoreHelper
import com.cycling.domain.model.Artist
import com.cycling.domain.repository.ArtistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistRepositoryImpl @Inject constructor(
    private val mediaStoreHelper: MediaStoreHelper
) : ArtistRepository {

    private var cachedArtists: List<Artist> = emptyList()

    override fun getAllArtists(): Flow<List<Artist>> = flow {
        Timber.d("getAllArtists: called")
        if (cachedArtists.isEmpty()) {
            Timber.d("getAllArtists: cache miss, querying MediaStore")
            cachedArtists = mediaStoreHelper.queryAllArtists()
        } else {
            Timber.d("getAllArtists: cache hit")
        }
        Timber.d("getAllArtists: found ${cachedArtists.size} artists")
        emit(cachedArtists)
    }

    override suspend fun getArtistById(id: Long): Artist? {
        Timber.d("getArtistById: id=$id")
        if (cachedArtists.isEmpty()) {
            Timber.d("getArtistById: cache miss, querying MediaStore")
            cachedArtists = mediaStoreHelper.queryAllArtists()
        }
        val artist = cachedArtists.find { it.id == id }
        Timber.d("getArtistById: id=$id, found=${artist != null}")
        return artist
    }

    override suspend fun getArtistCount(): Int {
        if (cachedArtists.isEmpty()) {
            cachedArtists = mediaStoreHelper.queryAllArtists()
        }
        return cachedArtists.size
    }

    override fun searchArtists(query: String): Flow<List<Artist>> = flow {
        Timber.d("searchArtists: query=$query")
        if (cachedArtists.isEmpty()) {
            Timber.d("searchArtists: cache miss, querying MediaStore")
            cachedArtists = mediaStoreHelper.queryAllArtists()
        }
        val lowerCaseQuery = query.lowercase()
        val results = cachedArtists.filter { artist ->
            artist.name.lowercase().contains(lowerCaseQuery)
        }
        Timber.d("searchArtists: found ${results.size} results for query=$query")
        emit(results)
    }
}
