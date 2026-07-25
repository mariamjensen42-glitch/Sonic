package com.cycling.sonic.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
actual fun rememberDominantColor(imageUrl: String?, filePath: String?, fallbackColor: Color): Color {
    return fallbackColor
}
