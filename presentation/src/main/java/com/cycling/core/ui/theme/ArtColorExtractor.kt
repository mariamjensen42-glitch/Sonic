package com.cycling.core.ui.theme

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 封面取色结果：一颗种子色 + 一组动态色面用的封面颜色。
 *
 * 两者的用途不同，所以取法也不同：
 * - [seed] 只取一颗 —— 它要展开成整套 M3 配色，原始颜色无法保证对比度与色阶关系；
 * - [backdrop] 取的是**封面上真实存在**的几种颜色（色相彼此拉开），只用于动态色面。
 *   不能用配色方案里的 container 角色代替：`SonTonalScheme.Expressive` 会把
 *   secondary / tertiary 的色相旋转 45°~120°，那两档颜色不在封面上，铺成背景就会「不像封面」。
 */
@Immutable
data class SonArtworkColors(
    val seed: Color,
    /** 1~3 个封面颜色，色相尽量分散；没有可用 swatch 时为空，此时色面回退到种子色。 */
    val backdrop: List<Color>
)

/**
 * 封面取色，全部交给 `androidx.palette` 判断「哪些颜色是封面的主色/鲜明色/柔和色」。
 *
 * 这里刻意**不再自己设彩度门槛**：Palette 的 vibrant / muted / dominant 已经是对整张图
 * 统计后的结论，再拿 HCT 彩度筛一遍只会把淡封面（奶油、灰蓝、莫兰迪色）筛空 —— 筛空之后
 * 色面只能退回配色方案里的 container，那正是「背景是品牌紫而不是封面色」的来源。
 * 彩度不足由色面渲染统一抬高（见 `SonMeshGradient.palette` 的 MIN_ROLE_CHROMA），
 * 所以这里只要保证色相来自封面、彼此不重复。
 */
object ArtColorExtractor {

    /** 种子色最低彩度：低于它的封面接近灰阶，展开成配色前先把彩度抬到可用区间。 */
    private const val MIN_SEED_CHROMA = 12.0

    /** 两个色相至少差这么多度才算「封面上的另一种颜色」。 */
    private const val MIN_HUE_SEPARATION = 32.0

    /** 同色相补位时的最小明度差：铺到色面上要能看出是一深一浅，而不是重复色。 */
    private const val MIN_TONE_SEPARATION = 6.0

    /** 动态色面最多用几种封面颜色。与 `SonMeshGradient` 的三个角色对应。 */
    private const val BACKDROP_COLOR_COUNT = 3

    /** Palette 的量化档数：比默认的 16 多一点，色相才有得挑。 */
    private const val PALETTE_COLOR_COUNT = 24

    private const val COLOR_OPAQUE = 0xFF shl 24

    /** 一次 Palette 分析同时得到种子色与动态色面颜色，避免为同一张封面跑两遍。 */
    suspend fun colorsFromBitmap(bitmap: Bitmap): SonArtworkColors? =
        withContext(Dispatchers.Default) {
            val palette = Palette.from(bitmap)
                .maximumColorCount(PALETTE_COLOR_COUNT)
                .generate()

            val seed = seedOf(palette) ?: return@withContext null
            SonArtworkColors(seed = seed, backdrop = backdropOf(palette))
        }

    /** 种子色按 CAM16 彩度挑最「有颜色」的那一档，保证展开出的方案有性格。 */
    private fun seedOf(palette: Palette): Color? {
        val candidates = listOfNotNull(
            palette.vibrantSwatch,
            palette.lightVibrantSwatch,
            palette.darkVibrantSwatch,
            palette.mutedSwatch,
            palette.lightMutedSwatch,
            palette.darkMutedSwatch,
            palette.dominantSwatch
        )
        val seed = candidates
            .map { Color(it.rgb or COLOR_OPAQUE) }
            .maxByOrNull { Hct.fromColor(it).chroma }
            ?: return null
        return seed.saturatedForScheme()
    }

    /**
     * 封面上实际存在的几种颜色：先取 Palette 给出的档位顺序，再挑色相彼此拉开的几档，
     * 单色封面用同色相不同明度的 swatch 补足。
     */
    private fun backdropOf(palette: Palette): List<Color> {
        val candidates = listOfNotNull(
            palette.vibrantSwatch,
            palette.dominantSwatch,
            palette.mutedSwatch,
            palette.darkVibrantSwatch,
            palette.lightVibrantSwatch,
            palette.lightMutedSwatch,
            palette.darkMutedSwatch
        ).map { Hct.fromColor(Color(it.rgb or COLOR_OPAQUE)) }
            .distinctBy { it.hue.roundToInt() to it.tone.roundToInt() }
        if (candidates.isEmpty()) return emptyList()

        val picked = mutableListOf<Hct>()
        for (hct in candidates) {
            if (picked.size == BACKDROP_COLOR_COUNT) break
            if (picked.all { hueDistance(it.hue, hct.hue) >= MIN_HUE_SEPARATION }) picked += hct
        }
        // 色相被占满的单色封面：用同色相、明度差得开的档位补足，避免色面只剩一个角色
        for (hct in candidates) {
            if (picked.size == BACKDROP_COLOR_COUNT) break
            if (picked.none { abs(it.tone - hct.tone) < MIN_TONE_SEPARATION }) picked += hct
        }

        return picked.map { it.toColor() }
    }

    /**
     * 把种子色整理成「能撑起一套配色」的状态。
     *
     * 两件事：彩度抬到 [MIN_SEED_CHROMA]，以及**彩度低于灰封面阈值时把色相换成品牌色相** ——
     * 纯灰 / 纯白封面的 CAM16 hue 只是噪声，抬了彩度就会冒出一个跟封面无关的颜色。
     * 阈值与 `SonTonalScheme` 共用同一个常量，所以整理过的种子一定会保留（或被明确换成）方案色相。
     */
    private fun Color.saturatedForScheme(): Color {
        val hct = Hct.fromColor(this)
        val hue = if (hct.chroma < SonTonalScheme.GRAYSCALE_CHROMA_THRESHOLD) {
            Hct.fromColor(SonicSeed).hue
        } else {
            hct.hue
        }
        val chroma = hct.chroma.coerceAtLeast(MIN_SEED_CHROMA)
        if (hue == hct.hue && chroma == hct.chroma) return this
        return Hct.from(hue, chroma, hct.tone).toColor()
    }

    /** 色相是环形的，0° 与 350° 只差 10°。 */
    private fun hueDistance(left: Double, right: Double): Double {
        val distance = abs(left - right) % 360.0
        return if (distance > 180.0) 360.0 - distance else distance
    }
}
