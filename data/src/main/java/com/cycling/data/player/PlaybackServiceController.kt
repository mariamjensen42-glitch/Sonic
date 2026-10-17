package com.cycling.data.player

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts the foreground playback service.
 *
 * Call this immediately before playback begins, never at app launch. `MediaSessionService` only
 * promotes itself to the foreground while its player is actually playing — the notification exists
 * to say "audio is running" — so a service started while the app is idle never reaches
 * `startForeground()` and Android kills the process about five seconds later with
 * `ForegroundServiceDidNotStartInTimeException`.
 *
 * Every call site is on the foreground path for a second reason: `startForegroundService()` is
 * rejected outright when the app is in the background.
 *
 * A one-method interface purely so
 * [com.cycling.data.player.PlayerManager] does not have to hold a `Context` — that keeps the
 * queue and state logic testable with a plain JVM test instead of needing an Android runtime.
 */
interface PlaybackServiceController {
    fun start()
}

@Singleton
class AndroidPlaybackServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) : PlaybackServiceController {
    override fun start() = PlayerService.start(context)
}
