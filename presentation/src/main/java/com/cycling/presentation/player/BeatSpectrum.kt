package com.cycling.presentation.player

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.cycling.core.ui.theme.LocalSonExtendedColors
import com.cycling.domain.model.BeatState
import kotlin.math.abs
import kotlin.math.sin

/**
 * 贴屏幕底边的频谱条：一排等宽的柱子，随节拍起伏。
 *
 * 这是播放页唯一的律动表达。封面本身保持静态 —— 早先画在封面背后的两圈同心环
 * 因为半径超出可用宽度、又被封面盖住，实际看不见，已经删掉。
 *
 * 为什么放在最底部：封面是这一屏的视觉重心，频谱挤在封面与标题之间会把
 * 「标题 + 进度 + 运输控件」压扁；贴底边则只借用 `bottomBarClearance` 里的留白，
 * 不改变任何既有版式。
 *
 * 实现约束（项目规范：常驻动画必须自己收口）：
 * - [beat] 是 lambda，只在 [Canvas] 的绘制阶段读快照，数值变化只触发重绘，不重组；
 * - [BeatState.isSilent]（暂停 / 静音）时整条不画 —— 暂停后这一屏必须回到 0 帧；
 * - 柱形分布用 `sin(i)` 算，不用 `Random`：每帧都要画，不能在绘制路径里分配对象。
 */
@Composable
fun BeatSpectrum(
    beat: () -> BeatState,
    modifier: Modifier = Modifier,
    barCount: Int = SPECTRUM_BARS
) {
    val activeColor = LocalSonExtendedColors.current.waveformActive
    val baseColor = LocalSonExtendedColors.current.waveform

    Canvas(modifier = modifier) {
        val snapshot = beat()
        if (snapshot.isSilent) return@Canvas

        val level = snapshot.level.coerceIn(0f, 1f)
        val pulse = snapshot.pulse.coerceIn(0f, 1f)
        val transient = snapshot.beat.coerceIn(0f, 1f)
        val slot = size.width / barCount
        val barWidth = slot * BAR_WIDTH_RATIO
        val minHeight = size.height * MIN_HEIGHT_RATIO
        val usableHeight = (size.height - minHeight).coerceAtLeast(0f)
        val energy = (level * 0.65f + pulse * 0.35f).coerceIn(0f, 1f)

        for (index in 0 until barCount) {
            // 固定形态因子：让柱子高低错落，而不是齐刷刷一条线。
            val shape = 0.35f + 0.65f * abs(sin(index * 1.7f))
            val kick = transient * 0.30f * shape
            val ratio = (shape * energy + kick).coerceIn(0f, 1f)
            val barHeight = minHeight + usableHeight * ratio
            val left = index * slot + (slot - barWidth) / 2f
            val top = size.height - barHeight
            drawBar(
                color = if (ratio > 0.55f) activeColor else baseColor,
                alpha = 0.35f + 0.55f * ratio,
                left = left,
                top = top,
                width = barWidth,
                height = barHeight
            )
        }
    }
}

private fun DrawScope.drawBar(
    color: Color,
    alpha: Float,
    left: Float,
    top: Float,
    width: Float,
    height: Float
) {
    drawRoundRect(
        color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(width / 2f, width / 2f)
    )
}

/** 柱子数量：1080p 宽屏上约 30dp 一根，密度适中。 */
private const val SPECTRUM_BARS = 36

/** 柱宽占槽位的比例，剩下的留作间隔。 */
private const val BAR_WIDTH_RATIO = 0.42f

/** 静音前的基准高度：留一点「有东西在这里」的暗示。 */
private const val MIN_HEIGHT_RATIO = 0.10f
