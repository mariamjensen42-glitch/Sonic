package com.cycling.data.di

import android.content.Context
import androidx.room.Room
import com.cycling.data.local.dao.AiInfoCacheDao
import com.cycling.data.local.dao.LyricsCacheDao
import com.cycling.data.local.dao.LyricsIndexDao
import com.cycling.data.local.dao.LyricsOverrideDao
import com.cycling.data.local.dao.PlaylistDao
import com.cycling.data.local.dao.SearchHistoryDao
import com.cycling.data.local.dao.SongDao
import com.cycling.data.local.dao.SongStateDao
import com.cycling.data.local.database.AppDatabase
import com.cycling.data.local.database.MIGRATION_1_2
import com.cycling.data.local.database.MIGRATION_2_3
import com.cycling.data.local.database.MIGRATION_3_4
import com.cycling.data.local.database.MIGRATION_4_5
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "sonic_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            // Downgrades only happen on a sideloaded older build; there is nothing to preserve.
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideSongDao(database: AppDatabase): SongDao {
        return database.songDao()
    }

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideAiInfoCacheDao(database: AppDatabase): AiInfoCacheDao {
        return database.aiInfoCacheDao()
    }

    @Provides
    fun provideSearchHistoryDao(database: AppDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }

    @Provides
    fun provideSongStateDao(database: AppDatabase): SongStateDao {
        return database.songStateDao()
    }

    @Provides
    fun provideLyricsCacheDao(database: AppDatabase): LyricsCacheDao {
        return database.lyricsCacheDao()
    }

    @Provides
    fun provideLyricsOverrideDao(database: AppDatabase): LyricsOverrideDao {
        return database.lyricsOverrideDao()
    }

    @Provides
    fun provideLyricsIndexDao(database: AppDatabase): LyricsIndexDao {
        return database.lyricsIndexDao()
    }
}
