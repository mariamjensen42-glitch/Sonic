package com.cycling.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 色面的两个角色。页面只能用这两个角色；不新增 ColorScheme 角色。
 *
 * - [PrimaryContainer]：hero 光晕的唯一颜色。
 * - [Surface]：安静面；标题、列表、正文都从这里开始。
 */
enum class SonColorFieldRole {
    PrimaryContainer,
    Surface
}

/**
 * 色面在 hero 下沿的收束方式。
 */
sealed interface SonColorFieldEnd {

    /** 到 hero 下沿归零（Player 用）。 */
    data object HeroBottom : SonColorFieldEnd

    /** 允许最多 [SonColorField.DetailTail] 进入动作区，绝不进入首行（CollectionDetail 用）。 */
    data object Detail : SonColorFieldEnd
}

/**
 * 一层渐变的纯描述：stop 是相对坐标（0..1），alpha 是 primaryContainer 的不透明度。
 * 最后一档之后沿用最后的 alpha（通常是 0），因此色面可以在 53% 处就收干净。
 */
@Immutable
data class SonColorFieldLayer(
    val stops: List<Float>,
    val alphas: List<Float>
) {

    init {
        require(stops.isNotEmpty()) { "color field layer needs at least one stop" }
        require(stops.size == alphas.size) { "stops and alphas must have the same size" }
        require(stops.first() == 0f) { "color field layer must start at 0" }
        require(stops.zipWithNext().all { (left, right) -> left < right }) {
            "color field stops must be strictly ascending"
        }
    }

    /** 在任意 stop 进度处的 alpha；超出最后一档时沿用最后一档。 */
    fun alphaAt(fraction: Float): Float {
        val clamped = fraction.coerceIn(0f, 1f)
        if (clamped >= stops.last()) return alphas.last()
        for (index in 1 until stops.size) {
            if (clamped <= stops[index]) {
                val span = stops[index] - stops[index - 1]
                if (span <= 0f) return alphas[index]
                val local = (clamped - stops[index - 1]) / span
                return alphas[index - 1] + (alphas[index] - alphas[index - 1]) * local
            }
        }
        return alphas.last()
    }
}

/**
 * 色面的完整纯规格。这是 #18 的单元测试接缝，也是 #20 播放页色面的唯一来源：
 * 页面只把 [SonColorFieldSpec] 交给渲染器，不自己拼 Brush、不写 alpha。
 */
@Immutable
data class SonColorFieldSpec(
    val radial: SonColorFieldLayer,
    val vertical: SonColorFieldLayer,
    val tail: SonColorFieldLayer?
) {

    val tintRole: SonColorFieldRole = SonColorFieldRole.PrimaryContainer

    val surfaceRole: SonColorFieldRole = SonColorFieldRole.Surface
}

/**
 * hero 色面的主题层规则（#6 / ADR-0004）。
 *
 * 两层都取 primaryContainer，叠在 surface 上：
 * - 径向：92% 50% at 50% 23%，stops 0 / 55% / 60%。
 * - 纵向：0 / 24% / 46% / 53%，53% 就是 hero 下沿。
 *
 * 亮色 alpha 必须明显高于暗色：亮色容器是 tone 90、surface 是 98，只差 8 个 tone；
 * 暗色 0.30 就够，亮色要 0.46。不要为了“统一”合并成一个 alpha。
 */
object SonColorField {

    /** 纵向层归零的位置；prototype 里等于 Player 封面下沿。 */
    const val HeroBottomFraction = 0.53f

    const val RadialCenterX = 0.50f
    const val RadialCenterY = 0.23f
    const val RadialRadiusX = 0.92f
    const val RadialRadiusY = 0.50f

    val RadialStops = listOf(0f, 0.55f, 0.60f)
    val VerticalStops = listOf(0f, 0.24f, 0.46f, 0.53f)

    /** CollectionDetail 的下沿 fade 上限；规则允许最多 12dp，不进入首行。 */
    val DetailTail = 12.dp

    private val LightRadialAlphas = listOf(0.70f, 0.34f, 0f)
    private val DarkRadialAlphas = listOf(0.62f, 0.30f, 0f)
    private val LightVerticalAlphas = listOf(0.18f, 0.46f, 0.16f, 0f)
    private val DarkVerticalAlphas = listOf(0.12f, 0.30f, 0.10f, 0f)

    // CollectionDetail 的 hero 是通栏方形，径向层被封面遮住；可见部分只有封面下沿的
    // 12dp fade。它取字段表里的纵向峰值（亮 0.46 / 暗 0.30），不引入 prototype 的
    // 0.58 / 0.42 这一组未验收数值。
    private const val LightDetailTailAlpha = 0.46f
    private const val DarkDetailTailAlpha = 0.30f

