package com.cycling.sonic.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
expect fun rememberDominantColor(
    imageUrl: String?,
    filePath: String? = null,
    fallbackColor: Color
): Color
