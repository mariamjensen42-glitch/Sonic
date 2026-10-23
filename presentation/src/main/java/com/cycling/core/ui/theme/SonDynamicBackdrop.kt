package com.cycling.core.ui.theme

import android.animation.ValueAnimator
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 动态色面（Apple Music / AMLL 风格）的纯计算部分，不含 Compose 运行时。
 *
 * 这是 [SonDynamicBackdrop] 的单元测试接缝：控制点颜色、域扭曲、双线性插值全在这里，
 * 因此可以在 JVM 上逐像素断言，而不必起一个真机去截图。
 *
 * 渲染方式是「网格渐变」而不是 shader、模糊或 RenderEffect：
 * 4×4 个控制点各拿一个封面上取到的颜色，先按 [ROLE_LAYOUT] 排布，再在参数空间做双线性插值，
 * 叠加两层随 `phase` 流动的域扭曲与一层整体漂移，最后输出 [RESOLUTION]² 像素的小位图由 GPU 放大。
 * 选择这条路线的三个原因：
 * 1. minSdk 30 没有 AGSL（`RuntimeShader` 要 33+），而位图放大是双线性，任何版本一致；
 * 2. 每帧只算 56×56 个像素（约 3k 次乘法），比常驻的 shader / 模糊便宜得多；
 * 3. 纯函数 → 可以像 [SonColorField] 一样用单测钉住「不透明、无接缝、明暗分离、动得出来」。
 */
internal object SonMeshGradient {

    /** 控制点每边的数量。4×4 = 16 个；再细在这个模糊尺度上肉眼看不出来，只是多算。 */
    const val GRID = 4

    /** 离屏画布边长（像素）。放大到全屏由 GPU 双线性完成，56 已经足够平滑。 */
    const val RESOLUTION = 56

    /**
     * 三种 container 角色在控制点上的排布（行优先）。
     *
     * 主色出现最多，因为它是封面身份；次色与第三色交错填进来，色场才是编织感，
     * 而不是三条横向色带。
     */
    private val ROLE_LAYOUT = intArrayOf(
        0, 1, 2, 0,
        2, 0, 1, 2,
        1, 2, 0, 1,
        0, 1, 2, 0
    )

    /**
     * 三种色面角色（[ROLE_LAYOUT] 里的 0 / 1 / 2）的目标明度。
     *
     * 封面上取到的颜色明度可能从纯黑到纯白都有，直接铺开会出现一块死黑、一块刺眼。
     * 这里只保留它的**色相**，把明度归一到这几档 —— Apple Music / AMLL 的背景也是这么做的
     * （它们那条 pipeline 是 saturate ×3 + brightness ×0.75，等于把颜色压到同一亮度带）。
     */
    private val DARK_TONES = doubleArrayOf(38.0, 30.0, 44.0)
    private val LIGHT_TONES = doubleArrayOf(86.0, 82.0, 90.0)

    /** 色面角色的彩度区间：太低看不出是封面颜色，太高在色域边缘会被压掉。 */
    private const val MIN_ROLE_CHROMA = 20.0
    private const val MAX_ROLE_CHROMA = 56.0

    /**
     * 各角色相对 surface 的混合比例，亮暗分开。
     *
     * 暗色 surface 是 tone 6、亮色是 tone 98，同一档色面在两种主题下的观感差很多，
     * 所以亮色的混合比例明显更高。这与 [SonColorField]「亮色 alpha 必须高于暗色」同一条规则。
     * 数值偏浓：这一层就是这两页的 expressive moment，太淡会看不出是封面的颜色。
     */
    private val DARK_MIX = floatArrayOf(0.72f, 0.56f, 0.64f)
    private val LIGHT_MIX = floatArrayOf(0.78f, 0.58f, 0.70f)

    /**
     * 封面采样路线的明度带与混合比例。
     *
     * 与 swatch 路线的差别：那边的控制点按「角色」取固定明度（30/38/44），这边每个格子保留
     * 自己相对的明暗，只把整体压进这一带 —— 压得够窄才保证白字对比度，又不至于变成一块死色。
     */
    private const val DARK_ART_TONE_MIN = 24.0
    private const val DARK_ART_TONE_MAX = 50.0
    private const val LIGHT_ART_TONE_MIN = 76.0
    private const val LIGHT_ART_TONE_MAX = 92.0

