package com.cycling.domain.repository

import com.cycling.domain.model.FolderContent
import com.cycling.domain.model.FolderRoot
import com.cycling.domain.model.LibraryStats
import com.cycling.domain.model.LyricsMatch
import com.cycling.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getAllSongs(): Flow<List<Song>>

    suspend fun getSongById(id: Long): Song?

    fun getSongsByAlbum(albumId: Long): Flow<List<Song>>

    fun getSongsByArtist(artistId: Long): Flow<List<Song>>

    fun searchSongs(query: String): Flow<List<Song>>

    /**
     * Songs whose indexed lyrics contain [query].
     *
     * Only lyrics already resolved by the player are searchable (played, opened on the detail
     * page, or edited by the user); sidecar/embedded text is indexed the first time it is read.
     */
    fun searchLyrics(query: String): Flow<List<LyricsMatch>>

    suspend fun refreshSongs()

    suspend fun getSongCount(): Int

    fun getFavoriteSongs(): Flow<List<Song>>

    fun getMostPlayedSongs(): Flow<List<Song>>

    fun getRecentlyPlayedSongs(): Flow<List<Song>>

    suspend fun toggleFavorite(songId: Long): Boolean

    /**
     * 把一首在线曲目的**指针**（平台键 + 平台 id）写进库，让它能像本地曲目一样被收藏、加入歌单。
     *
     * 不写任何 URL：取链结果随时会过期，存下来只会变成一堆死链。已存在的那一行保留用户列
     * （收藏、播放次数、自定义封面）。
     */
    suspend fun upsertOnlineSong(song: Song)

    /**
     * Sets the favourite flag on many songs at once.
     *
     * One statement rather than a `toggleFavorite` loop: the loop would read each song before
     * writing it, so a batch of 500 would do 500 queries and leave the list half-updated if the
     * user navigated away mid-way.
     */
    suspend fun setFavoriteBatch(songIds: List<Long>, isFavorite: Boolean)

    suspend fun incrementPlayCount(songId: Long)

    suspend fun updateLastPlayedAt(songId: Long)

    suspend fun getLibraryStats(): LibraryStats

    suspend fun updateSongInfo(songId: Long, title: String?, artist: String?, album: String?): Boolean

    /** 已授权的浏览目录树（SAF）。 */
    suspend fun getFolderRoots(): List<FolderRoot>

    /** 授权并记住一个 SAF 目录树；返回 false 表示授权无法持久化。 */
    suspend fun addFolderRoot(treeUri: String): Boolean

    /**
     * 读取 [rootUri] 目录树下 [documentId] 目录的直接子目录与音频文件。
     *
     * [documentId] 为空串表示目录树根。文件不经媒体库（MediaStore/Room）发现，
     * 未扫描过的音频同样会列出。
     */
    suspend fun getFolderContent(rootUri: String, documentId: String): FolderContent

    /**
     * 媒体库（MediaStore/Room 扫描产物）的目录树浏览，[path] 为空串时列顶层目录。
     * 与 SAF 方式并存，供用户在文件夹页自行选择。
     */
    suspend fun getLibraryFolderContent(path: String): FolderContent

    /**
     * How far [songId]'s lyrics have been nudged by the user, in milliseconds. Positive shifts the
     * lyrics later, negative shifts them earlier.
     *
     * Per song rather than global on purpose: a wrong offset comes from how one particular lyric
     * file was timed, so correcting one track must not shift every other track as well.
     */
    suspend fun getLyricsOffset(songId: Long): Long

    suspend fun setLyricsOffset(songId: Long, offsetMs: Long)
}
