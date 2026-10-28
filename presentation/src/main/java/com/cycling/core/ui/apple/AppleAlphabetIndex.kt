package com.cycling.core.ui.apple

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 右侧 A–Z 索引条，照 Apple Music 的 `FastScroller` 复刻。
 *
 * 规格取自 7.0.0-beta 的资源表：拇指 `fastscroller_bubble` 6×30dp（圆角 8dp）、
 * 拖动气泡 `fastscroller_handle` 80×80dp 圆、气泡文字 40sp、触摸带 100dp 宽。
 * 这里把触摸带收到 32dp（100dp 会白吃掉列表一列），视觉与手势区域一致。
 *
 * 字母**按屏高比例自适应**：整条占可用高度的 [IndexHeightFraction]，格高 = 该高度 / 字母数
 * （夹在 12–18dp），字号 = 格高 × 0.8（夹在 9–13sp）。字母少时自动撑开、26 个字母时自动收紧，
 * 不会出现"小屏太长、大屏太小"。
 *
 * 命中计算用 `(y / height × n)`：格子等高等距，与这条线性映射同源，
 * 所以手指在哪、气泡就出现在哪，不会有一格偏差。
 *
 * @param letters 当前曲库里真实存在的分区字母（不含空分区）
 * @param current 高亮的字母，由调用方按首个可见行推算
 * @param onLetterSelected 点到 / 拖到某个字母
 */
@Composable
fun AppleAlphabetIndex(
    letters: List<Char>,
    current: Char?,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    if (letters.isEmpty()) return

    val colors = AppleTheme.colors
    val density = LocalDensity.current
    var stripHeightPx by remember { mutableIntStateOf(0) }
    var draggingLetter by remember { mutableStateOf<Char?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .width(IndexTouchWidth)
            .onSizeChanged { stripHeightPx = it.height }
            .pointerInput(letters) {
                // 命中公式与 Column 的等高等距排布一致：(i + 0.5) / n。
                fun letterAt(y: Float): Char? {
                    if (size.height <= 0) return null
                    val index = (y / size.height * letters.size).toInt()
                        .coerceIn(0, letters.lastIndex)
                    return letters[index]
                }

                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        letterAt(offset.y)?.let {
                            draggingLetter = it
                            onLetterSelected(it)
                        }
                    },
                    onDragEnd = { draggingLetter = null },
                    onDragCancel = { draggingLetter = null },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        letterAt(change.position.y)?.let {
                            if (it != draggingLetter) {
                                draggingLetter = it
                                onLetterSelected(it)
                            }
                        }
                    }
                )
            }
            .pointerInput(letters) {
                detectTapGestures { offset ->
                    val index = (offset.y / size.height * letters.size).toInt()
                        .coerceIn(0, letters.lastIndex)
                    onLetterSelected(letters[index])
                }
            }
    ) {
        // 格高按"整条占多高"反推，再夹在上下界里：字母少时自动撑开，26 个时自动收紧。
        // 字号跟着格高走，所以小屏不会显得太长、大屏也不会显得太小。
        val cellHeight = (maxHeight * (IndexHeightFraction / letters.size))
            .coerceIn(IndexCellMin, IndexCellMax)
        val fontSize = (cellHeight.value * IndexFontRatio).coerceIn(IndexFontMin, IndexFontMax).sp

        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { letter ->
                Box(
                    modifier = Modifier.height(cellHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter.toString(),
                        color = if (letter == current) colors.label else colors.secondaryLabel,
                        fontSize = fontSize,
                        lineHeight = fontSize * 1.15f,
                        fontWeight = if (letter == current) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 拖动气泡：贴在手边、跟着当前字母的纵向位置走。
        draggingLetter?.let { letter ->
            val index = letters.indexOf(letter)
            if (index >= 0 && stripHeightPx > 0) {
                val bubblePx = with(density) { BubbleSize.toPx() }
                val y = ((index + 0.5f) / letters.size * stripHeightPx - bubblePx / 2f)
                    .coerceIn(0f, (stripHeightPx - bubblePx).coerceAtLeast(0f))
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(x = -(bubblePx + with(density) { 8.dp.toPx() }).toInt(), y = y.toInt())
                        }
                        .size(BubbleSize)
                        .shadow(elevation = 12.dp, shape = CircleShape)
                        .background(colors.material, CircleShape)
                        .border(AppleDims.hairline, colors.materialStroke, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter.toString(),
                        color = colors.label,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** 触摸带宽度。原版 100dp 是"透明命中区"，这里视觉与命中区合一，收到 32dp。 */
private val IndexTouchWidth = 32.dp

/** 单格高度的边界：太小点不中，太大整条又会被撑长。 */
private val IndexCellMin = 12.dp
private val IndexCellMax = 18.dp

/** 整条索引占可用高度的比例（格高 = 该高度 / 字母数，再夹到上面两条边界内）。 */
private const val IndexHeightFraction = 0.42f

/** 字号跟着格高走：字号 = 格高 × 该比例，再夹在 9–13sp。 */
private const val IndexFontRatio = 0.8f
private const val IndexFontMin = 9f
private const val IndexFontMax = 13f

/** 拖动气泡直径：`fastscroller_handle` 的 80×80dp。 */
private val BubbleSize = 80.dp