    /** 封面采样路线的彩度区间：下限比 swatch 路线低，淡封面才不会糊成灰。 */
    private const val ART_MIN_CHROMA = 14.0
    private const val ART_MAX_CHROMA = 52.0

    private const val DARK_ART_MIX = 0.66f
    private const val LIGHT_ART_MIX = 0.72f

    /**
     * 域扭曲的主振幅（参数空间，1 等于整个画面宽度）。
     *
     * 这个值直接决定「动不动得起来」：0.05 时一秒内采样点只移动不到半个纹素，色面看起来是
     * 静止的（真机上实测 12 秒的像素差只有个位数）—— 至少 0.2，才能让 4 格网格里的颜色在
     * 几秒内明显换位。
     */
    private const val WARP = 0.22f

    /** 第二层、频率更高的扭曲：只有一层的话整块色面会像在呼吸，两层才像在流。 */
    private const val WARP_DETAIL = 0.08f

    /**
     * 整体漂移振幅：让整个色场缓慢平移，而不是每个控制点各自原地晃。
     *
     * 有它色面才像「在流动」而不是「在呼吸」。乘以边缘衰减，最外圈不动，不会在屏幕边缘
     * 露出被 clamp 死的色带。
     */
    private const val DRIFT = 0.05f

    /** 边上的扭曲衰减宽度：控制点在最外圈不动，画面边缘不会露出底色或来回抖。 */
    private const val EDGE_FALLOFF = 0.18f

    /** surface 亮度低于它的算暗色主题（不读 `isSystemInDarkTheme`：封面主题也可能显式给出）。 */
    const val DARK_SURFACE_LUMINANCE = 0.5f

    private val TWO_PI = 2.0 * PI

    /**
     * 16 个控制点的颜色（不透明 ARGB）。
     *
     * [colors] 是封面上取到的真实颜色（见 `ArtColorExtractor.backdropOf`），数量 1~3：
     * 每个角色按 `role % colors.size` 取色，所以单色封面会得到同色相不同明度的色面。
     * 每档先按 [DARK_TONES] / [LIGHT_TONES] 归一明度、把彩度夹进可用区间，再混到 surface 上 ——
     * 得到的是不透明底色，因此压在它上面的文字对比度是可算的。
     */
    fun palette(colors: List<Color>, surface: Color, dark: Boolean): IntArray {
        require(colors.isNotEmpty()) { "backdrop needs at least one cover colour" }
        val mix = if (dark) DARK_MIX else LIGHT_MIX
        val tones = if (dark) DARK_TONES else LIGHT_TONES
        return IntArray(GRID * GRID) { index ->
            val role = ROLE_LAYOUT[index]
            val coverColor = colors[role % colors.size]
            val hct = Hct.fromColor(coverColor)
            val tint = Hct.from(
                hue = hct.hue,
                chroma = hct.chroma.coerceIn(MIN_ROLE_CHROMA, MAX_ROLE_CHROMA),
                tone = tones[role]
            ).toColor()
            tint.blendedOver(surface, mix[role]).toArgb()
        }
    }

    /**
     * 用封面的 4×4 采样直接当控制点。
     *
     * 与 [palette] 的区别只在控制点的来源：那边是 Palette 挑出的 2~3 个 swatch 按角色重复，
     * 得到的是「封面颜色的色相」；这里是封面每个格子的平均色，**保留了封面的明暗与构图**，
     * 所以糊开之后还能认出是那张图 —— 也就是 Apple Music / MeloX 那种背景的观感。
     *
     * 明度仍然是夹到可读区间（暗色 24~50 / 亮色 76~92）而不是照搬：封面上纯白或纯黑的区域
     * 直接铺开会让白字读不出来。彩度也留了下限，淡封面才不至于糊成灰。
     *
     * @return 控制点颜色；[cells] 不是 [GRID]×[GRID] 时返回 null，调用方退回 [palette]。
     */
    fun paletteFromArtwork(cells: List<Color>, surface: Color, dark: Boolean): IntArray? {
        if (cells.size != GRID * GRID) return null
        val toneMin = if (dark) DARK_ART_TONE_MIN else LIGHT_ART_TONE_MIN
        val toneMax = if (dark) DARK_ART_TONE_MAX else LIGHT_ART_TONE_MAX
        val mix = if (dark) DARK_ART_MIX else LIGHT_ART_MIX
        return IntArray(GRID * GRID) { index ->
            val hct = Hct.fromColor(cells[index])
            val tint = Hct.from(
                hue = hct.hue,
                chroma = hct.chroma.coerceIn(ART_MIN_CHROMA, ART_MAX_CHROMA),
                tone = toneMin + (hct.tone / 100.0).coerceIn(0.0, 1.0) * (toneMax - toneMin)
            ).toColor()
            tint.blendedOver(surface, mix).toArgb()
        }
    }

