package com.cycling.data.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.cycling.data.R
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(UnstableApi::class)
class PlayerService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var bitmapLoader: MediaStoreBitmapLoader

    private var mediaSession: MediaSession? = null

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "music_playback_channel"

        /**
         * Starts the service in the foreground.
         *
         * The five-second deadline this call arms is only met once the player is engaged, so
         * callers must be on their way into `play()` — see [PlaybackServiceController.start] —
         * and [PlayerService.onCreate] must have registered the session with [addSession].
         */
        fun start(context: Context) {
            context.startForegroundService(Intent(context, PlayerService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("PlayerService onCreate")
        
        createNotificationChannel()
        
        val session = MediaSession.Builder(this, exoPlayer)
            .setCallback(PlayerSessionCallback())
            .setSessionActivity(createSessionActivity())
            // The stock loader cannot read this app's album-art URIs, which is why the notification
            // and the system control bar used to come up without a cover — see
            // [MediaStoreBitmapLoader].
            .setBitmapLoader(bitmapLoader)
            .build()
        mediaSession = session

        // Hand the session to MediaSessionService, which is what actually makes `startForeground`
        // reachable. The base class only learns about a session from a media-action start intent
        // or from a controller binding, and this app never uses a MediaController (see
        // ExoPlayerPlaybackEngine), so without this call nothing ever reaches
        // `updateNotification` and every `startForegroundService` is killed five seconds later
        // with ForegroundServiceDidNotStartInTimeException.
        addSession(session)

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Timber.d("PlayerService: isPlaying changed to $isPlaying")
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                Timber.d("PlayerService: mediaItem transition - ${mediaItem?.mediaMetadata?.title}")
            }
        })
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createSessionActivity(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, startInForegroundRequired)
        Timber.d("PlayerService: onUpdateNotification - startInForegroundRequired=$startInForegroundRequired")
    }

    override fun onDestroy() {
        Timber.d("PlayerService onDestroy")
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private inner class PlayerSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
    }
}
