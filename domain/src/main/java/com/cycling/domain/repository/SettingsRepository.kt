package com.cycling.domain.repository

import com.cycling.domain.model.AudioEffectsSettings
import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * User preferences, one [Flow] per setting.
 *
 * Deliberately not a single `Flow<AppSettings>` snapshot: a theme change would then re-emit the
 * equalizer state and the lyrics provider order too, and every screen collecting any part of it
 * would recompose. Granular flows keep each change local to the screens that care.
 *
 * Implementations must be usable before anything has been written, so every getter has a
 * documented default.
 */
interface SettingsRepository {

    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    /** Playback rate, clamped to [MIN_PLAYBACK_SPEED]..[MAX_PLAYBACK_SPEED]. */
    val playbackSpeed: Flow<Float>
    suspend fun setPlaybackSpeed(speed: Float)

    val skipSilence: Flow<Boolean>
    suspend fun setSkipSilence(enabled: Boolean)

    val rememberPlaybackPosition: Flow<Boolean>
    suspend fun setRememberPlaybackPosition(enabled: Boolean)

    /** Fade in/out length at track boundaries; 0 disables fading entirely. */
    val audioFadeDurationMs: Flow<Long>
    suspend fun setAudioFadeDurationMs(durationMs: Long)

    val replayGainEnabled: Flow<Boolean>
    suspend fun setReplayGainEnabled(enabled: Boolean)

    val audioEffects: Flow<AudioEffectsSettings>
    suspend fun setAudioEffects(settings: AudioEffectsSettings)

    /** Default duration offered by the sleep-timer sheet, in minutes. */
    val sleepTimerPresetMinutes: Flow<Int>
    suspend fun setSleepTimerPresetMinutes(minutes: Int)

    val onlineLyricsEnabled: Flow<Boolean>
    suspend fun setOnlineLyricsEnabled(enabled: Boolean)

    /** Providers to try, in order. Never empty. */
    val lyricsProviderOrder: Flow<List<LyricsProvider>>
    suspend fun setLyricsProviderOrder(providers: List<LyricsProvider>)

    companion object {
        const val MIN_PLAYBACK_SPEED = 0.5f
        const val MAX_PLAYBACK_SPEED = 2.0f
        const val DEFAULT_PLAYBACK_SPEED = 1.0f
        const val DEFAULT_AUDIO_FADE_DURATION_MS = 200L
        const val MAX_AUDIO_FADE_DURATION_MS = 2_000L
        const val DEFAULT_SLEEP_TIMER_PRESET_MINUTES = 30

        val DEFAULT_LYRICS_PROVIDER_ORDER = listOf(LyricsProvider.NETEASE, LyricsProvider.LRCLIB)
    }
}
