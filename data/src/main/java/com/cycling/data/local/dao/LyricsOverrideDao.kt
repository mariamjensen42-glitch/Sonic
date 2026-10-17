package com.cycling.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cycling.data.local.entity.LyricsOverrideEntity

@Dao
interface LyricsOverrideDao {

    @Query("SELECT * FROM lyrics_override WHERE songId = :songId")
    suspend fun get(songId: Long): LyricsOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: LyricsOverrideEntity)

    @Query("DELETE FROM lyrics_override WHERE songId = :songId")
    suspend fun delete(songId: Long)
}
