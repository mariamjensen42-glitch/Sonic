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

    /**
     * 返回**真正写进去的行数**：一首歌在一张歌单里只有一行，重复添加是幂等的，
     * 已经在歌单里时返回 0。界面报数量时报这个值，不要报"本来想加几首"。
     */
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long): Int

    /** 批量添加同样按 songId 去重，返回真正写进去的行数（跳过已在单里的与本批重复的 id）。 */
    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>): Int

    /** 把歌单里第 [fromIndex] 首移到 [toIndex]（0-based，按当前显示顺序）。 */
    suspend fun moveSong(playlistId: Long, fromIndex: Int, toIndex: Int)

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    /** 批量移出歌单。一次事务，避免逐首删除时列表中途重排。 */
    suspend fun removeSongsFromPlaylist(playlistId: Long, songIds: List<Long>)

    fun getPlaylistSongCount(playlistId: Long): Flow<Int>

    suspend fun renamePlaylist(playlistId: Long, newName: String)
}
