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
    suspend fun insertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Delete
    suspend fun deleteSong(song: SongEntity)

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun updateFavorite(songId: Long, isFavorite: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedAt = :lastPlayedAt WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long, lastPlayedAt: Long)

    @Query("UPDATE songs SET lastPlayedAt = :lastPlayedAt WHERE id = :songId")
    suspend fun updateLastPlayedAt(songId: Long, lastPlayedAt: Long)

    @Query("SELECT COUNT(*) FROM songs WHERE bitrate > 1411")
    suspend fun getHrCount(): Int

    @Query("SELECT COUNT(*) FROM songs WHERE bitrate = 1411")
    suspend fun getSqCount(): Int

    @Query("SELECT COUNT(*) FROM songs WHERE bitrate >= 320 AND bitrate < 1411")
    suspend fun getHqCount(): Int

    @Query("SELECT COUNT(*) FROM songs WHERE bitrate < 320 OR bitrate = 0")
    suspend fun getOthersCount(): Int

    @Query("SELECT * FROM songs WHERE bitrate = 0")
    suspend fun getSongsWithoutBitrate(): List<SongEntity>

    @Query("UPDATE songs SET bitrate = :bitrate WHERE id = :songId")
    suspend fun updateBitrate(songId: Long, bitrate: Int)

    @Query("UPDATE songs SET title = :title, artist = :artist, album = :album WHERE id = :songId")
    suspend fun updateSongInfo(songId: Long, title: String, artist: String, album: String)

    @Query("""
        SELECT 
            COUNT(*) as totalSongs,
            SUM(CASE WHEN bitrate > 1411 THEN 1 ELSE 0 END) as hrCount,
            SUM(CASE WHEN bitrate = 1411 THEN 1 ELSE 0 END) as sqCount,
            SUM(CASE WHEN bitrate >= 320 AND bitrate < 1411 THEN 1 ELSE 0 END) as hqCount,
            SUM(CASE WHEN bitrate < 320 OR bitrate = 0 THEN 1 ELSE 0 END) as othersCount
        FROM songs
    """)
    suspend fun getLibraryStats(): LibraryStatsEntity

    @Query("SELECT path FROM songs")
    suspend fun getAllSongPaths(): List<String>

    @Query("SELECT * FROM songs WHERE path LIKE :pathPrefix || '%' ORDER BY title ASC")
    suspend fun getSongsByPathPrefix(pathPrefix: String): List<SongEntity>
}
