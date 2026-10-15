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
import com.cycling.domain.model.AudioEffectsSettings
import com.cycling.domain.model.EqualizerSettings
import com.cycling.domain.model.LoudnessSettings
import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.model.ThemeMode
import com.cycling.domain.model.ToggleEffectSettings
import com.cycling.domain.repository.SettingsRepository
import com.cycling.domain.repository.SettingsRepository.Companion.DEFAULT_AUDIO_FADE_DURATION_MS
import com.cycling.domain.repository.SettingsRepository.Companion.DEFAULT_SLEEP_TIMER_PRESET_MINUTES
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
 * The audio-effect settings are stored as flat scalar keys rather than a serialised blob: the
 * domain models live in `:domain`, which has no serialization plugin, and flattening avoids
 * keeping a parallel `@Serializable` DTO in sync with them. The only list is the equalizer band
 * levels, encoded as a comma-separated string.
 *
 * Reads clamp or fall back rather than trusting what is on disk — a preference can survive an
 * app downgrade, and a bad value must not reach ExoPlayer.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> =
        context.settingsDataStore.data.map { prefs ->
            SettingsConversions.parseEnum(prefs[THEME_MODE_KEY], ThemeMode.SYSTEM)
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

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

    override val audioEffects: Flow<AudioEffectsSettings> =
        context.settingsDataStore.data.map { prefs ->
            AudioEffectsSettings(
                equalizer = EqualizerSettings(
                    enabled = prefs[EQ_ENABLED_KEY] ?: false,
                    presetIndex = prefs[EQ_PRESET_KEY] ?: 0,
                    bandLevelsMdB = SettingsConversions.parseBandLevels(prefs[EQ_BANDS_KEY])
                ),
                bassBoost = ToggleEffectSettings(
                    enabled = prefs[BASS_ENABLED_KEY] ?: false,
                    strength = SettingsConversions.clampEffectStrength(prefs[BASS_STRENGTH_KEY] ?: SettingsConversions.DEFAULT_EFFECT_STRENGTH)
                ),
                virtualizer = ToggleEffectSettings(
                    enabled = prefs[VIRT_ENABLED_KEY] ?: false,
                    strength = SettingsConversions.clampEffectStrength(prefs[VIRT_STRENGTH_KEY] ?: SettingsConversions.DEFAULT_EFFECT_STRENGTH)
                ),
                loudness = LoudnessSettings(
                    enabled = prefs[LOUD_ENABLED_KEY] ?: false,
                    targetGainMdB = prefs[LOUD_GAIN_KEY] ?: 0
                )
            )
        }

    override suspend fun setAudioEffects(settings: AudioEffectsSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[EQ_ENABLED_KEY] = settings.equalizer.enabled
            prefs[EQ_PRESET_KEY] = settings.equalizer.presetIndex
            prefs[EQ_BANDS_KEY] = SettingsConversions.encodeBandLevels(settings.equalizer.bandLevelsMdB)
            prefs[BASS_ENABLED_KEY] = settings.bassBoost.enabled
            prefs[BASS_STRENGTH_KEY] = SettingsConversions.clampEffectStrength(settings.bassBoost.strength)
            prefs[VIRT_ENABLED_KEY] = settings.virtualizer.enabled
            prefs[VIRT_STRENGTH_KEY] = SettingsConversions.clampEffectStrength(settings.virtualizer.strength)
            prefs[LOUD_ENABLED_KEY] = settings.loudness.enabled
            prefs[LOUD_GAIN_KEY] = settings.loudness.targetGainMdB
        }
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

    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val PLAYBACK_SPEED_KEY = stringPreferencesKey("playback_speed")
        private val SKIP_SILENCE_KEY = booleanPreferencesKey("skip_silence")
        private val REMEMBER_POSITION_KEY = booleanPreferencesKey("remember_playback_position")
        private val AUDIO_FADE_DURATION_KEY = longPreferencesKey("audio_fade_duration_ms")
        private val REPLAY_GAIN_KEY = booleanPreferencesKey("replay_gain_enabled")

        private val EQ_ENABLED_KEY = booleanPreferencesKey("equalizer_enabled")
        private val EQ_PRESET_KEY = intPreferencesKey("equalizer_preset")
        private val EQ_BANDS_KEY = stringPreferencesKey("equalizer_band_levels")
        private val BASS_ENABLED_KEY = booleanPreferencesKey("bass_boost_enabled")
        private val BASS_STRENGTH_KEY = intPreferencesKey("bass_boost_strength")
        private val VIRT_ENABLED_KEY = booleanPreferencesKey("virtualizer_enabled")
        private val VIRT_STRENGTH_KEY = intPreferencesKey("virtualizer_strength")
        private val LOUD_ENABLED_KEY = booleanPreferencesKey("loudness_enabled")
        private val LOUD_GAIN_KEY = intPreferencesKey("loudness_gain_mdb")

        private val SLEEP_TIMER_PRESET_KEY = intPreferencesKey("sleep_timer_preset_minutes")
        private val ONLINE_LYRICS_KEY = booleanPreferencesKey("online_lyrics_enabled")
        private val LYRICS_PROVIDERS_KEY = stringPreferencesKey("lyrics_providers")
    }
}
