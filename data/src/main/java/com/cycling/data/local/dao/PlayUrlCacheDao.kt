package com.cycling.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cycling.data.local.entity.PlayUrlCacheEntity

@Dao
interface PlayUrlCacheDao {

    @Query("SELECT * FROM play_url_cache WHERE songId = :songId AND quality = :quality")
    suspend fun get(songId: Long, quality: String): PlayUrlCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: PlayUrlCacheEntity)

    @Query("DELETE FROM play_url_cache WHERE songId = :songId AND quality = :quality")
    suspend fun delete(songId: Long, quality: String)

    @Query("DELETE FROM play_url_cache WHERE expiresAt <= :now")
    suspend fun purgeExpired(now: Long): Int

    @Query("DELETE FROM play_url_cache")
    suspend fun clear()
}
