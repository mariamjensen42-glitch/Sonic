package com.cycling.presentation.lyrics.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.cycling.core.ui.theme.LocalSonExtendedColors
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.lyrics.model.karaoke.KaraokeAlignment
import kotlin.math.PI
import kotlin.math.cos

data class KaraokeBreathingDotsDefaults(
    val number: Int = 3,
    val size: Dp = SonSpacing.medium,
    val margin: Dp = SonSpacing.mediumSmall,
    val enterDurationMs: Int = 3000,
    val preExitStillDuration: Int = 200,
    val preExitDipAndRiseDuration: Int = 3000,
    val exitDurationMs: Int = 200,
    /** 留空表示跟随主题的"非当前行"色。 */
    val breathingDotsColor: Color = Color.Unspecified
)

@Composable
fun KaraokeBreathingDots(
    alignment: KaraokeAlignment,
    startTimeMs: Int,
    endTimeMs: Int,
    currentTimeProvider: () -> Int,
    modifier: Modifier = Modifier,
    defaults: KaraokeBreathingDotsDefaults = KaraokeBreathingDotsDefaults(),
    /** 歌词行居中时，间奏点也跟着居中，否则会单独黏在左边。 */
    center: Boolean = false,
) {
    val extended = LocalSonExtendedColors.current
    val dotColor =
        if (defaults.breathingDotsColor == Color.Unspecified) extended.lyricsInactive
        else defaults.breathingDotsColor

    // 撑满宽度，居中对齐才有参照；非居中时 Canvas 仍贴起始侧，观感不变。
    Box(modifier.fillMaxWidth()) {
        val size = with(LocalDensity.current) { defaults.size.toPx() }
        val margin = with(LocalDensity.current) { defaults.margin.toPx() }
        val totalWidth = size * defaults.number + margin * (defaults.number - 1)

        Canvas(
            Modifier
                .align(
                    when {
                        center -> Alignment.TopCenter
                        alignment == KaraokeAlignment.End -> Alignment.TopEnd
                        else -> Alignment.TopStart
                    }
                )
                .padding(vertical = SonSpacing.small, horizontal = SonSpacing.medium)
                .size(
                    width = defaults.size * defaults.number + defaults.margin * (defaults.number - 1),
                    height = defaults.size
                )
        ) {
            if (totalWidth <= 0f) return@Canvas

            val currentTime = currentTimeProvider().toFloat()
            val enterDuration = defaults.enterDurationMs.toFloat()
            val exitDuration = defaults.exitDurationMs.toFloat()
            val preExitStillDuration = defaults.preExitStillDuration.toFloat()
            val preExitDipAndRiseDuration = defaults.preExitDipAndRiseDuration.toFloat()

            val exitStartTime = endTimeMs - exitDuration
            val preExitStillStartTime = exitStartTime - preExitStillDuration
            val preExitDipAndRiseStartTime = preExitStillStartTime - preExitDipAndRiseDuration
            val breathingStartTime = startTimeMs + enterDuration
            val breathingDuration = preExitDipAndRiseStartTime - breathingStartTime

            var scale: Float
            var alpha: Float
            var revealProgress: Float

            if (breathingDuration < 0) {
                val overallProgress = when {
                    currentTime < startTimeMs + enterDuration -> (currentTime - startTimeMs) / enterDuration
                    currentTime > endTimeMs - exitDuration -> (endTimeMs - currentTime) / exitDuration
                    else -> 1f
                }.coerceIn(0f, 1f)

                scale = overallProgress
                alpha = overallProgress
                revealProgress = if (currentTime < startTimeMs + enterDuration) overallProgress else 1f
            } else {
                when {
                    currentTime < breathingStartTime -> {
                        val progress = (currentTime - startTimeMs) / enterDuration
                        alpha = LinearOutSlowInEasing.transform(progress)
                        scale = alpha * 0.8f
                        revealProgress = alpha
                    }

                    currentTime < preExitDipAndRiseStartTime -> {
                        alpha = 1f
                        revealProgress = 1f
                        val timeInPhase = currentTime - breathingStartTime
                        val angle = (timeInPhase / 3000f) * 2 * PI
                        scale = 0.9f - 0.1f * cos(angle.toFloat())
                    }

                    currentTime < preExitStillStartTime -> {
                        alpha = 1f
                        revealProgress = 1f
                        val phaseProgress = (currentTime - preExitDipAndRiseStartTime) / preExitDipAndRiseDuration

                        val angle = phaseProgress * 2 * PI
                        val cosValue = cos(angle.toFloat())

                        scale = 0.8f + 0.2f * cosValue
                    }

                    currentTime < exitStartTime -> {
                        alpha = 1f
                        revealProgress = 1f
                        scale = 1.0f
                    }

                    else -> {
                        val progress = ((endTimeMs - currentTime) / exitDuration).coerceIn(0f, 1f)
                        val eased = LinearOutSlowInEasing.transform(progress)
                        alpha = eased
                        scale = eased
                        revealProgress = 1f
                    }
                }
            }

            drawIntoCanvas { canvas ->
                val paint = Paint()
                canvas.saveLayer(Rect(Offset.Zero, Size(totalWidth, size)), paint)

                withTransform({
                    this.scale(
                        scale = scale,
                        pivot = Offset(totalWidth / 2f, size / 2f)
                    )
                }) {
                    repeat(defaults.number) { index ->
                        val dotAlpha: Float
                        if (breathingDuration > 0 && currentTime >= breathingStartTime) {
                            val dotDurationMs = breathingDuration / defaults.number
                            val dotStartTimeInPhase = breathingStartTime + index * dotDurationMs
                            val progressForAlpha = ((currentTime - dotStartTimeInPhase) / dotDurationMs).coerceIn(0f, 1f)
                            dotAlpha = 0.4f + 0.6f * LinearEasing.transform(progressForAlpha)
                        } else {
                            dotAlpha = 0.4f
                        }

                        drawCircle(
                            color = dotColor.copy(alpha = dotAlpha * alpha),
                            radius = size / 2,
                            center = Offset(size / 2 + (size + margin) * index, size / 2)
                        )
                    }
                }

                // 逐个点亮：右侧用 DstIn 遮罩擦掉尚未揭晓的部分。
                // 遮罩只看 alpha，所以"保留"端用完全不透明的主题色即可，不能写死黑色
                val softEdgeWidth = 0.5f
                val revealPosition = revealProgress * (1f + softEdgeWidth)
                val startFade = (revealPosition - softEdgeWidth).coerceIn(0f, 1f)
                val endFade = revealPosition.coerceIn(0f, 1f)

                drawRect(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to dotColor,
                            startFade to dotColor,
                            endFade to Color.Transparent,
                            1f to Color.Transparent
                        )
                    ),
                    blendMode = BlendMode.DstIn
                )
                canvas.restore()
            }
        }
    }
}
