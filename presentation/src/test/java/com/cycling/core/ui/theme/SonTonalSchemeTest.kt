package com.cycling.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 近灰封面的色相兜底：种子 HCT chroma < 16 用品牌色相，>= 16 保留种子色相。
 * 阈值是唯一的常量，页面不分支。
 */
class SonTonalSchemeTest {

    private val brandHue = Hct.fromColor(SonicSeed).hue

    @Test
    fun `gray seed below the threshold falls back to the brand hue`() {
        assertEquals(brandHue, SonTonalScheme.resolveSeedHue(seedHue = 200.0, chroma = 15.999), 0.0001)
    }

    @Test
    fun `seed exactly at the threshold keeps its hue`() {
        assertEquals(200.0, SonTonalScheme.resolveSeedHue(seedHue = 200.0, chroma = 16.0), 0.0001)
    }

    @Test
    fun `chromatic seed above the threshold keeps its hue`() {
        assertEquals(200.0, SonTonalScheme.resolveSeedHue(seedHue = 200.0, chroma = 48.0), 0.0001)
    }

    @Test
    fun `grayscale seed resolves the brand hue through from`() {
        // Pure gray: HCT chroma is 0, so the hue is meaningless.
        val scheme = SonTonalScheme.from(Color(0xFF777777))
        assertEquals(brandHue, scheme.primary.hue, 0.0001)
    }

    @Test
    fun `chromatic seed preserves its own hue through from`() {
        val seed = Color(0xFF3366CC)
        val seedHue = Hct.fromColor(seed).hue
        val scheme = SonTonalScheme.from(seed)
        assertEquals(seedHue, scheme.primary.hue, 0.0001)
    }
}