    /**
     * 把色场写进 [out]（ARGB、行优先、边长 [resolution]）。
     *
     * [phase] 以一个完整周期为 1，取模后驱动扭曲，因此 0 与 1 是同一帧 —— 循环播放没有接缝。
     */
    fun render(
        out: IntArray,
        palette: IntArray,
        phase: Float,
        resolution: Int = RESOLUTION
    ) {
        require(palette.size == GRID * GRID) {
            "palette must describe the ${GRID}x$GRID control grid"
        }
        require(resolution >= 2) { "resolution must be at least 2" }
        require(out.size >= resolution * resolution) {
            "out must hold ${resolution}x$resolution pixels"
        }

        val cycle = ((phase % 1f + 1f) % 1f) * TWO_PI.toFloat()
        // 整体漂移每帧是常数，提到循环外面
        val driftU = DRIFT * sin(cycle)
        val driftV = DRIFT * cos(cycle)
        val last = GRID - 1
        for (y in 0 until resolution) {
            val v = (y + 0.5f) / resolution
            for (x in 0 until resolution) {
                val u = (x + 0.5f) / resolution
                val edge = smoothstep(0f, EDGE_FALLOFF, minOf(minOf(u, 1f - u), minOf(v, 1f - v)))
                val warpedU = u + edge * (
                    driftU +
                        WARP * sin(cycle + v * 2.3f) +
                        WARP_DETAIL * sin(2f * cycle - u * 3.1f)
                    )
                val warpedV = v + edge * (
                    driftV +
                        WARP * cos(cycle + u * 2.1f) +
                        WARP_DETAIL * cos(2f * cycle - v * 2.9f)
                    )

                val gx = (warpedU * last).coerceIn(0f, last.toFloat())
                val gy = (warpedV * last).coerceIn(0f, last.toFloat())
                val ix = floor(gx).toInt().coerceIn(0, last - 1)
                val iy = floor(gy).toInt().coerceIn(0, last - 1)
                val fx = smoothstep(0f, 1f, gx - ix)
                val fy = smoothstep(0f, 1f, gy - iy)

                out[y * resolution + x] = interpolate(
                    c00 = palette[iy * GRID + ix],
                    c10 = palette[iy * GRID + ix + 1],
                    c01 = palette[(iy + 1) * GRID + ix],
                    c11 = palette[(iy + 1) * GRID + ix + 1],
                    fx = fx,
                    fy = fy
                )
            }
        }
    }

    private fun interpolate(
        c00: Int,
        c10: Int,
        c01: Int,
        c11: Int,
        fx: Float,
        fy: Float
    ): Int {
        val oneMinusFx = 1f - fx
        val oneMinusFy = 1f - fy
        val w00 = oneMinusFx * oneMinusFy
        val w10 = fx * oneMinusFy
        val w01 = oneMinusFx * fy
        val w11 = fx * fy
        return (0xFF shl 24) or
            (blendChannel(c00, c10, c01, c11, w00, w10, w01, w11, shift = 16) shl 16) or
            (blendChannel(c00, c10, c01, c11, w00, w10, w01, w11, shift = 8) shl 8) or
            blendChannel(c00, c10, c01, c11, w00, w10, w01, w11, shift = 0)
    }

    private fun blendChannel(
        c00: Int,
        c10: Int,
        c01: Int,
        c11: Int,
        w00: Float,
        w10: Float,
        w01: Float,
        w11: Float,
        shift: Int
    ): Int {
        val value = ((c00 ushr shift) and 0xFF) * w00 +
            ((c10 ushr shift) and 0xFF) * w10 +
            ((c01 ushr shift) and 0xFF) * w01 +
            ((c11 ushr shift) and 0xFF) * w11
        return value.roundToInt().coerceIn(0, 255)
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /**
     * 按 sRGB 通道逐通道混色（与 `Color.compositeOver` 同一套算法，只是不构造 `Color`）。
     *
     * 色面每首歌只算一次，这里写成 `Color` 运算更好读；[render] 才是每帧的路径。
     */
    private fun Color.blendedOver(background: Color, alpha: Float): Color = Color(
        red = red * alpha + background.red * (1f - alpha),
        green = green * alpha + background.green * (1f - alpha),
        blue = blue * alpha + background.blue * (1f - alpha)
    )
}

/**
 * 动态色面在页面上的收束方式。
 *
 * 播放页与歌词页对「安静」的要求不同：播放页的运输控件必须落在干净 surface 上（沿用
 * ADR-0004 的 hero 下沿），歌词页是阅读面，整屏铺开、只压低强度。
 */
sealed interface SonBackdropFade {

