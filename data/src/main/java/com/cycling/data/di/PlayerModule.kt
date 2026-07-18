package com.cycling.data.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.cycling.data.player.PlayerManager
import com.cycling.data.repository.PlayerRepositoryImpl
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SongRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    @Singleton
    abstract fun bindPlayerRepository(
        playerRepositoryImpl: PlayerRepositoryImpl
    ): PlayerRepository

    companion object {
        @Provides
        @Singleton
        @OptIn(UnstableApi::class)
        fun provideExoPlayer(
            @ApplicationContext context: Context
        ): ExoPlayer {
            val dataSourceFactory = DefaultDataSource.Factory(context)
            
            return ExoPlayer.Builder(context)
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .setHandleAudioBecomingNoisy(true)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .build()
        }

        @Provides
        @Singleton
        fun providePlayerManager(
            @ApplicationContext context: Context,
            songRepository: SongRepository
        ): PlayerManager {
            return PlayerManager(context, songRepository)
        }
    }
}
