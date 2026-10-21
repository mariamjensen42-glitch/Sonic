package com.cycling.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cycling.data.local.entity.LibraryStatsEntity
import com.cycling.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY title ASC")
    fun getSongsByAlbum(albumId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE artistId = :artistId ORDER BY title ASC")
    fun getSongsByArtist(artistId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE playCount > 0 ORDER BY playCount DESC")
    fun getMostPlayedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC")
    fun getRecentlyPlayedSongs(): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    /**
     * Scanned-row upsert that deliberately leaves every user-owned column alone.
     *
     * Two reasons this is hand-written rather than `@Insert(REPLACE)`:
     *
     * 1. `INSERT OR REPLACE` deletes the conflicting row before inserting, and a delete on
     *    `songs` fires the `ON DELETE CASCADE` on `playlist_songs` — so a rescan would empty
     *    every playlist. A native UPSERT updates in place and fires nothing.
     * 2. The columns omitted from `DO UPDATE SET` are exactly the ones the user owns, so their
     *    preservation is enforced by SQL rather than by remembering to merge them in Kotlin.
     *    `isFavorite`, `playCount`, `lastPlayedAt`, `rating`, `customAlbumArt` and `dateAdded`
     *    are intentionally absent below.
     *
     * The parameters are scalars rather than a [SongEntity] because Room cannot bind the fields
     * of an object parameter in a `@Query`.
     */
    @Query(
        """
        INSERT INTO songs (
            id, title, artist, album, duration, path, albumId, artistId, dateAdded,
            dateModified, size, mimeType, albumArt, isFavorite, playCount, lastPlayedAt,
            bitrate, rating, customAlbumArt
        ) VALUES (
            :id, :title, :artist, :album, :duration, :path, :albumId, :artistId, :dateAdded,
            :dateModified, :size, :mimeType, :albumArt, :isFavorite, :playCount, :lastPlayedAt,
            :bitrate, :rating, :customAlbumArt
        )
        ON CONFLICT(id) DO UPDATE SET
            title = excluded.title,
            artist = excluded.artist,
            album = excluded.album,
            duration = excluded.duration,
            path = excluded.path,
            albumId = excluded.albumId,
            artistId = excluded.artistId,
            dateModified = excluded.dateModified,
            size = excluded.size,
            mimeType = excluded.mimeType,
            albumArt = excluded.albumArt,
            bitrate = excluded.bitrate
        """
    )
    @Suppress("LongParameterList")
    suspend fun upsertScannedSong(
        id: Long,
        title: String,
        artist: String,
        album: String,
        duration: Long,
        path: String,
        albumId: Long,
        artistId: Long,
        dateAdded: Long,
        dateModified: Long,
        size: Long,
        mimeType: String,
        albumArt: String?,
        isFavorite: Boolean,
        playCount: Int,
        lastPlayedAt: Long?,
        bitrate: Int,
        rating: Int,
        customAlbumArt: String?
    )

    /**
     * 在线曲目入库。只写元数据与指针，用户列（收藏、次数、自定义封面）一概不碰。
     *
     * `path` 固定空串、`albumId`/`artistId` 固定 -1：在线曲目没有文件、也不属于本地专辑/艺术家，
     * 但它们仍是 `songs` 表的一行，这样才能直接复用收藏、歌单、播放次数与统计。
     */
    @Query(
        """
        INSERT INTO songs (
            id, title, artist, album, duration, path, albumId, artistId, dateAdded,
            dateModified, size, mimeType, albumArt, isFavorite, playCount, lastPlayedAt,
            bitrate, rating, customAlbumArt, onlineSourceKey, onlineSongId
        ) VALUES (
            :id, :title, :artist, :album, :duration, '', -1, -1, :now,
            0, 0, :mimeType, :albumArt, 0, 0, NULL,
            0, 0, NULL, :onlineSourceKey, :onlineSongId
        )
        ON CONFLICT(id) DO UPDATE SET
            title = excluded.title,
            artist = excluded.artist,
            album = excluded.album,
            duration = excluded.duration,
            albumArt = excluded.albumArt,
            mimeType = excluded.mimeType,
            onlineSourceKey = excluded.onlineSourceKey,
            onlineSongId = excluded.onlineSongId
        """
    )
    suspend fun upsertOnlineSong(
        id: Long,
        title: String,
        artist: String,
        album: String,
        duration: Long,
        mimeType: String,
        albumArt: String?,
        onlineSourceKey: String,
        onlineSongId: String,
        now: Long
    )

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<Long>)

    @Query("DELETE FROM songs WHERE path IN (:paths)")
    suspend fun deleteSongsByPaths(paths: List<String>)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun updateFavorite(songId: Long, isFavorite: Boolean)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id IN (:songIds)")
    suspend fun updateFavoriteBatch(songIds: List<Long>, isFavorite: Boolean)

    @Query("UPDATE songs SET rating = :rating WHERE id = :songId")
    suspend fun updateRating(songId: Long, rating: Int)

    @Query("UPDATE songs SET customAlbumArt = :path WHERE id = :songId")
    suspend fun updateCustomAlbumArt(songId: Long, path: String?)

    @Query("SELECT * FROM songs WHERE rating > 0 ORDER BY rating DESC, title ASC")
    fun getRatedSongs(): Flow<List<SongEntity>>

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedAt = :lastPlayedAt WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long, lastPlayedAt: Long)

    @Query("UPDATE songs SET lastPlayedAt = :lastPlayedAt WHERE id = :songId")
    suspend fun updateLastPlayedAt(songId: Long, lastPlayedAt: Long)

    /**
     * Restores listening stats from a backup, never lowering what is already on this device.
     *
     * `MAX` rather than assignment: a restore is usually onto a fresh install (nothing to lose),
     * but importing an older backup over a lived-in library must not erase later plays.
     */
    @Query(
        """
        UPDATE songs SET
            playCount = MAX(playCount, :playCount),
            lastPlayedAt = CASE
                WHEN lastPlayedAt IS NULL THEN :lastPlayedAt
                WHEN :lastPlayedAt IS NULL THEN lastPlayedAt
                ELSE MAX(lastPlayedAt, :lastPlayedAt)
            END
        WHERE id = :songId
        """
    )
    suspend fun restorePlayStats(songId: Long, playCount: Int, lastPlayedAt: Long?)

    @Query("SELECT * FROM songs WHERE bitrate = 0")
    suspend fun getSongsWithoutBitrate(): List<SongEntity>

    @Query("UPDATE songs SET bitrate = :bitrate WHERE id = :songId")
    suspend fun updateBitrate(songId: Long, bitrate: Int)

    @Query("UPDATE songs SET title = :title, artist = :artist, album = :album WHERE id = :songId")
    suspend fun updateSongInfo(songId: Long, title: String, artist: String, album: String)

    @Query("""
        SELECT
            COUNT(*) as totalSongs,
            IFNULL(SUM(CASE WHEN bitrate > 1411 THEN 1 ELSE 0 END), 0) as hrCount,
            IFNULL(SUM(CASE WHEN bitrate = 1411 THEN 1 ELSE 0 END), 0) as sqCount,
            IFNULL(SUM(CASE WHEN bitrate >= 320 AND bitrate < 1411 THEN 1 ELSE 0 END), 0) as hqCount,
            IFNULL(SUM(CASE WHEN bitrate < 320 OR bitrate = 0 THEN 1 ELSE 0 END), 0) as othersCount
        FROM songs
        WHERE onlineSourceKey IS NULL
    """)
    suspend fun getLibraryStats(): LibraryStatsEntity

    @Query("SELECT path FROM songs")
    suspend fun getAllSongPaths(): List<String>

    /**
     * 只取本地文件路径，供扫描器判定 stale。
     *
     * 在线行的 `path` 是空串，永远不在 MediaStore 的结果集里；用全量路径去比对，
     * 每次扫描都会把它们当成"文件已删除"清掉。
     */
    @Query("SELECT path FROM songs WHERE onlineSourceKey IS NULL")
    suspend fun getLocalSongPaths(): List<String>

    @Query("SELECT * FROM songs WHERE path LIKE :pathPrefix || '%' ORDER BY title ASC")
    suspend fun getSongsByPathPrefix(pathPrefix: String): List<SongEntity>

    /** 已入库曲目的 (bitrate, dateModified)，供扫描跳过逐个文件读元数据。 */
    @Query("SELECT id, bitrate, dateModified FROM songs")
    suspend fun getKnownSongFiles(): List<KnownSongFile>
}

/**
 * 扫描去重用的极小投影：只回答"这首的比特率还能不能直接沿用"。
 *
 * 单独一个投影而不是整行 `SongEntity`：全库扫描时要把它整张读出来跟 MediaStore 对，
 * 只取这三列才不会把一次增量刷新变成一次全表反序列化。
 */
data class KnownSongFile(
    val id: Long,
    val bitrate: Int,
    val dateModified: Long
)
