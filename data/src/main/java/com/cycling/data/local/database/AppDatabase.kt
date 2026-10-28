package com.cycling.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cycling.data.local.dao.AiInfoCacheDao
import com.cycling.data.local.dao.DownloadTaskDao
import com.cycling.data.local.dao.LyricsCacheDao
import com.cycling.data.local.dao.LyricsIndexDao
import com.cycling.data.local.dao.LyricsOverrideDao
import com.cycling.data.local.dao.PlayEventDao
import com.cycling.data.local.dao.PlayUrlCacheDao
import com.cycling.data.local.dao.PlaylistDao
import com.cycling.data.local.dao.SearchHistoryDao
import com.cycling.data.local.dao.SongDao
import com.cycling.data.local.dao.SongMetadataDao
import com.cycling.data.local.dao.SongStateDao
import com.cycling.data.local.entity.AiInfoCacheEntity
import com.cycling.data.local.entity.DownloadTaskEntity
import com.cycling.data.local.entity.LyricsCacheEntity
import com.cycling.data.local.entity.LyricsIndexEntity
import com.cycling.data.local.entity.LyricsOverrideEntity
import com.cycling.data.local.entity.PlayEventEntity
import com.cycling.data.local.entity.PlayUrlCacheEntity
import com.cycling.data.local.entity.PlaylistEntity
import com.cycling.data.local.entity.PlaylistSongEntity
import com.cycling.data.local.entity.SearchHistoryEntity
import com.cycling.data.local.entity.SongEntity
import com.cycling.data.local.entity.SongMetadataEntity
import com.cycling.data.local.entity.SongStateEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        AiInfoCacheEntity::class,
        SearchHistoryEntity::class,
        SongStateEntity::class,
        LyricsCacheEntity::class,
        LyricsOverrideEntity::class,
        LyricsIndexEntity::class,
        SongMetadataEntity::class,
        PlayEventEntity::class,
        DownloadTaskEntity::class,
        PlayUrlCacheEntity::class
    ],
    version = 10,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun aiInfoCacheDao(): AiInfoCacheDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun songStateDao(): SongStateDao
    abstract fun lyricsCacheDao(): LyricsCacheDao
    abstract fun lyricsOverrideDao(): LyricsOverrideDao
    abstract fun lyricsIndexDao(): LyricsIndexDao
    abstract fun songMetadataDao(): SongMetadataDao
    abstract fun playEventDao(): PlayEventDao
    abstract fun downloadTaskDao(): DownloadTaskDao
    abstract fun playUrlCacheDao(): PlayUrlCacheDao
}
