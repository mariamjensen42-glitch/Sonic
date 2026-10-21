package com.cycling.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cycling.data.local.entity.LyricsCacheEntity

@Dao
interface LyricsCacheDao {

    @Query("SELECT * FROM lyrics_cache WHERE cacheKey = :cacheKey")
    suspend fun get(cacheKey: String): LyricsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: LyricsCacheEntity)

    @Query("DELETE FROM lyrics_cache WHERE cacheKey = :cacheKey")
    suspend fun delete(cacheKey: String)

    @Query("DELETE FROM lyrics_cache WHERE createdAt < :before")
    suspend fun deleteOlderThan(before: Long)

    /** 歌词来源集合变化时整表清空：旧行没有「比它优先的来源还没问过」这个信息。 */
    @Query("DELETE FROM lyrics_cache")
    suspend fun clear()

    /** 缓存面板的用量统计。 */
    @Query("SELECT COUNT(*) FROM lyrics_cache")
    suspend fun count(): Int
}
