package com.cycling.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * 单一色相的色调板：固定 hue + chroma，沿 L* 明度轴取 0..100 的各级色阶。
 * 色调档位按 Material 惯例取整数，结果逐档缓存 —— 一个配色方案会反复取同一档。
 */
@Immutable
class SonTonalPalette internal constructor(
    val hue: Double,
    val chroma: Double
) {

    private val cache = HashMap<Int, Color>()

    /** 取指定 L* 明度的色阶（0..100）。 */
    fun tone(tone: Int): Color {
        val level = tone.coerceIn(0, 100)
        return cache.getOrPut(level) { Hct.from(hue, chroma, level.toDouble()).toColor() }
    }

    companion object {
        fun of(hue: Double, chroma: Double): SonTonalPalette =
            SonTonalPalette(((hue % 360.0) + 360.0) % 360.0, chroma.coerceAtLeast(0.0))

        fun fromColor(color: Color): SonTonalPalette {
            val hct = Hct.fromColor(color)
            return of(hct.hue, hct.chroma)
        }
    }
}

/** 色料方案（Color Scheme Variant）。Expressive 为 Material 3 Expressive 的高彩度方案。 */
enum class SonSchemeStyle {
    /** 高彩度、大色相偏移，体现 Expressive 的鲜明个性。 */
    Expressive,

    /** 经典 Tonal Spot：低彩度、邻近色，最稳妥。 */
    TonalSpot
}

/**
 * 由一颗种子色派生出的 5 条色调板，对应 Material 的 primary / secondary / tertiary /
 * neutral / neutralVariant 五个色相组。
 */
