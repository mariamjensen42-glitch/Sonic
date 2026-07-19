package com.cycling.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.cycling.core.ui.theme.M3Motion

/**
 * 按压缩放动画：为组件创建按下时的缩放效果。
 * 返回 Triple (interactionSource, scale, isPressed)，调用方需将 scale 应用到 modifier.scale(scale)。
 *
 * 当 [enabled] 为 false 时始终返回 1f（无缩放），且 isPressed 恒为 false。
 */
@Composable
fun rememberPressScale(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enabled: Boolean = true,
    pressScale: Float = M3Motion.buttonPressScale
): Triple<MutableInteractionSource, Float, Boolean> {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressScale else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "pressScale"
    )
    return Triple(interactionSource, scale, isPressed && enabled)
}
