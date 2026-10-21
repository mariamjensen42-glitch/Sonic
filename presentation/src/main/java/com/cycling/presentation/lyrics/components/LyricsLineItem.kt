package com.cycling.presentation.lyrics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LyricsLineItem(
    isFocused: Boolean,
    isRightAligned: Boolean,
    onLineClicked: () -> Unit,
    onLinePressed: () -> Unit,
    blurRadius: () -> Float,
    modifier: Modifier = Modifier,
    activeAlpha: Float = 1f,
    inactiveAlpha: Float = 0.4f,
    /** 行文本居中时，缩放原点也要在中间，否则放大像是往一侧甩。 */
    isCentered: Boolean = false,
    content: @Composable () -> Unit
) {
    val motionScheme = MaterialTheme.motionScheme

    // 聚焦是"靠近"、失焦是"退开"：靠近用默认空间弹簧，退开用更快的，避免拖尾
    // 体量差 1.04 : 0.92，当前行比其它行大约 13%，才看得出"这一句在唱"
    val scaleState by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 0.92f,
        animationSpec = if (isFocused) {
            motionScheme.defaultSpatialSpec()
        } else {
            motionScheme.fastSpatialSpec()
        },
        label = "scale"
    )

    val alphaState by animateFloatAsState(
        targetValue = if (isFocused) activeAlpha else inactiveAlpha,
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scaleState
                scaleY = scaleState
                alpha = alphaState
                transformOrigin = TransformOrigin(
                    if (isCentered) 0.5f else if (isRightAligned) 1f else 0f,
                    1f
                )

                val radius = blurRadius()
                // 只有真的需要模糊时才开离屏层，否则白白多一层离屏缓冲
                compositingStrategy =
                    if (radius > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                renderEffect = if (radius > 0f) {
                    BlurEffect(
                        radiusX = radius,
                        radiusY = radius,
                        edgeTreatment = TileMode.Clamp
                    )
                } else {
                    null
                }
            }
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(
                onClick = onLineClicked,
                onLongClick = onLinePressed
            )
    ) {
        content()
    }
}
