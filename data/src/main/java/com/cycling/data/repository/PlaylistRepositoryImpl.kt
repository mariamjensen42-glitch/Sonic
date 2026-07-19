package com.cycling.data.repository

import com.cycling.data.local.dao.PlaylistDao
import com.cycling.data.local.entity.PlaylistEntity
import com.cycling.data.mapper.toDomain
import com.cycling.domain.model.Playlist
import com.cycling.domain.model.Song
import com.cycling.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> {
        Timber.d("getAllPlaylists: called")
        return playlistDao.getAllPlaylists().flatMapLatest { entities ->
            if (entities.isEmpty()) {
                flowOf(emptyList())
            } else {
                Timber.d("getAllPlaylists: found ${entities.size} playlists")
                val playlistFlows = entities.map { entity ->
                    combine(
                        playlistDao.getPlaylistSongCount(entity.id),
                        playlistDao.getPlaylistFirstSongAlbumArtFlow(entity.id)
                    ) { count, albumArt ->
                        entity.toDomain().copy(numberOfSongs = count, albumArt = albumArt)
                    }
                }
                combine(playlistFlows) { it.toList() }
            }
        }
    }

    override suspend fun getPlaylistById(id: Long): Playlist? {
        val playlist = playlistDao.getPlaylistById(id)?.toDomain()
        Timber.d("getPlaylistById: id=$id, found=${playlist != null}")
        return playlist
    }

    override fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>> {
        Timber.d("getSongsInPlaylist: playlistId=$playlistId")
        return playlistDao.getSongsInPlaylist(playlistId).map { entities ->
            Timber.d("getSongsInPlaylist: found ${entities.size} songs in playlistId=$playlistId")
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createPlaylist(name: String): Long {
        val playlist = PlaylistEntity(
            name = name,
            dateAdded = System.currentTimeMillis() / 1000,
            dateModified = System.currentTimeMillis() / 1000
        )
        return playlistDao.insertPlaylist(playlist)
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.getPlaylistById(playlistId)?.let { playlist ->
            playlistDao.deletePlaylist(playlist)
        }
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        playlistDao.addSongToPlaylist(playlistId, songId)
    }

    override suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        playlistDao.addSongsToPlaylist(playlistId, songIds)
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    override fun getPlaylistSongCount(playlistId: Long): Flow<Int> {
        return playlistDao.getPlaylistSongCount(playlistId)
    }

    override suspend fun renamePlaylist(playlistId: Long, newName: String) {
        Timber.d("renamePlaylist: playlistId=$playlistId, newName=$newName")
        playlistDao.renamePlaylist(
            id = playlistId,
            name = newName,
            dateModified = System.currentTimeMillis() / 1000
        )
    }
}
