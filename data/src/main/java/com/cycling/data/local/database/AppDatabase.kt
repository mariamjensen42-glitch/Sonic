package com.cycling.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cycling.data.local.dao.AiInfoCacheDao
import com.cycling.data.local.dao.PlaylistDao
import com.cycling.data.local.dao.SearchHistoryDao
import com.cycling.data.local.dao.SongDao
import com.cycling.data.local.entity.AiInfoCacheEntity
import com.cycling.data.local.entity.PlaylistEntity
import com.cycling.data.local.entity.PlaylistSongEntity
import com.cycling.data.local.entity.SearchHistoryEntity
import com.cycling.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        AiInfoCacheEntity::class,
        SearchHistoryEntity::class   
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun aiInfoCacheDao(): AiInfoCacheDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}
