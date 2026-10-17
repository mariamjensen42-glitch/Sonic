package com.cycling.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cycling.data.local.entity.AiInfoCacheEntity

@Dao
interface AiInfoCacheDao {
    @Query("SELECT * FROM ai_info_cache WHERE cacheKey = :cacheKey")
    suspend fun getByCacheKey(cacheKey: String): AiInfoCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cacheEntity: AiInfoCacheEntity)

}
