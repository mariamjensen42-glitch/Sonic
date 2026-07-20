package com.cycling.data.repository

import com.cycling.data.local.mediastore.MediaStoreHelper
import com.cycling.domain.model.Album
import com.cycling.domain.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepositoryImpl @Inject constructor(
    private val mediaStoreHelper: MediaStoreHelper
) : AlbumRepository {

    private var cachedAlbums: List<Album> = emptyList()

    override fun getAllAlbums(): Flow<List<Album>> = flow {
        if (cachedAlbums.isEmpty()) {
            cachedAlbums = mediaStoreHelper.queryAllAlbums()
        }
        emit(cachedAlbums)
    }.flowOn(Dispatchers.IO)

    override suspend fun getAlbumById(id: Long): Album? = withContext(Dispatchers.IO) {
        if (cachedAlbums.isEmpty()) {
            cachedAlbums = mediaStoreHelper.queryAllAlbums()
        }
        cachedAlbums.find { it.id == id }
    }

    override fun getAlbumsByArtist(artistId: Long): Flow<List<Album>> = flow {
        if (cachedAlbums.isEmpty()) {
            cachedAlbums = mediaStoreHelper.queryAllAlbums()
        }
        val songs = mediaStoreHelper.querySongsByArtist(artistId)
        val albumIds = songs.map { it.albumId }.distinct()
        emit(cachedAlbums.filter { it.id in albumIds })
    }.flowOn(Dispatchers.IO)

    override suspend fun getAlbumCount(): Int = withContext(Dispatchers.IO) {
        if (cachedAlbums.isEmpty()) {
            cachedAlbums = mediaStoreHelper.queryAllAlbums()
        }
        cachedAlbums.size
    }

    override fun searchAlbums(query: String): Flow<List<Album>> = flow {
        if (cachedAlbums.isEmpty()) {
            cachedAlbums = mediaStoreHelper.queryAllAlbums()
        }
        val lowerCaseQuery = query.lowercase()
        emit(cachedAlbums.filter { album ->
            album.name.lowercase().contains(lowerCaseQuery) ||
            album.artist.lowercase().contains(lowerCaseQuery)
        })
    }.flowOn(Dispatchers.IO)
}
