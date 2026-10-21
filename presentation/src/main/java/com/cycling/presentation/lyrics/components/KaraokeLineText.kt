package com.cycling.presentation.lyrics.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.theme.LocalSonExtendedColors
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.lyrics.model.karaoke.KaraokeAlignment
import com.cycling.domain.lyrics.model.karaoke.KaraokeLine
import com.cycling.presentation.lyrics.utils.LayerPaint
import com.cycling.presentation.lyrics.utils.easing.Bounce
import com.cycling.presentation.lyrics.utils.easing.Swell
import com.cycling.presentation.lyrics.utils.isRtl
import kotlin.math.roundToInt

/** 未激活文字的不透明度，与渐变蒙版的未激活色保持一致 */
private const val InactiveAlpha = 0.2f

// 静态行不读帧时钟，直接用一个远早/远晚的时间戳让动画计算落在 0 / 1 两个端点。
// 不用 Int.MIN_VALUE / Int.MAX_VALUE 是为了避免 currentTimeMs - start 溢出。
private const val StaticBeforeStartTimeMs = -1_000_000_000
private const val StaticAfterEndTimeMs = 1_000_000_000

/**
 * 一行歌词当前的渲染形态：
 * - [BeforeStart] 还没开始唱，整行未激活色
 * - [Animating] 正在推进，需要逐帧重绘 + 渐变蒙版
 * - [AfterEnd] 已经唱完，整行激活色
 *
 * 后两者与时间无关，可以不带蒙版直接绘制，也不必逐帧重绘。
 */
enum class KaraokeLinePhase { BeforeStart, Animating, AfterEnd }

private fun createLineGradientBrush(
    lineLayout: List<SyllableLayout>,
    currentTimeMs: Int,
    isRtl: Boolean,
    color: Color
): Brush {
    val activeColor = color
    val inactiveColor = color.copy(alpha = InactiveAlpha)
    val minFadeWidth = 100f

    if (lineLayout.isEmpty()) {
        return Brush.horizontalGradient(colors = listOf(inactiveColor, inactiveColor))
    }

    val totalMinX = lineLayout.minOf { it.position.x }
    val totalMaxX = lineLayout.maxOf { it.position.x + it.width }
    val totalWidth = totalMaxX - totalMinX

    if (totalWidth <= 0f) {
        val isFinished = currentTimeMs >= lineLayout.last().syllable.end
        val color = if (isFinished) activeColor else inactiveColor
        return Brush.horizontalGradient(listOf(color, color))
    }

    val firstSyllableStart = lineLayout.first().syllable.start
    val lastSyllableEnd = lineLayout.last().syllable.end

    val lineProgress = run {
        if (currentTimeMs <= firstSyllableStart) return Brush.horizontalGradient(
            listOf(inactiveColor, inactiveColor)
        )
        if (currentTimeMs >= lastSyllableEnd) return Brush.horizontalGradient(
            listOf(activeColor, activeColor)
        )

        val activeSyllableLayout = lineLayout.find {
            currentTimeMs in it.syllable.start until it.syllable.end
        }

        val currentPixelPosition = when {
            activeSyllableLayout != null -> {
                val syllableProgress = activeSyllableLayout.syllable.progress(currentTimeMs)
                if (isRtl) {
                    activeSyllableLayout.position.x + activeSyllableLayout.width * (1f - syllableProgress)
                } else {
                    activeSyllableLayout.position.x + activeSyllableLayout.width * syllableProgress
                }
            }
            else -> {
                val lastFinished = lineLayout.lastOrNull { currentTimeMs >= it.syllable.end }
                if (isRtl) {
                    lastFinished?.position?.x ?: totalMaxX
                } else {
                    lastFinished?.let { it.position.x + it.width } ?: totalMinX
                }
            }
        }
        ((currentPixelPosition - totalMinX) / totalWidth).coerceIn(0f, 1f)
    }

    val fadeRange = (minFadeWidth / totalWidth).coerceAtMost(1f)
    val fadeCenterStart = -fadeRange / 2f
    val fadeCenterEnd = 1f + fadeRange / 2f
    val fadeCenter = fadeCenterStart + (fadeCenterEnd - fadeCenterStart) * lineProgress
    val fadeStart = fadeCenter - fadeRange / 2f
    val fadeEnd = fadeCenter + fadeRange / 2f

    val colorStops = if (isRtl) {
        arrayOf(
            0.0f to inactiveColor,
            fadeStart.coerceIn(0f, 1f) to inactiveColor,
            fadeEnd.coerceIn(0f, 1f) to activeColor,
            1.0f to activeColor
        )
    } else {
        arrayOf(
            0.0f to activeColor,
            fadeStart.coerceIn(0f, 1f) to activeColor,
            fadeEnd.coerceIn(0f, 1f) to inactiveColor,
            1.0f to inactiveColor
        )
    }

    return Brush.horizontalGradient(
        colorStops = colorStops,
        startX = totalMinX,
        endX = totalMaxX
    )
}

