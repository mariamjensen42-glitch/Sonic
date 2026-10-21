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

    /**
     * IGNORE 而不是 REPLACE：撞上 `(playlistId, songId)` 唯一索引时，REPLACE 是先删旧行
     * 再插新行 —— 等于把这首歌挪到队尾；IGNORE 则保持它原来的位置不动。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSong(playlistSong: PlaylistSongEntity)

    /**
     * 单首添加，返回真正写进去的行数（0 = 这首歌本来就在单里）。
     *
     * `(playlistId, songId)` 上有唯一索引，一首歌在一张歌单里只占一行（歌单页用
     * `song.id` 当 LazyColumn 的 key，重复行会闪退）。调用方拿返回值报真实数量，
     * 而不是拿"想加几首"当结果。
     */
    @Transaction
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long): Int {
        if (songId in getPlaylistSongIds(playlistId)) return 0
        val position = (getMaxPosition(playlistId) ?: -1) + 1
        insertPlaylistSong(PlaylistSongEntity(playlistId = playlistId, songId = songId, position = position))
        return 1
    }

    /**
     * 批量添加同一套规则，返回真正写进去的行数：跳过歌单里已有的，也跳过本批次内重复的 id。
     *
     * 先查一遍、而不是只靠唯一索引去挡，是为了让 position 保持连续：否则"添加 10 首、
     * 其中 3 首已在单里"会让新歌的 position 跳掉一段空号。
     */
    @Transaction
    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>): Int {
        if (songIds.isEmpty()) return 0
        val existing = getPlaylistSongIds(playlistId).toMutableSet()
        var position = (getMaxPosition(playlistId) ?: -1) + 1
        var added = 0
        songIds.forEach { songId ->
            if (songId in existing) return@forEach
            existing += songId
            insertPlaylistSong(
                PlaylistSongEntity(playlistId = playlistId, songId = songId, position = position)
            )
            position++
            added++
        }
        return added
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
