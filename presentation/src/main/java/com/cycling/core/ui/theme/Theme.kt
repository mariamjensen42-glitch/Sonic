package com.cycling.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Spotify-style dark color scheme.
 */
private val SpotifyDarkColorScheme = darkColorScheme(
    primary = SpotifyColors.Accent,
    onPrimary = Color.White,
    primaryContainer = SpotifyColors.Accent.copy(alpha = 0.18f),
    onPrimaryContainer = SpotifyColors.Accent,
    secondary = SpotifyColors.PlayGreen,
    onSecondary = Color.Black,
    secondaryContainer = SpotifyColors.PlayGreen.copy(alpha = 0.18f),
    onSecondaryContainer = SpotifyColors.PlayGreen,
    tertiary = SpotifyColors.Accent.copy(alpha = 0.8f),
    onTertiary = Color.White,
    background = SpotifyColors.Background,
    onBackground = SpotifyColors.TextPrimary,
    surface = SpotifyColors.Surface,
    onSurface = SpotifyColors.TextPrimary,
    surfaceVariant = SpotifyColors.SurfaceVariant,
    onSurfaceVariant = SpotifyColors.TextSecondary,
    outline = SpotifyColors.Divider,
    outlineVariant = SpotifyColors.Divider,
    error = Color(0xFFCF6679),
    onError = Color.Black,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceDim = SpotifyColors.Background,
    surfaceBright = SpotifyColors.Surface,
    surfaceContainerLowest = Color(0xFF050508),
    surfaceContainerLow = SpotifyColors.GridBackground,
    surfaceContainer = SpotifyColors.Surface,
    surfaceContainerHigh = SpotifyColors.SurfaceVariant,
    surfaceContainerHighest = Color(0xFF2E2E33),
    scrim = Color.Black,
    inverseSurface = SpotifyColors.TextPrimary,
    inverseOnSurface = SpotifyColors.Background,
    inversePrimary = SpotifyColors.Accent
)

@Composable
fun SpotifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Spotify is always dark — ignore darkTheme param
    val colorScheme = SpotifyDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = SpotifyColors.Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = M3Typography,
        content = content
    )
}