fun DrawScope.drawLyricsLine(
    lineLayouts: List<List<SyllableLayout>>,
    currentTimeMs: Int,
    color: Color,
    isRtl: Boolean,
    phase: KaraokeLinePhase = KaraokeLinePhase.Animating
) {
    if (phase != KaraokeLinePhase.Animating) {
        // 静态行整行单色，不需要离屏蒙版（saveLayer）
        val staticColor =
            if (phase == KaraokeLinePhase.BeforeStart) color.copy(alpha = InactiveAlpha) else color
        lineLayouts.forEach { rowLayouts ->
            if (rowLayouts.isNotEmpty()) {
                drawRowText(rowLayouts, staticColor, currentTimeMs)
            }
        }
        return
    }

    lineLayouts.forEach { rowLayouts ->
        if (rowLayouts.isEmpty()) return@forEach

        val lastSyllableEnd = rowLayouts.last().syllable.end

        if (currentTimeMs >= lastSyllableEnd) {
            drawRowText(rowLayouts, color, currentTimeMs)
            return@forEach
        }

        val minX = rowLayouts.minOf { it.position.x }
        val maxX = rowLayouts.maxOf { it.position.x + it.width }
        val minY = rowLayouts.minOf { it.position.y }
        val totalHeight = rowLayouts.maxOf { it.textLayoutResult.size.height }.toFloat()

        val verticalPadding = (totalHeight * 0.1).dp.toPx()
        val horizontalPadding = ((maxX - minX) * 0.2).dp.toPx()

        drawIntoCanvas { canvas ->
            val layerBounds = Rect(
                left = minX - horizontalPadding,
                top = minY - verticalPadding,
                right = maxX + horizontalPadding,
                bottom = minY + totalHeight + verticalPadding
            )
            canvas.saveLayer(layerBounds, LayerPaint)

            drawRowText(rowLayouts, color, currentTimeMs)

            val progressBrush = createLineGradientBrush(rowLayouts, currentTimeMs, isRtl, color)
            drawRect(
                brush = progressBrush,
                topLeft = layerBounds.topLeft,
                size = layerBounds.size,
                blendMode = BlendMode.DstIn
            )
            canvas.restore()
        }
    }
}

