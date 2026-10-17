package com.cycling.presentation.lyrics.utils.modifier

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.cycling.presentation.lyrics.utils.LayerPaint
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSpacing

/**
 * 列表首尾的渐隐遮罩：内容滚到视口边缘时淡出，暗示"后面还有"。
 *
 * 遮罩用 `DstIn` 擦除，只关心 alpha，因此 [maskColor] 只需是一个**不透明**的主题色 ——
 * 调用方传入当前页底色即可，不要写死黑色。
 */
fun Modifier.dynamicFadingEdge(
    listState: LazyListState,
    index: Int,
    maskColor: Color,
    topFadeHeight: Dp = SonSpacing.large,
    bottomFadeHeight: Dp = SonLayout.bottomBarClearance
): Modifier = this.drawWithContent {
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    val itemInfo = visibleItems.find { it.index == index }

    if (itemInfo == null) {
        drawContent()
        return@drawWithContent
    }

    val topFadeHeightPx = topFadeHeight.toPx()
    val bottomFadeHeightPx = bottomFadeHeight.toPx()

    val containerHeight = layoutInfo.viewportSize.height.toFloat()

    val itemTop = (itemInfo.offset + layoutInfo.beforeContentPadding).toFloat()
    val itemBottom = itemTop + itemInfo.size.toFloat()

    val touchesTop = itemTop < topFadeHeightPx

    val touchesBottom = itemBottom > (containerHeight - bottomFadeHeightPx)

    if (!touchesTop && !touchesBottom) {
        drawContent()
        return@drawWithContent
    }

    drawContext.canvas.saveLayer(
        Rect(0f, 0f, size.width, size.height),
        LayerPaint
    )

    drawContent()

    if (touchesTop) {
        val gradientStart = -itemTop
        val gradientEnd = topFadeHeightPx - itemTop

        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to maskColor,
                startY = gradientStart,
                endY = gradientEnd
            ),
            blendMode = BlendMode.DstIn
        )
    }

    if (touchesBottom) {
        val gradientStart = (containerHeight - bottomFadeHeightPx) - itemTop
        val gradientEnd = containerHeight - itemTop

        drawRect(
            brush = Brush.verticalGradient(
                0f to maskColor,
                1f to Color.Transparent,
                startY = gradientStart,
                endY = gradientEnd
            ),
            blendMode = BlendMode.DstIn
        )
    }

    drawContext.canvas.restore()
}
