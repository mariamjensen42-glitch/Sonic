package com.cycling.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cycling.domain.model.HiResBadge
import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.repository.SettingsRepository
import com.cycling.domain.repository.SettingsRepository.Companion.DEFAULT_AUDIO_FADE_DURATION_MS
import com.cycling.domain.repository.SettingsRepository.Companion.DEFAULT_CACHE_LIMIT_BYTES
import com.cycling.domain.repository.SettingsRepository.Companion.DEFAULT_LYRICS_TEXT_SIZE_SP
import com.cycling.domain.repository.SettingsRepository.Companion.DEFAULT_SLEEP_TIMER_PRESET_MINUTES
import com.cycling.domain.repository.SettingsRepository.Companion.MAX_CACHE_LIMIT_BYTES
import com.cycling.domain.repository.SettingsRepository.Companion.MAX_LYRICS_TEXT_SIZE_SP
import com.cycling.domain.repository.SettingsRepository.Companion.MIN_CACHE_LIMIT_BYTES
import com.cycling.domain.repository.SettingsRepository.Companion.MIN_LYRICS_TEXT_SIZE_SP
import com.cycling.domain.repository.SettingsRepository.Companion.MAX_AUDIO_FADE_DURATION_MS
import com.cycling.domain.repository.SettingsRepository.Companion.MAX_PLAYBACK_SPEED
import com.cycling.domain.repository.SettingsRepository.Companion.MIN_PLAYBACK_SPEED
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "sonic_settings")

