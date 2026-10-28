package com.cycling.data.repository

import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.model.SortOrder
import com.cycling.domain.repository.SettingsRepository.Companion.DEFAULT_LYRICS_PROVIDER_ORDER
import com.cycling.domain.repository.SettingsRepository.Companion.DEFAULT_PLAYBACK_SPEED
import com.cycling.domain.repository.SettingsRepository.Companion.MAX_PLAYBACK_SPEED
import com.cycling.domain.repository.SettingsRepository.Companion.MIN_PLAYBACK_SPEED
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsConversionsTest {

    @Test
    fun `enum parsing falls back when the stored name is unknown or missing`() {
        assertEquals(SortOrder.TITLE, SettingsConversions.parseEnum(null, SortOrder.TITLE))
        assertEquals(SortOrder.TITLE, SettingsConversions.parseEnum("", SortOrder.TITLE))
        assertEquals(SortOrder.TITLE, SettingsConversions.parseEnum("SEPIA", SortOrder.TITLE))
        assertEquals(SortOrder.ARTIST, SettingsConversions.parseEnum("ARTIST", SortOrder.TITLE))
    }

    @Test
    fun `playback speed clamps to the supported range`() {
        assertEquals(MIN_PLAYBACK_SPEED, SettingsConversions.parsePlaybackSpeed("0.1"))
        assertEquals(MAX_PLAYBACK_SPEED, SettingsConversions.parsePlaybackSpeed("5.0"))
        assertEquals(1.5f, SettingsConversions.parsePlaybackSpeed("1.5"))
    }

    @Test
    fun `playback speed falls back on garbage`() {
        assertEquals(DEFAULT_PLAYBACK_SPEED, SettingsConversions.parsePlaybackSpeed(null))
        assertEquals(DEFAULT_PLAYBACK_SPEED, SettingsConversions.parsePlaybackSpeed("fast"))
        assertEquals(DEFAULT_PLAYBACK_SPEED, SettingsConversions.parsePlaybackSpeed(""))
    }

    @Test
    fun `lyrics provider order keeps the stored order and appends providers it predates`() {
        assertEquals(
            listOf(
                LyricsProvider.LRCLIB,
                LyricsProvider.NETEASE,
                LyricsProvider.AMLL,
                LyricsProvider.QQ_MUSIC,
                LyricsProvider.KUGOU,
                LyricsProvider.LX
            ),
            SettingsConversions.parseLyricsProviders("LRCLIB,NETEASE")
        )
    }

    @Test
    fun `lyrics provider parsing drops unknown names and never returns empty`() {
        // "SPOTIFY" is not a provider: the known name survives, the rest are appended after it.
        assertEquals(
            listOf(
                LyricsProvider.LRCLIB,
                LyricsProvider.AMLL,
                LyricsProvider.NETEASE,
                LyricsProvider.QQ_MUSIC,
                LyricsProvider.KUGOU,
                LyricsProvider.LX
            ),
            SettingsConversions.parseLyricsProviders("SPOTIFY,LRCLIB")
        )
        // An empty result would silently disable online lyrics, so the default takes over.
        assertEquals(DEFAULT_LYRICS_PROVIDER_ORDER, SettingsConversions.parseLyricsProviders(null))
        assertEquals(DEFAULT_LYRICS_PROVIDER_ORDER, SettingsConversions.parseLyricsProviders("NOPE"))
    }

    @Test
    fun `a provider repeated in the stored order is listed once`() {
        assertEquals(
            listOf(
                LyricsProvider.NETEASE,
                LyricsProvider.AMLL,
                LyricsProvider.QQ_MUSIC,
                LyricsProvider.KUGOU,
                LyricsProvider.LRCLIB,
                LyricsProvider.LX
            ),
            SettingsConversions.parseLyricsProviders("NETEASE,NETEASE")
        )
    }

    @Test
    fun `encoding an empty provider list writes the default rather than nothing`() {
        assertEquals(
            DEFAULT_LYRICS_PROVIDER_ORDER.joinToString(",") { it.name },
            SettingsConversions.encodeLyricsProviders(emptyList())
        )
    }
}
