package com.cycling.domain.repository

import com.cycling.domain.model.FolderContent
import com.cycling.domain.model.LibraryStats
import com.cycling.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getAllSongs(): Flow<List<Song>>

    suspend fun getSongById(id: Long): Song?

    fun getSongsByAlbum(albumId: Long): Flow<List<Song>>

    fun getSongsByArtist(artistId: Long): Flow<List<Song>>

    fun searchSongs(query: String): Flow<List<Song>>

    suspend fun refreshSongs()

    suspend fun getSongCount(): Int

    fun getFavoriteSongs(): Flow<List<Song>>

    fun getMostPlayedSongs(): Flow<List<Song>>

    fun getRecentlyPlayedSongs(): Flow<List<Song>>

    suspend fun toggleFavorite(songId: Long): Boolean

    suspend fun incrementPlayCount(songId: Long)

    suspend fun updateLastPlayedAt(songId: Long)

    suspend fun getLibraryStats(): LibraryStats

    suspend fun updateSongInfo(songId: Long, title: String?, artist: String?, album: String?): Boolean

    suspend fun getFolderContent(path: String): FolderContent
}
