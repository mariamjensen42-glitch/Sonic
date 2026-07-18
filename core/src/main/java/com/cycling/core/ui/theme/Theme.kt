package com.cycling.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = M3Colors.Primary,
    onPrimary = M3Colors.OnPrimary,
    primaryContainer = M3Colors.PrimaryContainer,
    onPrimaryContainer = M3Colors.OnPrimaryContainer,
    secondary = M3Colors.Secondary,
    onSecondary = M3Colors.OnSecondary,
    secondaryContainer = M3Colors.SecondaryContainer,
    onSecondaryContainer = M3Colors.OnSecondaryContainer,
    tertiary = M3Colors.Tertiary,
    onTertiary = M3Colors.OnTertiary,
    tertiaryContainer = M3Colors.TertiaryContainer,
    onTertiaryContainer = M3Colors.OnTertiaryContainer,
    error = M3Colors.Error,
    onError = M3Colors.OnError,
    errorContainer = M3Colors.ErrorContainer,
    onErrorContainer = M3Colors.OnErrorContainer,
    background = M3Colors.Background,
    onBackground = M3Colors.OnBackground,
    surface = M3Colors.Surface,
    onSurface = M3Colors.OnSurface,
    surfaceVariant = M3Colors.SurfaceVariant,
    onSurfaceVariant = M3Colors.OnSurfaceVariant,
    outline = M3Colors.Outline,
    outlineVariant = M3Colors.OutlineVariant,
    inverseSurface = M3Colors.InverseSurface,
    inverseOnSurface = M3Colors.InverseOnSurface,
    inversePrimary = M3Colors.InversePrimary,
    surfaceDim = M3Colors.SurfaceDim,
    surfaceBright = M3Colors.SurfaceBright,
    surfaceContainerLowest = M3Colors.SurfaceContainerLowest,
    surfaceContainerLow = M3Colors.SurfaceContainerLow,
    surfaceContainer = M3Colors.SurfaceContainer,
    surfaceContainerHigh = M3Colors.SurfaceContainerHigh,
    surfaceContainerHighest = M3Colors.SurfaceContainerHighest,
    scrim = M3Colors.Scrim
)

private val DarkColorScheme = darkColorScheme(
    primary = M3DarkColors.Primary,
    onPrimary = M3DarkColors.OnPrimary,
    primaryContainer = M3DarkColors.PrimaryContainer,
    onPrimaryContainer = M3DarkColors.OnPrimaryContainer,
    secondary = M3DarkColors.Secondary,
    onSecondary = M3DarkColors.OnSecondary,
    secondaryContainer = M3DarkColors.SecondaryContainer,
    onSecondaryContainer = M3DarkColors.OnSecondaryContainer,
    tertiary = M3DarkColors.Tertiary,
    onTertiary = M3DarkColors.OnTertiary,
    tertiaryContainer = M3DarkColors.TertiaryContainer,
    onTertiaryContainer = M3DarkColors.OnTertiaryContainer,
    error = M3DarkColors.Error,
    onError = M3DarkColors.OnError,
    errorContainer = M3DarkColors.ErrorContainer,
    onErrorContainer = M3DarkColors.OnErrorContainer,
    background = M3DarkColors.Background,
    onBackground = M3DarkColors.OnBackground,
    surface = M3DarkColors.Surface,
    onSurface = M3DarkColors.OnSurface,
    surfaceVariant = M3DarkColors.SurfaceVariant,
    onSurfaceVariant = M3DarkColors.OnSurfaceVariant,
    outline = M3DarkColors.Outline,
    outlineVariant = M3DarkColors.OutlineVariant,
    inverseSurface = M3DarkColors.InverseSurface,
    inverseOnSurface = M3DarkColors.InverseOnSurface,
    inversePrimary = M3DarkColors.InversePrimary,
    surfaceDim = M3DarkColors.SurfaceDim,
    surfaceBright = M3DarkColors.SurfaceBright,
    surfaceContainerLowest = M3DarkColors.SurfaceContainerLowest,
    surfaceContainerLow = M3DarkColors.SurfaceContainerLow,
    surfaceContainer = M3DarkColors.SurfaceContainer,
    surfaceContainerHigh = M3DarkColors.SurfaceContainerHigh,
    surfaceContainerHighest = M3DarkColors.SurfaceContainerHighest,
    scrim = M3DarkColors.Scrim
)

@Composable
fun M3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = M3Typography,
        content = content
    )
}