    fun spec(
        dark: Boolean,
        end: SonColorFieldEnd = SonColorFieldEnd.HeroBottom
    ): SonColorFieldSpec {
        val radial = SonColorFieldLayer(
            stops = RadialStops,
            alphas = if (dark) DarkRadialAlphas else LightRadialAlphas
        )
        val vertical = SonColorFieldLayer(
            stops = VerticalStops,
            alphas = if (dark) DarkVerticalAlphas else LightVerticalAlphas
        )
        val tail = if (end is SonColorFieldEnd.Detail) {
            SonColorFieldLayer(
                stops = listOf(0f, 1f),
                alphas = listOf(
                    if (dark) DarkDetailTailAlpha else LightDetailTailAlpha,
                    0f
                )
            )
        } else {
            null
        }
        return SonColorFieldSpec(
            radial = radial,
            vertical = vertical,
            tail = tail
        )
    }
}

/**
 * 把 [SonColorFieldSpec] 渲染成 hero 背景（CollectionDetail 与 #20 Player 共用）。
 *
 * 容器就是 hero 区域：高度取 [heroBottom]；纵向 stop 与径向椭圆按
 * [SonColorField.HeroBottomFraction] 归一化，让 53% 正好落在 hero 下沿。
 * surface 只填 hero 区，不会向下遮挡正文；hero 以下的 12dp 收尾由
 * [sonColorFieldDetailFade] 负责。
 */
@Composable
fun SonColorFieldBackground(
    heroBottom: Dp,
    modifier: Modifier = Modifier,
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    dark: Boolean = isSystemInDarkTheme()
) {
    val spec = remember(dark) { SonColorField.spec(dark = dark) }
    val tint = spec.tintRole.resolve(colorScheme)
    val surface = spec.surfaceRole.resolve(colorScheme)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroBottom)
            .drawBehind {
                drawRect(color = surface)
                drawRadialLayer(spec.radial, tint)
                drawRect(
                    brush = Brush.verticalGradient(
                        *spec.vertical.toColorStops(
                            tint = tint,
                            stopScale = SonColorField.HeroBottomFraction
                        )
                    )
                )
            }
    ) { }
}

/**
 * CollectionDetail 的 hero 下沿 fade：只在被调用 composable 的顶部 12dp 内绘制，
 * 不改变布局高度，也不进入首行。动作簇把它画在自己的 top padding 里。
 */
@Composable
fun Modifier.sonColorFieldDetailFade(
    dark: Boolean = isSystemInDarkTheme(),
    tail: Dp = SonColorField.DetailTail,
    colorScheme: ColorScheme = MaterialTheme.colorScheme
): Modifier {
    val spec = remember(dark, tail) {
        SonColorField.spec(dark = dark, end = SonColorFieldEnd.Detail)
    }
    val tint = spec.tintRole.resolve(colorScheme)
    val startAlpha = spec.tail?.alphaAt(0f) ?: 0f

    return drawBehind {
        drawRect(
            brush = Brush.verticalGradient(
                0f to tint.copy(alpha = startAlpha),
                1f to Color.Transparent
            ),
            size = Size(size.width, tail.toPx())
        )
    }
}

private fun SonColorFieldRole.resolve(colorScheme: ColorScheme): Color = when (this) {
    SonColorFieldRole.PrimaryContainer -> colorScheme.primaryContainer
    SonColorFieldRole.Surface -> colorScheme.surface
}

private fun SonColorFieldLayer.toColorStops(
    tint: Color,
    stopScale: Float = 1f
): Array<Pair<Float, Color>> =
    stops.indices.map { index ->
        (stops[index] / stopScale).coerceIn(0f, 1f) to tint.copy(alpha = alphas[index])
    }.toTypedArray()

/**
 * Compose 的 radialGradient 只有圆形半径；这里用 canvas 的纵向缩放把它变成
 * 规则里的椭圆（92% 宽 × 50% 高）。径向的 y 坐标同样除以 HeroBottomFraction，
 * 因此 hero 下沿正好落在径向的 60% stop（alpha 0）。绘制矩形放大到足够覆盖
 * 组件，超出部分由 DrawScope 的边界裁掉。
 */
private fun DrawScope.drawRadialLayer(layer: SonColorFieldLayer, tint: Color) {
    val center = Offset(
        x = size.width * SonColorField.RadialCenterX,
        y = size.height * (SonColorField.RadialCenterY / SonColorField.HeroBottomFraction)
    )
    val radiusX = size.width * SonColorField.RadialRadiusX
    val radiusY = size.height * (SonColorField.RadialRadiusY / SonColorField.HeroBottomFraction)
    if (radiusX <= 0f || radiusY <= 0f) return

    val brush = Brush.radialGradient(
        *layer.toColorStops(tint),
        center = center,
        radius = radiusX
    )
    withTransform({
        scale(scaleX = 1f, scaleY = radiusY / radiusX, pivot = center)
    }) {
        drawRect(
            brush = brush,
            topLeft = Offset(0f, center.y - size.height),
            size = Size(size.width, size.height * 2f)
        )
    }
}
