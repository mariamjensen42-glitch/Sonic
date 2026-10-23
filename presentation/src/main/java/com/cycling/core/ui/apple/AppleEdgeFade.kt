package com.cycling.core.ui.apple

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 两端 alpha 渐隐，对应 Apple 的 `FullWidthAlphaGradientFlexboxLayout`。
 *
 * 长歌词贴到屏幕边缘时自然淡出，而不是缩字号或者截断。做法是把内容画进离屏层，
 * 再用一段「透明 → 不透明 → 不透明 → 透明」的水平渐变以 [BlendMode.DstIn] 抹一遍。
 *
 * 注意必须开 `CompositingStrategy.Offscreen`：否则 `DstIn` 会作用到父层的已有像素上，
 * 把身后的背景一起擦掉。
 */
fun Modifier.appleEdgeFade(fadeWidth: Dp = 24.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        if (size.width <= 0f) return@drawWithContent
        val f = (fadeWidth.toPx() / size.width).coerceIn(0f, 0.5f)
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                f to Color.Black,
                1f - f to Color.Black,
                1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }
