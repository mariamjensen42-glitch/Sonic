package com.cycling.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cycling.data.local.entity.PlaylistEntity
import com.cycling.data.local.entity.PlaylistSongEntity
import com.cycling.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY dateModified DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    /**
     * 观察单张歌单。介绍生成后会回写 `playlists` 表，详情页据此自动刷新 ——
     * 用一次性查询的话，UI 会一直拿着写入前的快照。
     */
    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observePlaylistById(id: Long): Flow<PlaylistEntity?>

    @Transaction
    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC
    """)
    fun getSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, dateModified = :dateModified WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String, dateModified: Long)

    @Query("UPDATE playlists SET description = :description, dateModified = :dateModified WHERE id = :id")
    suspend fun updatePlaylistDescription(id: Long, description: String?, dateModified: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(playlistSong: PlaylistSongEntity)

    @Transaction
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val maxPosition = getMaxPosition(playlistId) ?: -1
        insertPlaylistSong(PlaylistSongEntity(playlistId = playlistId, songId = songId, position = maxPosition + 1))
    }

    @Transaction
    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        val maxPosition = getMaxPosition(playlistId) ?: -1
        songIds.forEachIndexed { index, songId ->
            insertPlaylistSong(PlaylistSongEntity(playlistId = playlistId, songId = songId, position = maxPosition + 1 + index))
        }
    }

    @Query("SELECT MAX(position) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int?

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistSongIds(playlistId: Long): List<Long>

    @Query("UPDATE playlist_songs SET position = :position WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updateSongPosition(playlistId: Long, songId: Long, position: Int)

    /**
     * 把 [fromIndex] 的曲目移动到 [toIndex]，并重写整张歌单的 position。
     *
     * 一次事务内先读出完整顺序再逐行回写：拖拽是"整段位移"而不是交换两行，中间任何一行
     * 失败都会让顺序错位，所以不能拆成多次独立写。position 没有唯一约束，逐行更新不会
     * 撞上临时重复值。
     */
    @Transaction
    suspend fun moveSong(playlistId: Long, fromIndex: Int, toIndex: Int) {
        val ids = getPlaylistSongIds(playlistId).toMutableList()
        if (fromIndex !in ids.indices || toIndex !in ids.indices || fromIndex == toIndex) return
        val moved = ids.removeAt(fromIndex)
        ids.add(toIndex, moved)
        ids.forEachIndexed { index, songId ->
            updateSongPosition(playlistId = playlistId, songId = songId, position = index)
        }
    }

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId IN (:songIds)")
    suspend fun removeSongsFromPlaylist(playlistId: Long, songIds: List<Long>)

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    fun getPlaylistSongCount(playlistId: Long): Flow<Int>

    @Query("""
        SELECT s.albumArt FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        LIMIT 1
    """)
    fun getPlaylistFirstSongAlbumArtFlow(playlistId: Long): Flow<String?>
}
