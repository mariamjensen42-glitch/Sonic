package com.cycling.domain.repository

import com.cycling.domain.model.Playlist
import com.cycling.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>

    suspend fun getPlaylistById(id: Long): Playlist?

    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>>

    suspend fun createPlaylist(name: String): Long

    suspend fun deletePlaylist(playlistId: Long)

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)

    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>)

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    fun getPlaylistSongCount(playlistId: Long): Flow<Int>

    suspend fun renamePlaylist(playlistId: Long, newName: String)
}
