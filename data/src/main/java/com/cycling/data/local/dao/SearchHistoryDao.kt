package com.cycling.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cycling.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 20")
    fun getAllHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: SearchHistoryEntity)

    @Query("DELETE FROM search_history")
    suspend fun clearAllHistory()

    @Query("SELECT DISTINCT query FROM search_history WHERE query LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT 5")
    fun getSuggestions(query: String): Flow<List<String>>
}