private fun DrawScope.drawRowText(
    rowLayouts: List<SyllableLayout>,
    drawColor: Color,
    currentTimeMs: Int,
) {
    rowLayouts.forEach { syllableLayout ->
        val wordAnimInfo = syllableLayout.wordAnimInfo

        if (wordAnimInfo != null) {
            val fastCharAnimationThresholdMs = 200f
            val awesomeDuration = wordAnimInfo.wordDuration * 0.8f

            val charLayouts = syllableLayout.charLayouts ?: emptyList()
            val charBounds = syllableLayout.charOriginalBounds ?: emptyList()

            syllableLayout.syllable.content.forEachIndexed { charIndex, _ ->
                val singleCharLayoutResult = charLayouts.getOrNull(charIndex) ?: return@forEachIndexed
                val charBox = charBounds.getOrNull(charIndex) ?: return@forEachIndexed

                val absoluteCharIndex = syllableLayout.charOffsetInWord + charIndex
                val numCharsInWord = wordAnimInfo.wordContent.length
                val earliestStartTime = wordAnimInfo.wordStartTime
                val latestStartTime = wordAnimInfo.wordEndTime - awesomeDuration

                val charRatio = if (numCharsInWord > 1) absoluteCharIndex.toFloat() / (numCharsInWord - 1) else 0.5f
                val awesomeStartTime = (earliestStartTime + (latestStartTime - earliestStartTime) * charRatio).toLong()
                val awesomeProgress = ((currentTimeMs - awesomeStartTime).toFloat() / awesomeDuration).coerceIn(0f, 1f)

                val scale = 1f + Swell(
                    (0.1 * (wordAnimInfo.wordDuration - fastCharAnimationThresholdMs * numCharsInWord) / 1000).coerceIn(0.0, 0.1)
                ).transform(awesomeProgress)

                val centeredOffsetX = (charBox.width - singleCharLayoutResult.size.width) / 2f
                val xPos = syllableLayout.position.x + charBox.left + centeredOffsetX
                val yPos = syllableLayout.position.y + charBox.top

                val blurRadius = 10f * Bounce.transform(awesomeProgress)
                // 模糊半径为 0 时阴影完全被字形覆盖，直接跳过，
                // 否则静态行降透明度后阴影会透出来
                val shadow = if (blurRadius > 0f) {
                    Shadow(
                        color = drawColor.copy(0.4f),
                        offset = Offset(0f, 0f),
                        blurRadius = blurRadius
                    )
                } else {
                    null
                }
                withTransform({ scale(scale = scale, pivot = syllableLayout.wordPivot) }) {
                    drawText(
                        textLayoutResult = singleCharLayoutResult,
                        color = drawColor,
                        topLeft = Offset(xPos, yPos),
                        shadow = shadow,
                    )
                }
            }
        } else {
            drawText(
                textLayoutResult = syllableLayout.textLayoutResult,
                color = drawColor,
                topLeft = syllableLayout.position,
            )
        }
    }
}

