package com.cycling.core.ui.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import com.cycling.core.ui.theme.M3Duration

object M3EasingTokens {
    val standard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val emphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val standardDecelerate: Easing = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val standardAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)
}

object M3DurationTokens {
    val short = M3Duration.short
    val medium = M3Duration.medium
    val long = M3Duration.long
    val extraLong = M3Duration.extraLong
}

fun <T> emphasizedTween(
    durationMillis: Int = M3DurationTokens.medium,
    delayMillis: Int = 0
): FiniteAnimationSpec<T> = tween(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    easing = M3EasingTokens.emphasized
)

fun <T> emphasizedDecelerateTween(
    durationMillis: Int = M3DurationTokens.medium,
    delayMillis: Int = 0
): FiniteAnimationSpec<T> = tween(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    easing = M3EasingTokens.emphasizedDecelerate
)

fun <T> emphasizedAccelerateTween(
    durationMillis: Int = M3DurationTokens.short,
    delayMillis: Int = 0
): FiniteAnimationSpec<T> = tween(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    easing = M3EasingTokens.emphasizedAccelerate
)

fun <T> standardTween(
    durationMillis: Int = M3DurationTokens.medium,
    delayMillis: Int = 0
): FiniteAnimationSpec<T> = tween(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    easing = M3EasingTokens.standard
)

fun <T> standardDecelerateTween(
    durationMillis: Int = M3DurationTokens.medium,
    delayMillis: Int = 0
): FiniteAnimationSpec<T> = tween(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    easing = M3EasingTokens.standardDecelerate
)

fun <T> standardAccelerateTween(
    durationMillis: Int = M3DurationTokens.short,
    delayMillis: Int = 0
): FiniteAnimationSpec<T> = tween(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    easing = M3EasingTokens.standardAccelerate
)
