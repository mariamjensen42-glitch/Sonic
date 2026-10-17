package com.cycling.domain.repository

import com.cycling.domain.model.Playlist
import com.cycling.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>

    suspend fun getPlaylistById(id: Long): Playlist?

    /** 观察单张歌单；字段被外部改写（如生成介绍）时会重新发射。 */
    fun observePlaylistById(id: Long): Flow<Playlist?>

    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>>

    suspend fun createPlaylist(name: String): Long

    /** 建歌单并同时写入介绍，供 AI 创建流程使用。 */
    suspend fun createPlaylist(name: String, description: String?): Long

    /** 回填介绍；详情页在缺描述时可单独补写。 */
    suspend fun updatePlaylistDescription(playlistId: Long, description: String?)

    suspend fun deletePlaylist(playlistId: Long)

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)

    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>)

    /** 把歌单里第 [fromIndex] 首移到 [toIndex]（0-based，按当前显示顺序）。 */
    suspend fun moveSong(playlistId: Long, fromIndex: Int, toIndex: Int)

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    /** 批量移出歌单。一次事务，避免逐首删除时列表中途重排。 */
    suspend fun removeSongsFromPlaylist(playlistId: Long, songIds: List<Long>)

    fun getPlaylistSongCount(playlistId: Long): Flow<Int>

    suspend fun renamePlaylist(playlistId: Long, newName: String)
}
