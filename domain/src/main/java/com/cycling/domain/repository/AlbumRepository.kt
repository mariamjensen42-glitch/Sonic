package com.cycling.domain.repository

import com.cycling.domain.model.Album
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {
    fun getAllAlbums(): Flow<List<Album>>

    suspend fun getAlbumById(id: Long): Album?

    fun getAlbumsByArtist(artistId: Long): Flow<List<Album>>

    suspend fun getAlbumCount(): Int

    fun searchAlbums(query: String): Flow<List<Album>>
}
