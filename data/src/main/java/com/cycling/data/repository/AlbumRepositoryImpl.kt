package com.cycling.data.repository

import com.cycling.data.local.mediastore.MediaStoreHelper
import com.cycling.domain.model.Album
import com.cycling.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepositoryImpl @Inject constructor(
    private val mediaStoreHelper: MediaStoreHelper
) : AlbumRepository {

    private var cachedAlbums: List<Album> = emptyList()

    override fun getAllAlbums(): Flow<List<Album>> = flow {
        Timber.d("getAllAlbums: called")
        if (cachedAlbums.isEmpty()) {
            Timber.d("getAllAlbums: cache miss, querying MediaStore")
            cachedAlbums = mediaStoreHelper.queryAllAlbums()
        } else {
            Timber.d("getAllAlbums: cache hit")
        }
        Timber.d("getAllAlbums: found ${cachedAlbums.size} albums")
        emit(cachedAlbums)
    }

    override suspend fun getAlbumById(id: Long): Album? {
        Timber.d("getAlbumById: id=$id")
        if (cachedAlbums.isEmpty()) {
            Timber.d("getAlbumById: cache miss, querying MediaStore")
            cachedAlbums = mediaStoreHelper.queryAllAlbums()
        }
        val album = cachedAlbums.find { it.id == id }
        Timber.d("getAlbumById: id=$id, found=${album != null}")
        return album
    }

    override fun getAlbumsByArtist(artistId: Long): Flow<List<Album>> = flow {
        Timber.d("getAlbumsByArtist: artistId=$artistId")
        if (cachedAlbums.isEmpty()) {
            Timber.d("getAlbumsByArtist: cache miss, querying MediaStore")
            cachedAlbums = mediaStoreHelper.queryAllAlbums()
        }
        val songs = mediaStoreHelper.querySongsByArtist(artistId)
        val albumIds = songs.map { it.albumId }.distinct()
        val albums = cachedAlbums.filter { it.id in albumIds }
        Timber.d("getAlbumsByArtist: found ${albums.size} albums for artistId=$artistId")
        emit(albums)
    }

    override suspend fun getAlbumCount(): Int {
        if (cachedAlbums.isEmpty()) {
            Timber.d("getAlbumCount: cache miss, querying MediaStore")
            cachedAlbums = mediaStoreHelper.queryAllAlbums()
        }
        Timber.d("getAlbumCount: count=${cachedAlbums.size}")
        return cachedAlbums.size
    }

    override fun searchAlbums(query: String): Flow<List<Album>> = flow {
        Timber.d("searchAlbums: query=$query")
        if (cachedAlbums.isEmpty()) {
            Timber.d("searchAlbums: cache miss, querying MediaStore")
            cachedAlbums = mediaStoreHelper.queryAllAlbums()
        }
        val lowerCaseQuery = query.lowercase()
        val results = cachedAlbums.filter { album ->
            album.name.lowercase().contains(lowerCaseQuery) ||
            album.artist.lowercase().contains(lowerCaseQuery)
        }
        Timber.d("searchAlbums: found ${results.size} results for query=$query")
        emit(results)
    }
}
