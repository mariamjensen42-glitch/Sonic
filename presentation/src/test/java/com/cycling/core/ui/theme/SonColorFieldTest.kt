package com.cycling.core.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 色面规格的纯契约：两层渐变的 stop / alpha、hero 下沿归零、只有 primaryContainer + surface。
 * 这些数字是主题层唯一来源，页面不再拼 Brush。
 */
class SonColorFieldTest {

    @Test
    fun `light field uses the theme radial and vertical stops`() {
        val spec = SonColorField.spec(dark = false)

        assertEquals(listOf(0f, 0.55f, 0.60f), spec.radial.stops)
        assertEquals(listOf(0.70f, 0.34f, 0f), spec.radial.alphas)
        assertEquals(listOf(0f, 0.24f, 0.46f, 0.53f), spec.vertical.stops)
        assertEquals(listOf(0.18f, 0.46f, 0.16f, 0f), spec.vertical.alphas)
    }

    @Test
    fun `dark field uses the theme radial and vertical stops`() {
        val spec = SonColorField.spec(dark = true)

        assertEquals(listOf(0f, 0.55f, 0.60f), spec.radial.stops)
        assertEquals(listOf(0.62f, 0.30f, 0f), spec.radial.alphas)
        assertEquals(listOf(0f, 0.24f, 0.46f, 0.53f), spec.vertical.stops)
        assertEquals(listOf(0.12f, 0.30f, 0.10f, 0f), spec.vertical.alphas)
    }

    @Test
    fun `field is zero at the hero bottom in both themes`() {
        listOf(false, true).forEach { dark ->
            val spec = SonColorField.spec(dark = dark)

            assertEquals(0f, spec.vertical.alphaAt(SonColorField.HeroBottomFraction), 0f)
            assertEquals(0f, spec.radial.alphaAt(0.60f), 0f)

            // 径向椭圆在 hero 下沿正好走到最后一个 stop（60%），两层同时归零。
            val radialProgressAtHeroBottom =
                (SonColorField.HeroBottomFraction - SonColorField.RadialCenterY) /
                    SonColorField.RadialRadiusY
            assertEquals(0.60f, radialProgressAtHeroBottom, 0.0001f)
        }
    }

    @Test
    fun `field only uses primary container and surface roles`() {
        val spec = SonColorField.spec(dark = false)

        assertEquals(SonColorFieldRole.PrimaryContainer, spec.tintRole)
        assertEquals(SonColorFieldRole.Surface, spec.surfaceRole)
    }

    @Test
    fun `hero bottom end has no tail`() {
        val spec = SonColorField.spec(dark = false, end = SonColorFieldEnd.HeroBottom)

        assertNull(spec.tail)
        assertEquals(0f, spec.tail?.alphaAt(0f) ?: 0f, 0f)
    }

    @Test
    fun `detail tail uses the accepted vertical peak and ends within twelve dp`() {
        val spec = SonColorField.spec(dark = false, end = SonColorFieldEnd.Detail)

        assertEquals(12.dp, SonColorField.DetailTail)
        assertNotNull(spec.tail)
        assertEquals(0.46f, spec.tail?.alphaAt(0f) ?: -1f, 0f)
        assertEquals(0f, spec.tail?.alphaAt(1f) ?: -1f, 0f)

        // 首行歌曲位于 tail 之后：垂直层在 hero 下沿已经归零，不会再往下染色。
        assertEquals(0f, spec.vertical.alphaAt(SonColorField.HeroBottomFraction), 0f)
    }

    @Test
    fun `dark detail tail is dimmer than light tail`() {
        val light = SonColorField.spec(dark = false, end = SonColorFieldEnd.Detail)
        val dark = SonColorField.spec(dark = true, end = SonColorFieldEnd.Detail)

        assertEquals(0.30f, dark.tail?.alphaAt(0f) ?: -1f, 0f)
        assertTrue((light.tail?.alphaAt(0f) ?: 0f) > (dark.tail?.alphaAt(0f) ?: 0f))
    }
}
