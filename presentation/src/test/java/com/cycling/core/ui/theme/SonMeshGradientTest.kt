package com.cycling.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 动态色面的纯契约：色面不透明、循环无接缝、明暗两套可区分、
 * **色面保留的是封面上取到的色相**（而不是配色方案里被旋转过的 container 色相）。
 *
 * 这些断言就是 ADR-0005 的「色面颜色来自封面」与「循环没有接缝」两条规则，
 * 改成 shader 或换算法时先让它们继续绿。
 */
class SonMeshGradientTest {

    private val darkScheme: ColorScheme = sonColorSchemeOf(SonicSeed, darkTheme = true)
    private val lightScheme: ColorScheme = sonColorSchemeOf(SonicSeed, darkTheme = false)
    private val size = SonMeshGradient.RESOLUTION

    /** 封面上取到的三种颜色：红 / 绿 / 蓝，色相彼此拉开。 */
    private val coverColors = listOf(
        Color(0xFFD32F2F),
        Color(0xFF2E7D32),
        Color(0xFF1565C0)
    )

    private fun render(
        scheme: ColorScheme,
        dark: Boolean,
        phase: Float,
        colors: List<Color> = coverColors
    ): IntArray {
        val out = IntArray(size * size)
        SonMeshGradient.render(
            out = out,
            palette = SonMeshGradient.palette(colors, scheme.surface, dark),
            phase = phase
        )
        return out
    }

    @Test
    fun `palette gives every control point one opaque colour`() {
        val palette = SonMeshGradient.palette(coverColors, lightScheme.surface, dark = false)

        assertEquals(SonMeshGradient.GRID * SonMeshGradient.GRID, palette.size)
        assertTrue(palette.all { (it ushr 24) and 0xFF == 0xFF })
        assertEquals(3, palette.toSet().size)
    }

    @Test
    fun `palette keeps the cover hues instead of the scheme hues`() {
        val palette = SonMeshGradient.palette(coverColors, darkScheme.surface, dark = true)
            .map { Hct.fromColor(Color(it)).hue }

        coverColors.forEach { cover ->
            val coverHue = Hct.fromColor(cover).hue
            assertTrue(
                "cover hue $coverHue missing from the field",
                palette.any { hueDistance(it, coverHue) < HUE_TOLERANCE }
            )
        }
    }

    @Test
    fun `palette normalises lightness so no colour is lost in the field`() {
        // 纯黑封面颜色也必须得到可见的色面，而不是一块死黑
        val palette = SonMeshGradient.palette(
            colors = listOf(Color.Black, Color.Black, Color.Black),
            surface = darkScheme.surface,
            dark = true
        )

        assertTrue(palette.all { Hct.fromColor(Color(it)).tone > DARK_FIELD_MIN_TONE })
    }

    @Test
    fun `palette falls back to a single cover colour gracefully`() {
        val palette = SonMeshGradient.palette(listOf(Color(0xFFD32F2F)), lightScheme.surface, dark = false)

        assertEquals(SonMeshGradient.GRID * SonMeshGradient.GRID, palette.size)
        assertTrue(palette.all { (it ushr 24) and 0xFF == 0xFF })
    }

    @Test
    fun `every pixel is painted opaque`() {
        val pixels = render(darkScheme, dark = true, phase = 0.37f)

        assertTrue(pixels.all { (it ushr 24) and 0xFF == 0xFF })
    }

    @Test
    fun `the top-left pixel takes the top-left control point colour`() {
        val palette = SonMeshGradient.palette(coverColors, darkScheme.surface, dark = true)
        val first = render(darkScheme, dark = true, phase = 0f).first()

        assertEquals(palette[0], palette.toList().minBy { channelDistance(it, first) })
    }

    @Test
    fun `the loop has no seam at the end of a cycle`() {
        assertArrayEquals(
            render(lightScheme, dark = false, phase = 0f),
            render(lightScheme, dark = false, phase = 1f)
        )
        assertArrayEquals(
            render(lightScheme, dark = false, phase = 0.25f),
            render(lightScheme, dark = false, phase = 2.25f)
        )
    }

    @Test
    fun `the field moves between phases`() {
        assertFalse(
            render(darkScheme, dark = true, phase = 0f)
                .contentEquals(render(darkScheme, dark = true, phase = 0.5f))
        )
    }

    @Test
    fun `a light scheme paints a lighter field than a dark one`() {
        assertTrue(
            averageChannel(render(lightScheme, dark = false, phase = 0.2f)) >
                averageChannel(render(darkScheme, dark = true, phase = 0.2f))
        )
    }

    @Test
    fun `the field visibly moves within one second`() {
        // 真机回归：32s 周期 + 0.05 振幅时，播放中的色面 12 秒的像素差只有个位数，
        // 用户看到的就是「一张不动的背景」。一秒之内必须有能看出来的位移。
        val start = render(darkScheme, dark = true, phase = 0f)
        val oneSecondLater = render(
            darkScheme,
            dark = true,
            phase = 1_000f / SonMotionTiming.BACKDROP_CYCLE_MS.toFloat()
        )

        val maxDelta = start.indices.maxOf { channelDistance(start[it], oneSecondLater[it]) }
        assertTrue(
            "one second of flow must be visible, max channel delta was $maxDelta",
            maxDelta >= MIN_ONE_SECOND_DELTA
        )
    }

    private fun hueDistance(left: Double, right: Double): Double {
        val distance = kotlin.math.abs(left - right) % 360.0
        return if (distance > 180.0) 360.0 - distance else distance
    }

    private fun channelDistance(left: Int, right: Int): Int {
        var distance = 0
        for (shift in intArrayOf(16, 8, 0)) {
            distance += kotlin.math.abs(((left ushr shift) and 0xFF) - ((right ushr shift) and 0xFF))
        }
        return distance
    }

    private fun averageChannel(pixels: IntArray): Int =
        pixels.sumOf { pixel ->
            ((pixel ushr 16) and 0xFF) + ((pixel ushr 8) and 0xFF) + (pixel and 0xFF)
        } / pixels.size

    private companion object {
        /**
         * 混到 surface 上会轻微拉动色相，留 20° 余量；源色相彼此相差 100° 以上、
         * Expressive 的 secondary / tertiary 旋转是 45°~120°，所以这个余量仍然能抓住
         * 「色面用了被旋转过的色相」这个回归。
         */
        const val HUE_TOLERANCE = 20.0

        /** 暗色主题下色面最暗角色的归一明度是 30，混入 surface 后仍应高于 10。 */
        const val DARK_FIELD_MIN_TONE = 10.0

        /**
         * 一秒流动后至少要有多少像素差（R+G+B 三通道之和）。
         *
         * 真机实测的旧实现是「12 秒才 20/765」，所以 24 这条线钉住的是「一秒内已经能被看见」，
         * 而不是某个具体的美术效果；调慢周期或调小 `WARP` 会立刻让它变红。
         */
        const val MIN_ONE_SECOND_DELTA = 24
    }
}
