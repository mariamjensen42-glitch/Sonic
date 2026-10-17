package com.cycling.data.di

import android.content.Context
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.cycling.data.player.AndroidPlaybackServiceController
import com.cycling.data.player.PlaybackServiceController
import com.cycling.data.player.PlayerManager
import com.cycling.data.player.engine.ExoPlayerPlaybackEngine
import com.cycling.data.player.engine.PlaybackEngine
import com.cycling.domain.repository.PlayerRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    @Singleton
    abstract fun bindPlayerRepository(playerManager: PlayerManager): PlayerRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackEngine(engine: ExoPlayerPlaybackEngine): PlaybackEngine

    @Binds
    @Singleton
    abstract fun bindPlaybackServiceController(
        impl: AndroidPlaybackServiceController
    ): PlaybackServiceController

    companion object {

        @Provides
        @Singleton
        @OptIn(UnstableApi::class)
        fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer {
            // The session id is generated and pinned up front rather than read back later:
            // `ExoPlayer.audioSessionId` stays AUDIO_SESSION_ID_UNSET until the first audio sink
            // exists.
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val sessionId = audioManager.generateAudioSessionId()

            val dataSourceFactory = DefaultDataSource.Factory(context)
            return ExoPlayer.Builder(context)
                .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
                .setHandleAudioBecomingNoisy(true)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .build()
                .apply { setAudioSessionId(sessionId) }
        }

        /**
         * `Main.immediate` so engine calls made from the UI thread take effect synchronously
         * instead of hopping through the queue; tests substitute a test dispatcher.
         */
        @Provides
        @PlayerDispatcher
        fun providePlayerDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate
    }
}