@Immutable
class SonTonalScheme internal constructor(
    val source: Color,
    val primary: SonTonalPalette,
    val secondary: SonTonalPalette,
    val tertiary: SonTonalPalette,
    val neutral: SonTonalPalette,
    val neutralVariant: SonTonalPalette
) {

    /** 暗色 Lightness 映射表 —— 与 Material 官方 dynamicDarkColorScheme 的档位一致。 */
    fun toDarkColorScheme(): ColorScheme = darkColorScheme(
        primary = primary.tone(80),
        onPrimary = primary.tone(20),
        primaryContainer = primary.tone(30),
        onPrimaryContainer = primary.tone(90),
        inversePrimary = primary.tone(40),
        secondary = secondary.tone(80),
        onSecondary = secondary.tone(20),
        secondaryContainer = secondary.tone(30),
        onSecondaryContainer = secondary.tone(90),
        tertiary = tertiary.tone(80),
        onTertiary = tertiary.tone(20),
        tertiaryContainer = tertiary.tone(30),
        onTertiaryContainer = tertiary.tone(90),
        background = neutralVariant.tone(6),
        onBackground = neutralVariant.tone(90),
        surface = neutralVariant.tone(6),
        onSurface = neutralVariant.tone(90),
        surfaceVariant = neutralVariant.tone(30),
        onSurfaceVariant = neutralVariant.tone(80),
        inverseSurface = neutralVariant.tone(90),
        inverseOnSurface = neutralVariant.tone(20),
        outline = neutralVariant.tone(60),
        outlineVariant = neutralVariant.tone(30),
        scrim = neutralVariant.tone(0),
        surfaceBright = neutralVariant.tone(24),
        surfaceDim = neutralVariant.tone(6),
        surfaceContainer = neutralVariant.tone(12),
        surfaceContainerHigh = neutralVariant.tone(17),
        surfaceContainerHighest = neutralVariant.tone(22),
        surfaceContainerLow = neutralVariant.tone(10),
        surfaceContainerLowest = neutralVariant.tone(4),
        surfaceTint = primary.tone(80),
        primaryFixed = primary.tone(90),
        primaryFixedDim = primary.tone(80),
        onPrimaryFixed = primary.tone(10),
        onPrimaryFixedVariant = primary.tone(30),
        secondaryFixed = secondary.tone(90),
        secondaryFixedDim = secondary.tone(80),
        onSecondaryFixed = secondary.tone(10),
        onSecondaryFixedVariant = secondary.tone(30),
        tertiaryFixed = tertiary.tone(90),
        tertiaryFixedDim = tertiary.tone(80),
        onTertiaryFixed = tertiary.tone(10),
        onTertiaryFixedVariant = tertiary.tone(30)
    )

    /** 亮色 Lightness 映射表。 */
    fun toLightColorScheme(): ColorScheme = lightColorScheme(
        primary = primary.tone(40),
        onPrimary = primary.tone(100),
        primaryContainer = primary.tone(90),
        onPrimaryContainer = primary.tone(10),
        inversePrimary = primary.tone(80),
        secondary = secondary.tone(40),
        onSecondary = secondary.tone(100),
        secondaryContainer = secondary.tone(90),
        onSecondaryContainer = secondary.tone(10),
        tertiary = tertiary.tone(40),
        onTertiary = tertiary.tone(100),
        tertiaryContainer = tertiary.tone(90),
        onTertiaryContainer = tertiary.tone(10),
        background = neutralVariant.tone(98),
        onBackground = neutralVariant.tone(10),
        surface = neutralVariant.tone(98),
        onSurface = neutralVariant.tone(10),
        surfaceVariant = neutralVariant.tone(90),
        onSurfaceVariant = neutralVariant.tone(30),
        inverseSurface = neutralVariant.tone(20),
        inverseOnSurface = neutralVariant.tone(95),
        outline = neutralVariant.tone(50),
        outlineVariant = neutralVariant.tone(80),
        scrim = neutralVariant.tone(0),
        surfaceBright = neutralVariant.tone(98),
        surfaceDim = neutralVariant.tone(87),
        surfaceContainer = neutralVariant.tone(94),
        surfaceContainerHigh = neutralVariant.tone(92),
        surfaceContainerHighest = neutralVariant.tone(90),
        surfaceContainerLow = neutralVariant.tone(96),
        surfaceContainerLowest = neutralVariant.tone(100),
        surfaceTint = primary.tone(40),
        primaryFixed = primary.tone(90),
        primaryFixedDim = primary.tone(80),
        onPrimaryFixed = primary.tone(10),
        onPrimaryFixedVariant = primary.tone(30),
        secondaryFixed = secondary.tone(90),
        secondaryFixedDim = secondary.tone(80),
        onSecondaryFixed = secondary.tone(10),
        onSecondaryFixedVariant = secondary.tone(30),
        tertiaryFixed = tertiary.tone(90),
        tertiaryFixedDim = tertiary.tone(80),
        onTertiaryFixed = tertiary.tone(10),
        onTertiaryFixedVariant = tertiary.tone(30)
    )

    companion object {

        // Expressive 方案的色相偏移表（对齐 Material Color Utilities SchemeExpressive）
        private val EXPRESSIVE_HUES = doubleArrayOf(0.0, 21.0, 51.0, 121.0, 151.0, 191.0, 271.0, 321.0, 351.0, 381.0)
        private val EXPRESSIVE_SECONDARY_ROTATIONS = doubleArrayOf(45.0, 95.0, 45.0, 20.0, 45.0, 90.0, 45.0, 45.0, 45.0, 45.0)
        private val EXPRESSIVE_TERTIARY_ROTATIONS = doubleArrayOf(120.0, 120.0, 20.0, 45.0, 20.0, 15.0, 20.0, 120.0, 20.0, 120.0)

        fun from(color: Color, style: SonSchemeStyle = SonSchemeStyle.Expressive): SonTonalScheme {
            val hct = Hct.fromColor(color)
            // 种子接近无彩色时色相不稳定，退回品牌色相，避免整库变灰
            val hue = if (hct.chroma < Hct.ACHROMATIC_CHROMA * 5) Hct.fromColor(SonicSeed).hue else hct.hue
            return when (style) {
                SonSchemeStyle.Expressive -> expressive(hue)
                SonSchemeStyle.TonalSpot -> tonalSpot(hue)
            }
        }

        private fun expressive(hue: Double): SonTonalScheme {
            val secondaryHue = sanitize(hue + rotationFor(hue, EXPRESSIVE_SECONDARY_ROTATIONS))
            val tertiaryHue = sanitize(hue + rotationFor(hue, EXPRESSIVE_TERTIARY_ROTATIONS))
            val neutralHue = sanitize(hue + 15.0)
            return SonTonalScheme(
                source = Hct.from(hue, 48.0, 50.0).toColor(),
                primary = SonTonalPalette.of(hue, 48.0),
                secondary = SonTonalPalette.of(secondaryHue, 16.0),
                tertiary = SonTonalPalette.of(tertiaryHue, 32.0),
                neutral = SonTonalPalette.of(neutralHue, 8.0),
                neutralVariant = SonTonalPalette.of(neutralHue, 12.0)
            )
        }

        private fun tonalSpot(hue: Double): SonTonalScheme = SonTonalScheme(
            source = Hct.from(hue, 36.0, 50.0).toColor(),
            primary = SonTonalPalette.of(hue, 36.0),
            secondary = SonTonalPalette.of(hue, 16.0),
            tertiary = SonTonalPalette.of(sanitize(hue + 60.0), 24.0),
            neutral = SonTonalPalette.of(hue, 6.0),
            neutralVariant = SonTonalPalette.of(hue, 8.0)
        )

        /** 取距种子色相最近的档位，决定该用哪一份色相偏移。 */
        private fun rotationFor(hue: Double, rotations: DoubleArray): Double {
            var best = 0
            var bestDistance = abs(hue - EXPRESSIVE_HUES[0])
            for (i in 1 until EXPRESSIVE_HUES.size) {
                val distance = abs(hue - EXPRESSIVE_HUES[i])
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = i
                }
            }
            return rotations[best]
        }

        private fun sanitize(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0
    }
}
