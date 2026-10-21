package com.cycling.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cycling.data.local.entity.SongMetadataEntity

@Dao
interface SongMetadataDao {

    @Query("SELECT * FROM song_metadata WHERE songId = :songId")
    suspend fun get(songId: Long): SongMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: SongMetadataEntity)
}
