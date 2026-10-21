package com.cycling.domain.online

import com.cycling.domain.online.model.MusicQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicQualityTest {

    @Test
    fun `ladder starts at the preferred quality when the plugin supports it`() {
        val available = listOf("128k", "320k", "flac", "hires")

        val ladder = MusicQuality.ladder(available, MusicQuality.LOSSLESS)

        // 从偏好档位向下降级，不会升级到 hires（那是更高的档位）。
        assertEquals(listOf("flac", "320k", "128k"), ladder)
    }

    @Test
    fun `ladder degrades when the preferred quality is unavailable`() {
        val available = listOf("128k", "320k")

        val ladder = MusicQuality.ladder(available, MusicQuality.MASTER)

        assertEquals(listOf("320k", "128k"), ladder)
    }

    @Test
    fun `ladder never mixes plugin only qualities into official degradation`() {
        val available = listOf("128k", "320k", "master")

        val ladder = MusicQuality.ladder(
            available = available,
            preferred = MusicQuality.MASTER,
            pluginOnlyPreferred = true
        )

        assertEquals(listOf("master"), ladder)
    }

    @Test
    fun `pickLxQuality falls back to the highest available below the preference`() {
        assertEquals("320k", MusicQuality.pickLxQuality(listOf("128k", "320k"), MusicQuality.LOSSLESS))
        // 插件没声明档位时按偏好原样请求（和原实现一致：不做无依据的降级）。
        assertEquals("flac", MusicQuality.pickLxQuality(emptyList(), MusicQuality.LOSSLESS))
    }

    @Test
    fun `plugin only qualities are flagged`() {
        assertTrue(MusicQuality.MASTER.pluginOnly)
        assertTrue(MusicQuality.ATMOS_PLUS.pluginOnly)
        assertFalse(MusicQuality.LOSSLESS.pluginOnly)
        assertFalse(MusicQuality.STANDARD.pluginOnly)
    }

    @Test
    fun `fromLxKey maps script quality keys`() {
        assertEquals(MusicQuality.EXHIGH, MusicQuality.fromLxKey("320k"))
        assertEquals(MusicQuality.FLAC24BIT, MusicQuality.fromLxKey("flac24bit"))
        assertEquals(null, MusicQuality.fromLxKey("unknown"))
    }
}
