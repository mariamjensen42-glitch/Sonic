package com.cycling.data.repository

import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.repository.SettingsRepository.Companion.DEFAULT_LYRICS_PROVIDER_ORDER
import com.cycling.domain.repository.SettingsRepository.Companion.DEFAULT_PLAYBACK_SPEED
import com.cycling.domain.repository.SettingsRepository.Companion.MAX_PLAYBACK_SPEED
import com.cycling.domain.repository.SettingsRepository.Companion.MIN_PLAYBACK_SPEED

/**
 * Pure conversions between DataStore's scalar values and domain types.
 *
 * Extracted so they can be unit-tested on the JVM: a DataStore-backed repository needs an Android
 * context, but the parsing is where values actually go wrong — a preference can outlive an app
 * downgrade, so every read has to clamp or fall back rather than trust the file.
 */
internal object SettingsConversions {

    inline fun <reified T : Enum<T>> parseEnum(raw: String?, fallback: T): T =
        raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

    fun parsePlaybackSpeed(raw: String?): Float =
        raw?.toFloatOrNull()?.coerceIn(MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED) ?: DEFAULT_PLAYBACK_SPEED

    /** Unknown names are dropped; an empty result falls back to the default order. */
    fun parseLyricsProviders(raw: String?): List<LyricsProvider> {
        val parsed = raw?.split(',')?.mapNotNull { name ->
            LyricsProvider.entries.firstOrNull { it.name == name.trim() }
        }.orEmpty()
        return parsed.ifEmpty { DEFAULT_LYRICS_PROVIDER_ORDER }
    }

    fun encodeLyricsProviders(providers: List<LyricsProvider>): String =
        providers.ifEmpty { DEFAULT_LYRICS_PROVIDER_ORDER }.joinToString(",") { it.name }
}
