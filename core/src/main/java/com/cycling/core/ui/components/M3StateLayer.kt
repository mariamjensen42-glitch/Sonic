package com.cycling.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import com.cycling.core.ui.animation.M3DurationTokens
import com.cycling.core.ui.theme.M3StateLayers

object M3StateLayerDefaults {
    val hoverAlpha = M3StateLayers.hover
    val focusAlpha = M3StateLayers.focus
    val pressedAlpha = M3StateLayers.pressed
    val dragAlpha = M3StateLayers.drag
}

@Composable
fun rememberM3StateLayerColor(
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource,
    enabled: Boolean = true,
    baseColor: Color = MaterialTheme.colorScheme.onSurface,
    hoverAlpha: Float = M3StateLayerDefaults.hoverAlpha,
    focusAlpha: Float = M3StateLayerDefaults.focusAlpha,
    pressedAlpha: Float = M3StateLayerDefaults.pressedAlpha
): Color {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetAlpha = when {
        !enabled -> 0f
        isPressed -> pressedAlpha
        isFocused -> focusAlpha
        isHovered -> hoverAlpha
        else -> 0f
    }

    val animatedColor by animateColorAsState(
        targetValue = baseColor.copy(alpha = targetAlpha),
        animationSpec = tween(durationMillis = M3DurationTokens.short),
        label = "stateLayerAlpha"
    )

    return animatedColor
}

fun Modifier.m3StateLayer(
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource,
    enabled: Boolean = true,
    baseColor: Color = Color.Unspecified
): Modifier = composed {
    val color = if (baseColor == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurface
    } else {
        baseColor
    }

    val stateLayerColor = rememberM3StateLayerColor(
        interactionSource = interactionSource,
        enabled = enabled,
        baseColor = color
    )

    this.then(Modifier)
}

@Composable
fun M3StateLayer(
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    baseColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit
) {
    val stateLayerColor = rememberM3StateLayerColor(
        interactionSource = interactionSource,
        enabled = enabled,
        baseColor = baseColor
    )

    Box(modifier = modifier) {
        content()
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(stateLayerColor)
        )
    }
}
