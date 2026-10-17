@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.core.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

/**
 * 主题层的两个 shape morph（#6 / ADR-0004）。页面只消费结果，不自己写形状或弹簧：
 *
 * 1. 播放键按压：MaterialTheme.shapes.extraLarge（28dp）↔ CircleShape，
 *    只在按压期间发生，松手/取消回到 idle；播放/暂停切换不参与。
 * 2. hero 折叠：尺寸、圆角（RectangleShape → MaterialTheme.shapes.small 8dp）、
 *    位置连续插值，由滚动进度驱动，不是定时动画。
 *
 * 其余 Material 组件（Button / IconButton / ListItem / Chip …）保留官方按压反馈，
 * 不在这里包转发壳。
 */
object SonShapeMorph {

    /** LargeFloatingActionButton 的默认圆角，与 SonShapes.extraLarge 一致。 */
    val PressIdleCorner = 28.dp

    /** 96dp 的 LargeFloatingActionButton 变成圆时的半径；CircleShape 的几何等价。 */
    val PressEndCorner = SonSizes.fabLarge / 2f

    /** hero 折叠后的圆角，与 SonShapes.small 一致。 */
    val HeroFoldCorner = 8.dp

    /** 按压进度 0..1 对应的圆角；0 是 idle，1 是圆。 */
    fun pressCorner(progress: Float, idleCorner: Dp = PressIdleCorner): Dp =
        lerp(idleCorner, PressEndCorner, progress.coerceIn(0f, 1f))

    /**
     * 按压进度 0..1 对应的形状。0 返回主题的 extraLarge（不是手写 28dp），
     * 1 返回 CircleShape；中间按圆角线性插值。
     */
    fun pressShape(progress: Float, idleCorner: Dp = PressIdleCorner): Shape {
        val clamped = progress.coerceIn(0f, 1f)
        return when {
            clamped <= 0f ->
                if (idleCorner == PressIdleCorner) SonShapes.extraLarge else RoundedCornerShape(idleCorner)

            clamped >= 1f -> CircleShape
            else -> RoundedCornerShape(pressCorner(clamped, idleCorner))
        }
    }

    /**
     * hero 折叠在某一进度下的几何。start 通常是 hero 的边长与左上角，
     * end 是 top bar 缩略图的边长与左上角；中间全部线性插值。
     */
    data class HeroFold(
        val size: Dp,
        val corner: Dp,
        val x: Dp,
        val y: Dp
    )

    fun heroFold(
        progress: Float,
        startSize: Dp,
        endSize: Dp,
        startX: Dp = 0.dp,
        endX: Dp = 0.dp,
        startY: Dp = 0.dp,
        endY: Dp = 0.dp
    ): HeroFold {
        val clamped = progress.coerceIn(0f, 1f)
        return HeroFold(
            size = lerp(startSize, endSize, clamped),
            corner = HeroFoldCorner * clamped,
            x = lerp(startX, endX, clamped),
            y = lerp(startY, endY, clamped)
        )
    }

    /**
     * hero 折叠的形状：0 是 RectangleShape，1 是 MaterialTheme.shapes.small (8dp)，
     * 中间按视觉圆角线性插值。
     */
    fun heroFoldShape(progress: Float): Shape {
        val clamped = progress.coerceIn(0f, 1f)
        return when {
            clamped <= 0f -> RectangleShape
            clamped >= 1f -> SonShapes.small
            else -> RoundedCornerShape(HeroFoldCorner * clamped)
        }
    }

    /**
     * graphicsLayer 版本的折叠形状。
     *
     * 折叠 overlay 用 layer 的 scale/translation 完成，避免每帧改布局、让封面重新解码；
     * 因此 clip 形状写在 layer 的局部坐标里。视觉圆角 = HeroFoldCorner * progress，
     * 局部圆角要除以当前 scale，缩放后才是同一个视觉圆角。
     */
    fun heroFoldLayerShape(progress: Float, scale: Float): Shape {
        val clamped = progress.coerceIn(0f, 1f)
        if (clamped <= 0f || scale <= 0f) return RectangleShape
        return RoundedCornerShape(HeroFoldCorner * clamped / scale)
    }
}

/**
 * 播放键的按压形变。必须传入 FAB 自己的 InteractionSource；按压/取消由组件的
 * clickable 触发，播放/暂停切换不会改变它。弹簧只从 MaterialTheme.motionScheme
 * 取 fastSpatialSpec，不写裸 spring/tween。
 */
@Composable
fun rememberPressShapeMorph(
    interactionSource: InteractionSource,
    idleCorner: Dp = SonShapeMorph.PressIdleCorner
): Shape {
    val pressed by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>(),
        label = "pressShapeMorph"
    )
    return SonShapeMorph.pressShape(progress = progress, idleCorner = idleCorner)
}