/**
 * Every user-facing preference, in one DataStore file.
 *
 * Reads clamp or fall back rather than trusting what is on disk — a preference can survive an
 * app downgrade, and a bad value must not reach ExoPlayer.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    override val playbackSpeed: Flow<Float> =
        context.settingsDataStore.data.map { prefs ->
            SettingsConversions.parsePlaybackSpeed(prefs[PLAYBACK_SPEED_KEY])
        }

    override suspend fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED)
        context.settingsDataStore.edit { it[PLAYBACK_SPEED_KEY] = clamped.toString() }
    }

    override val skipSilence: Flow<Boolean> =
        context.settingsDataStore.data.map { it[SKIP_SILENCE_KEY] ?: false }

    override suspend fun setSkipSilence(enabled: Boolean) {
        context.settingsDataStore.edit { it[SKIP_SILENCE_KEY] = enabled }
    }

    override val playerSpectrumEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[PLAYER_SPECTRUM_KEY] ?: true }

    override suspend fun setPlayerSpectrumEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[PLAYER_SPECTRUM_KEY] = enabled }
    }

    override val rememberPlaybackPosition: Flow<Boolean> =
        context.settingsDataStore.data.map { it[REMEMBER_POSITION_KEY] ?: true }

    override suspend fun setRememberPlaybackPosition(enabled: Boolean) {
        context.settingsDataStore.edit { it[REMEMBER_POSITION_KEY] = enabled }
    }

    override val audioFadeDurationMs: Flow<Long> =
        context.settingsDataStore.data.map { prefs ->
            prefs[AUDIO_FADE_DURATION_KEY]?.coerceIn(0L, MAX_AUDIO_FADE_DURATION_MS)
                ?: DEFAULT_AUDIO_FADE_DURATION_MS
        }

    override suspend fun setAudioFadeDurationMs(durationMs: Long) {
        val clamped = durationMs.coerceIn(0L, MAX_AUDIO_FADE_DURATION_MS)
        context.settingsDataStore.edit { it[AUDIO_FADE_DURATION_KEY] = clamped }
    }

    override val replayGainEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[REPLAY_GAIN_KEY] ?: false }

    override suspend fun setReplayGainEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[REPLAY_GAIN_KEY] = enabled }
    }

    override val sleepTimerPresetMinutes: Flow<Int> =
        context.settingsDataStore.data.map { it[SLEEP_TIMER_PRESET_KEY] ?: DEFAULT_SLEEP_TIMER_PRESET_MINUTES }

    override suspend fun setSleepTimerPresetMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[SLEEP_TIMER_PRESET_KEY] = minutes.coerceAtLeast(1) }
    }

    override val onlineLyricsEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[ONLINE_LYRICS_KEY] ?: true }

    override suspend fun setOnlineLyricsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[ONLINE_LYRICS_KEY] = enabled }
    }

    override val openPlayerOnPlay: Flow<Boolean> =
        context.settingsDataStore.data.map { it[OPEN_PLAYER_ON_PLAY_KEY] ?: true }

    override suspend fun setOpenPlayerOnPlay(enabled: Boolean) {
        context.settingsDataStore.edit { it[OPEN_PLAYER_ON_PLAY_KEY] = enabled }
    }

    override val autoContinueEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[AUTO_CONTINUE_KEY] ?: true }

    override suspend fun setAutoContinueEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[AUTO_CONTINUE_KEY] = enabled }
    }

    override val cacheLimitBytes: Flow<Long> =
        context.settingsDataStore.data.map { prefs ->
            (prefs[CACHE_LIMIT_KEY] ?: DEFAULT_CACHE_LIMIT_BYTES)
                .coerceIn(MIN_CACHE_LIMIT_BYTES, MAX_CACHE_LIMIT_BYTES)
        }

    override suspend fun setCacheLimitBytes(bytes: Long) {
        context.settingsDataStore.edit {
            it[CACHE_LIMIT_KEY] = bytes.coerceIn(MIN_CACHE_LIMIT_BYTES, MAX_CACHE_LIMIT_BYTES)
        }
    }

    override val autoCleanCacheEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[AUTO_CLEAN_CACHE_KEY] ?: true }

    override suspend fun setAutoCleanCacheEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[AUTO_CLEAN_CACHE_KEY] = enabled }
    }

    override val lyricsProviderOrder: Flow<List<LyricsProvider>> =
        context.settingsDataStore.data.map { prefs ->
            SettingsConversions.parseLyricsProviders(prefs[LYRICS_PROVIDERS_KEY])
        }

    override suspend fun setLyricsProviderOrder(providers: List<LyricsProvider>) {
        // Never store an empty order — that would silently disable online lyrics.
        context.settingsDataStore.edit {
            it[LYRICS_PROVIDERS_KEY] = SettingsConversions.encodeLyricsProviders(providers)
        }
    }

    override val lyricsTextSizeSp: Flow<Int> =
        context.settingsDataStore.data.map { prefs ->
            (prefs[LYRICS_TEXT_SIZE_KEY] ?: DEFAULT_LYRICS_TEXT_SIZE_SP)
                .coerceIn(MIN_LYRICS_TEXT_SIZE_SP, MAX_LYRICS_TEXT_SIZE_SP)
        }

    override suspend fun setLyricsTextSizeSp(sp: Int) {
        context.settingsDataStore.edit {
            it[LYRICS_TEXT_SIZE_KEY] = sp.coerceIn(MIN_LYRICS_TEXT_SIZE_SP, MAX_LYRICS_TEXT_SIZE_SP)
        }
    }

    override val lyricsAlignCenter: Flow<Boolean> =
        context.settingsDataStore.data.map { it[LYRICS_ALIGN_CENTER_KEY] ?: false }

    override suspend fun setLyricsAlignCenter(enabled: Boolean) {
        context.settingsDataStore.edit { it[LYRICS_ALIGN_CENTER_KEY] = enabled }
    }

    override val lyricsGlowEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[LYRICS_GLOW_KEY] ?: false }

    override suspend fun setLyricsGlowEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[LYRICS_GLOW_KEY] = enabled }
    }

    override val lyricsBlurEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[LYRICS_BLUR_KEY] ?: true }

    override suspend fun setLyricsBlurEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[LYRICS_BLUR_KEY] = enabled }
    }

    override val hiResBadge: Flow<HiResBadge> =
        context.settingsDataStore.data.map { prefs ->
            HiResBadge.fromId(prefs[HI_RES_BADGE_KEY])
        }

    override suspend fun setHiResBadge(badge: HiResBadge) {
        context.settingsDataStore.edit { it[HI_RES_BADGE_KEY] = badge.name }
    }

    override val hiResBadgeCustomUri: Flow<String?> =
        context.settingsDataStore.data.map { it[HI_RES_BADGE_URI_KEY] }

    override suspend fun setHiResBadgeCustomUri(uri: String?) {
        context.settingsDataStore.edit { prefs ->
            if (uri == null) prefs.remove(HI_RES_BADGE_URI_KEY) else prefs[HI_RES_BADGE_URI_KEY] = uri
        }
    }

    companion object {
        private val PLAYBACK_SPEED_KEY = stringPreferencesKey("playback_speed")
        private val SKIP_SILENCE_KEY = booleanPreferencesKey("skip_silence")
        private val PLAYER_SPECTRUM_KEY = booleanPreferencesKey("player_spectrum_enabled")
        private val REMEMBER_POSITION_KEY = booleanPreferencesKey("remember_playback_position")
        private val AUDIO_FADE_DURATION_KEY = longPreferencesKey("audio_fade_duration_ms")
        private val REPLAY_GAIN_KEY = booleanPreferencesKey("replay_gain_enabled")

        private val SLEEP_TIMER_PRESET_KEY = intPreferencesKey("sleep_timer_preset_minutes")
        private val ONLINE_LYRICS_KEY = booleanPreferencesKey("online_lyrics_enabled")
        private val OPEN_PLAYER_ON_PLAY_KEY = booleanPreferencesKey("open_player_on_play")
        private val AUTO_CONTINUE_KEY = booleanPreferencesKey("auto_continue_enabled")
        private val CACHE_LIMIT_KEY = longPreferencesKey("cache_limit_bytes")
        private val AUTO_CLEAN_CACHE_KEY = booleanPreferencesKey("auto_clean_cache_enabled")
        private val LYRICS_PROVIDERS_KEY = stringPreferencesKey("lyrics_providers")
        private val LYRICS_TEXT_SIZE_KEY = intPreferencesKey("lyrics_text_size_sp")
        private val LYRICS_ALIGN_CENTER_KEY = booleanPreferencesKey("lyrics_align_center")
        private val LYRICS_GLOW_KEY = booleanPreferencesKey("lyrics_glow_enabled")
        private val LYRICS_BLUR_KEY = booleanPreferencesKey("lyrics_blur_enabled")
        private val HI_RES_BADGE_KEY = stringPreferencesKey("hi_res_badge")
        private val HI_RES_BADGE_URI_KEY = stringPreferencesKey("hi_res_badge_custom_uri")
    }
}