    /** 铺满整屏，不向 surface 收（歌词页）。 */
    data object None : SonBackdropFade

    /**
     * 从 [start] 开始向 surface 收，到 [end] 完全收干净（播放页）。
     * 两个值都是屏幕高度的比例，`0f..1f`。
     */
    data class Bottom(val start: Float, val end: Float) : SonBackdropFade
}

/**
 * 动态色面的几何规则。
 *
 * 时序 token 在 [SonMotionTiming] 里登记（`docs/design/motion.md` 的「时序例外」），
 * 这里只放页面侧需要知道的规则。
 */
object SonBackdrop {

    /** 播放页的色面收束：hero 下沿之前开始收，运输控件所在的下半屏回到 surface。 */
    fun heroFade(heroBottom: Dp, screenHeight: Dp): SonBackdropFade.Bottom {
        val hero = if (screenHeight.value > 0f) {
            (heroBottom / screenHeight).coerceIn(0f, 1f)
        } else {
            HERO_FALLBACK_FRACTION
        }
        val start = (hero - 0.04f).coerceIn(0.20f, 0.72f)
        val end = (hero + 0.26f).coerceIn(start + 0.15f, 0.94f)
        return SonBackdropFade.Bottom(start = start, end = end)
    }

    private const val HERO_FALLBACK_FRACTION = 0.55f
}

/**
 * 封面派生的动态色面：一个缓慢流动的网格渐变，铺在播放页与歌词页的内容之下。
 *
 * 颜色来自**封面上真实取到的几个颜色**（`ArtColorExtractor` 直接用 `androidx.palette` 的
 * vibrant / muted / dominant 档位按色相挑 2~3 个，经 `LocalSonBackdropColors` 注入），
 * 这里只保留色相、把明度归一到固定档位、把彩度夹进 [MIN_ROLE_CHROMA]..[MAX_ROLE_CHROMA] ——
 * 淡封面（奶油色、灰蓝、莫兰迪色）的 swatch 彩度本来就低，抬高之后才看得出来是封面的颜色。
 * 这不等于配色方案里的 container 角色 —— `SonTonalScheme.Expressive` 会把 secondary /
 * tertiary 的色相旋转 45°~120°，那两档颜色根本不在封面上，铺成背景就会「不像封面」。
 * 没有封面时退回配色方案里的 container 角色。
 *
 * 绘制只用渐变与位图放大，**不使用 blur / RenderEffect / 大面积 graphicsLayer 模糊**
 * （ADR-0004 的硬规则）。
 *
 * 两条硬约束：
 * - **播放暂停就冻结**：不再请求帧，`dumpsys gfxinfo` 在暂停时应为 0 帧/秒；
 * - **系统关闭动画**（开发者选项或无障碍）时停在第一帧，不做常驻动画。
 *
 * @param playing 跟随播放状态；暂停时保持当前帧不动。
 * @param fade 向 surface 收束的方式，见 [SonBackdropFade]。
 * @param dim 整屏压暗的比例：歌词这类阅读面用一点，让文字保持安静。
 */
@Composable
fun SonDynamicBackdrop(
    playing: Boolean,
    modifier: Modifier = Modifier,
    fade: SonBackdropFade = SonBackdropFade.None,
    dim: Float = 0f
) {
    val colorScheme = MaterialTheme.colorScheme
    val palette = rememberBackdropPalette(colorScheme, LocalSonBackdropColors.current)
    val state = rememberBackdropState()
    rememberBackdropFlow(playing, state)
    Canvas(modifier) {
        drawBackdrop(state, palette, colorScheme.surface, fade, dim)
    }
}

/**
 * 与 [SonDynamicBackdrop] 同一套绘制，但画在调用方自己的 `drawBehind` 里。
 *
 * 给「整页背景、但内容层需要自己控制状态栏留白」的页面用（歌词页）：把这一层挂在页面根
 * `Modifier` 上，色面就铺在 padding 之外，而内容仍然在自己的留白里。
 */
@Composable
fun Modifier.sonDynamicBackdrop(
    playing: Boolean,
    fade: SonBackdropFade = SonBackdropFade.None,
    dim: Float = 0f
): Modifier {
    val colorScheme = MaterialTheme.colorScheme
    val palette = rememberBackdropPalette(colorScheme, LocalSonBackdropColors.current)
    val state = rememberBackdropState()
    rememberBackdropFlow(playing, state)
    return drawBehind {
        drawBackdrop(state, palette, colorScheme.surface, fade, dim)
    }
}

/** 每帧被覆写的小位图与当前相位。相位是快照状态：只有绘制阶段读它，因此只触发重绘。 */
private class BackdropState(
    val pixels: IntArray,
    val bitmap: Bitmap,
    val image: ImageBitmap
) {
    var phase by mutableFloatStateOf(0f)
}

@Composable
private fun rememberBackdropState(): BackdropState = remember {
    val size = SonMeshGradient.RESOLUTION
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    BackdropState(
        pixels = IntArray(size * size),
        bitmap = bitmap,
        image = bitmap.asImageBitmap()
    )
}

@Composable
private fun rememberBackdropPalette(colorScheme: ColorScheme, backdrop: List<Color>): IntArray {
    val dark = colorScheme.surface.luminance() < SonMeshGradient.DARK_SURFACE_LUMINANCE
    val artwork = LocalSonBackdropArtwork.current
    return remember(colorScheme, backdrop, artwork, dark) {
        // 有封面采样就先用它：控制点是封面每一格的颜色，色面因此留着封面的明暗与构图。
        SonMeshGradient.paletteFromArtwork(artwork, colorScheme.surface, dark)
            ?: SonMeshGradient.palette(
                colors = if (backdrop.isNotEmpty()) {
                    backdrop
                } else {
                    // 没有封面（或封面接近灰阶）：退回配色方案里的三个 container 角色。
                    listOf(
                        colorScheme.primaryContainer,
                        colorScheme.secondaryContainer,
                        colorScheme.tertiaryContainer
                    )
                },
                surface = colorScheme.surface,
                dark = dark
            )
    }
}

/** 播放中按 30fps 推进相位；暂停或系统关闭动画时不再请求帧。 */
@Composable
private fun rememberBackdropFlow(playing: Boolean, state: BackdropState) {
    // 系统动画缩放为 0 时必须自己停：这是常驻动画，Compose 不会替我们处理。
    val flowing = playing && ValueAnimator.getDurationScale() > 0f
    LaunchedEffect(flowing) {
        if (!flowing) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos == 0L) {
                    lastNanos = now
                } else {
                    val elapsedMs = (now - lastNanos) / 1_000_000f
                    if (elapsedMs >= SonMotionTiming.BACKDROP_FRAME_MS) {
                        lastNanos = now
                        val advanced = state.phase +
                            elapsedMs / SonMotionTiming.BACKDROP_CYCLE_MS.toFloat()
                        state.phase = advanced % 1f
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawBackdrop(
    state: BackdropState,
    palette: IntArray,
    surface: Color,
    fade: SonBackdropFade,
    dim: Float
) {
    val resolution = SonMeshGradient.RESOLUTION
    SonMeshGradient.render(state.pixels, palette, state.phase)
    state.bitmap.setPixels(state.pixels, 0, resolution, 0, 0, resolution, resolution)
    drawImage(
        image = state.image,
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
    )
    if (dim > 0f) {
        drawRect(color = surface.copy(alpha = dim.coerceIn(0f, 1f)))
    }
    if (fade is SonBackdropFade.Bottom) {
        drawRect(brush = fade.brush(surface))
    }
}

/** 把收束区间画成竖向渐变；`None` 与「从 0 开始」都不需要重复的 stop。 */
private fun SonBackdropFade.Bottom.brush(surface: Color): Brush {
    val fadeStart = start.coerceIn(0f, 0.98f)
    val fadeEnd = end.coerceIn(fadeStart + 0.01f, 1f)
    return if (fadeStart > 0f) {
        Brush.verticalGradient(
            0f to Color.Transparent,
            fadeStart to Color.Transparent,
            fadeEnd to surface,
            1f to surface
        )
    } else {
        Brush.verticalGradient(
            0f to Color.Transparent,
            fadeEnd to surface,
            1f to surface
        )
    }
}