@Composable
fun KaraokeLineText(
    line: KaraokeLine,
    currentTimeProvider: () -> Int,
    phase: KaraokeLinePhase = KaraokeLinePhase.Animating,
    modifier: Modifier = Modifier,
    normalLineTextStyle: TextStyle = LocalTextStyle.current,
    accompanimentLineTextStyle: TextStyle = LocalTextStyle.current,
    activeColor: Color = Color.Unspecified,
    precalculatedLayouts: List<SyllableLayout>? = null,
    /** 行文本在可用宽度内居中；关掉就按语言方向靠起始侧。 */
    alignCenter: Boolean = false,
    textMeasurer: TextMeasurer = rememberTextMeasurer()
) {
    val isLineRtl = remember(line.syllables) { line.syllables.any { it.content.isRtl() } }

    val isRightAligned = remember(line.alignment, isLineRtl) {
        when (line.alignment) {
            KaraokeAlignment.Start, KaraokeAlignment.Unspecified -> isLineRtl
            KaraokeAlignment.End -> !isLineRtl
        }
    }

    val translationTextAlign = remember(isRightAligned, alignCenter) {
        when {
            alignCenter -> TextAlign.Center
            isRightAligned -> TextAlign.End
            else -> TextAlign.Start
        }
    }

    val columnHorizontalAlignment = remember(isRightAligned, alignCenter) {
        when {
            alignCenter -> Alignment.CenterHorizontally
            isRightAligned -> Alignment.End
            else -> Alignment.Start
        }
    }

    // 已唱/未唱的刷色都来自主题角色，调用方不传就取"当前行高亮"色
    val extended = LocalSonExtendedColors.current
    val lyricsColor = if (activeColor == Color.Unspecified) extended.lyricsActive else activeColor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SonSpacing.small, horizontal = SonSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(SonSpacing.extraSmall),
        horizontalAlignment = columnHorizontalAlignment
    ) {
        BoxWithConstraints {
            val density = LocalDensity.current
            val availableWidthPx = with(density) { maxWidth.toPx() }

            val textStyle = remember(line.isAccompaniment) {
                val baseStyle = if (line.isAccompaniment) accompanimentLineTextStyle else normalLineTextStyle
                baseStyle.copy(textDirection = TextDirection.Content)
            }

            val spaceWidth = remember(textMeasurer, textStyle) {
                textMeasurer.measure(" ", textStyle).size.width.toFloat()
            }

            val processedSyllables = remember(line.syllables, line.alignment) {
                if (line.alignment == KaraokeAlignment.End) {
                    line.syllables.dropLastWhile { it.content.isBlank() }
                } else {
                    line.syllables
                }
            }

            val initialLayouts = remember(
                precalculatedLayouts, processedSyllables, textStyle, spaceWidth, textMeasurer,
                line.isAccompaniment
            ) {
                precalculatedLayouts ?: measureSyllablesAndDetermineAnimation(
                    syllables = processedSyllables,
                    textMeasurer = textMeasurer,
                    style = textStyle,
                    isAccompanimentLine = line.isAccompaniment,
                    spaceWidth = spaceWidth
                )
            }

            val wrappedLines = remember(initialLayouts, availableWidthPx, textStyle, textMeasurer) {
                calculateBalancedLines(
                    syllableLayouts = initialLayouts,
                    availableWidthPx = availableWidthPx,
                    textMeasurer = textMeasurer,
                    style = textStyle
                )
            }

            val lineHeight = remember(textStyle) {
                textMeasurer.measure("M", textStyle).size.height.toFloat()
            }

            val finalLineLayouts = remember(wrappedLines, availableWidthPx, lineHeight,
                isLineRtl, isRightAligned, alignCenter) {
                calculateStaticLineLayout(
                    wrappedLines = wrappedLines,
                    isLineRightAligned = isRightAligned,
                    canvasWidth = availableWidthPx,
                    lineHeight = lineHeight,
                    isRtl = isLineRtl,
                    isLineCentered = alignCenter
                )
            }

            val totalHeight = remember(wrappedLines, lineHeight) {
                lineHeight * wrappedLines.size
            }

            Canvas(modifier = Modifier.size(maxWidth, (totalHeight.roundToInt() + 8).toDp())) {
                val time = when (phase) {
                    KaraokeLinePhase.BeforeStart -> StaticBeforeStartTimeMs
                    KaraokeLinePhase.AfterEnd -> StaticAfterEndTimeMs
                    KaraokeLinePhase.Animating -> currentTimeProvider()
                }
                drawLyricsLine(
                    lineLayouts = finalLineLayouts,
                    currentTimeMs = time,
                    color = lyricsColor,
                    isRtl = isLineRtl,
                    phase = phase
                )
            }
        }
        // 去掉了 graphicsLayer 中的 blendMode
        line.translation?.let { translation ->
            // 译文没有逐字时间轴，就跟着原文的高亮走：本行还没开始保持"非当前行"色，开始了就点亮
            val translationColor by animateColorAsState(
                targetValue = if (phase == KaraokeLinePhase.BeforeStart) {
                    extended.lyricsInactive
                } else {
                    extended.lyricsActive
                },
                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                label = "translationColor"
            )
            Text(
                text = translation,
                style = MaterialTheme.typography.titleMedium,
                color = translationColor,
                textAlign = translationTextAlign
            )
        }
    }
}

@Composable
private fun Int.toDp(): Dp = with(LocalDensity.current) { this@toDp.toDp() }
