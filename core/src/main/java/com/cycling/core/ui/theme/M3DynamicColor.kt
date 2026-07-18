package com.cycling.core.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.annotation.ColorInt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class M3ContrastLevel {
    Default,
    Medium,
    High
}

enum class M3ColorStyle {
    TonalSpot,
    Neutral,
    Vibrant,
    Expressive,
    Rainbow,
    FruitSalad,
    Monochrome,
    Fidelity,
    Content
}

object M3DynamicColorDefaults {
    val DefaultSeedColor = Color(0xFF6750A4)

    val SemanticColors = listOf(
        "error", "onError", "errorContainer", "onErrorContainer"
    )
}

@Composable
fun M3DynamicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    seedColor: Color? = null,
    contrastLevel: M3ContrastLevel = M3ContrastLevel.Default,
    colorStyle: M3ColorStyle = M3ColorStyle.TonalSpot,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val colorScheme = remember(darkTheme, dynamicColor, seedColor, contrastLevel, colorStyle) {
        when {
            seedColor != null -> {
                createColorSchemeFromSeed(
                    context = context,
                    seedColor = seedColor,
                    darkTheme = darkTheme,
                    contrastLevel = contrastLevel,
                    colorStyle = colorStyle
                )
            }
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                applyContrastLevel(
                    scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context),
                    contrastLevel = contrastLevel,
                    darkTheme = darkTheme
                )
            }
            else -> {
                applyContrastLevel(
                    scheme = if (darkTheme) createDarkColorScheme() else createLightColorScheme(),
                    contrastLevel = contrastLevel,
                    darkTheme = darkTheme
                )
            }
        }
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

@Composable
fun M3ExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    seedColor: Color? = null,
    contrastLevel: M3ContrastLevel = M3ContrastLevel.Default,
    content: @Composable () -> Unit
) {
    M3DynamicTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        seedColor = seedColor,
        contrastLevel = contrastLevel,
        colorStyle = M3ColorStyle.Expressive,
        content = content
    )
}

private fun createColorSchemeFromSeed(
    context: Context,
    seedColor: Color,
    darkTheme: Boolean,
    contrastLevel: M3ContrastLevel,
    colorStyle: M3ColorStyle
): ColorScheme {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val baseScheme = if (darkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
        applySeedColorAndContrast(baseScheme, seedColor, contrastLevel, darkTheme)
    } else {
        val baseScheme = if (darkTheme) {
            createDarkColorScheme()
        } else {
            createLightColorScheme()
        }
        applySeedColorAndContrast(baseScheme, seedColor, contrastLevel, darkTheme)
    }
}

private fun applySeedColorAndContrast(
    baseScheme: ColorScheme,
    seedColor: Color,
    contrastLevel: M3ContrastLevel,
    darkTheme: Boolean
): ColorScheme {
    val harmonizedColors = harmonizeWithSeed(baseScheme, seedColor, darkTheme)
    return applyContrastLevel(harmonizedColors, contrastLevel, darkTheme)
}

private fun harmonizeWithSeed(
    scheme: ColorScheme,
    seedColor: Color,
    darkTheme: Boolean
): ColorScheme {
    val harmonizer = ColorHarmonizer(seedColor)
    
    return if (darkTheme) {
        darkColorScheme(
            primary = harmonizer.harmonizePrimary(scheme.primary, darkTheme),
            onPrimary = harmonizer.harmonizeOnPrimary(scheme.onPrimary, darkTheme),
            primaryContainer = harmonizer.harmonizePrimaryContainer(scheme.primaryContainer, darkTheme),
            onPrimaryContainer = harmonizer.harmonizeOnPrimaryContainer(scheme.onPrimaryContainer, darkTheme),
            secondary = harmonizer.harmonizeSecondary(scheme.secondary, darkTheme),
            onSecondary = harmonizer.harmonizeOnSecondary(scheme.onSecondary, darkTheme),
            secondaryContainer = harmonizer.harmonizeSecondaryContainer(scheme.secondaryContainer, darkTheme),
            onSecondaryContainer = harmonizer.harmonizeOnSecondaryContainer(scheme.onSecondaryContainer, darkTheme),
            tertiary = harmonizer.harmonizeTertiary(scheme.tertiary, darkTheme),
            onTertiary = harmonizer.harmonizeOnTertiary(scheme.onTertiary, darkTheme),
            tertiaryContainer = harmonizer.harmonizeTertiaryContainer(scheme.tertiaryContainer, darkTheme),
            onTertiaryContainer = harmonizer.harmonizeOnTertiaryContainer(scheme.onTertiaryContainer, darkTheme),
            error = scheme.error,
            onError = scheme.onError,
            errorContainer = scheme.errorContainer,
            onErrorContainer = scheme.onErrorContainer,
            background = scheme.background,
            onBackground = scheme.onBackground,
            surface = scheme.surface,
            onSurface = scheme.onSurface,
            surfaceVariant = harmonizer.harmonizeSurfaceVariant(scheme.surfaceVariant, darkTheme),
            onSurfaceVariant = scheme.onSurfaceVariant,
            outline = harmonizer.harmonizeOutline(scheme.outline, darkTheme),
            outlineVariant = harmonizer.harmonizeOutlineVariant(scheme.outlineVariant, darkTheme),
            inverseSurface = scheme.inverseSurface,
            inverseOnSurface = scheme.inverseOnSurface,
            inversePrimary = harmonizer.harmonizeInversePrimary(scheme.inversePrimary, darkTheme),
            surfaceDim = scheme.surfaceDim,
            surfaceBright = scheme.surfaceBright,
            surfaceContainerLowest = scheme.surfaceContainerLowest,
            surfaceContainerLow = scheme.surfaceContainerLow,
            surfaceContainer = scheme.surfaceContainer,
            surfaceContainerHigh = scheme.surfaceContainerHigh,
            surfaceContainerHighest = scheme.surfaceContainerHighest,
            scrim = scheme.scrim
        )
    } else {
        lightColorScheme(
            primary = harmonizer.harmonizePrimary(scheme.primary, darkTheme),
            onPrimary = harmonizer.harmonizeOnPrimary(scheme.onPrimary, darkTheme),
            primaryContainer = harmonizer.harmonizePrimaryContainer(scheme.primaryContainer, darkTheme),
            onPrimaryContainer = harmonizer.harmonizeOnPrimaryContainer(scheme.onPrimaryContainer, darkTheme),
            secondary = harmonizer.harmonizeSecondary(scheme.secondary, darkTheme),
            onSecondary = harmonizer.harmonizeOnSecondary(scheme.onSecondary, darkTheme),
            secondaryContainer = harmonizer.harmonizeSecondaryContainer(scheme.secondaryContainer, darkTheme),
            onSecondaryContainer = harmonizer.harmonizeOnSecondaryContainer(scheme.onSecondaryContainer, darkTheme),
            tertiary = harmonizer.harmonizeTertiary(scheme.tertiary, darkTheme),
            onTertiary = harmonizer.harmonizeOnTertiary(scheme.onTertiary, darkTheme),
            tertiaryContainer = harmonizer.harmonizeTertiaryContainer(scheme.tertiaryContainer, darkTheme),
            onTertiaryContainer = harmonizer.harmonizeOnTertiaryContainer(scheme.onTertiaryContainer, darkTheme),
            error = scheme.error,
            onError = scheme.onError,
            errorContainer = scheme.errorContainer,
            onErrorContainer = scheme.onErrorContainer,
            background = scheme.background,
            onBackground = scheme.onBackground,
            surface = scheme.surface,
            onSurface = scheme.onSurface,
            surfaceVariant = harmonizer.harmonizeSurfaceVariant(scheme.surfaceVariant, darkTheme),
            onSurfaceVariant = scheme.onSurfaceVariant,
            outline = harmonizer.harmonizeOutline(scheme.outline, darkTheme),
            outlineVariant = harmonizer.harmonizeOutlineVariant(scheme.outlineVariant, darkTheme),
            inverseSurface = scheme.inverseSurface,
            inverseOnSurface = scheme.inverseOnSurface,
            inversePrimary = harmonizer.harmonizeInversePrimary(scheme.inversePrimary, darkTheme),
            surfaceDim = scheme.surfaceDim,
            surfaceBright = scheme.surfaceBright,
            surfaceContainerLowest = scheme.surfaceContainerLowest,
            surfaceContainerLow = scheme.surfaceContainerLow,
            surfaceContainer = scheme.surfaceContainer,
            surfaceContainerHigh = scheme.surfaceContainerHigh,
            surfaceContainerHighest = scheme.surfaceContainerHighest,
            scrim = scheme.scrim
        )
    }
}

private fun applyContrastLevel(
    scheme: ColorScheme,
    contrastLevel: M3ContrastLevel,
    darkTheme: Boolean
): ColorScheme {
    return when (contrastLevel) {
        M3ContrastLevel.Default -> scheme
        M3ContrastLevel.Medium -> applyMediumContrast(scheme, darkTheme)
        M3ContrastLevel.High -> applyHighContrast(scheme, darkTheme)
    }
}

private fun applyMediumContrast(scheme: ColorScheme, darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        scheme.copy(
            onSurface = increaseLuminance(scheme.onSurface, 0.1f),
            onSurfaceVariant = increaseLuminance(scheme.onSurfaceVariant, 0.1f),
            outline = increaseLuminance(scheme.outline, 0.1f),
            primary = increaseLuminance(scheme.primary, 0.05f)
        )
    } else {
        scheme.copy(
            onSurface = decreaseLuminance(scheme.onSurface, 0.1f),
            onSurfaceVariant = decreaseLuminance(scheme.onSurfaceVariant, 0.1f),
            outline = decreaseLuminance(scheme.outline, 0.1f),
            primary = decreaseLuminance(scheme.primary, 0.05f)
        )
    }
}

private fun applyHighContrast(scheme: ColorScheme, darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        scheme.copy(
            onSurface = Color.White,
            onSurfaceVariant = Color.White.copy(alpha = 0.9f),
            outline = Color.White.copy(alpha = 0.7f),
            primary = Color.White,
            onPrimary = Color.Black,
            secondary = Color.White.copy(alpha = 0.9f),
            onSecondary = Color.Black
        )
    } else {
        scheme.copy(
            onSurface = Color.Black,
            onSurfaceVariant = Color.Black.copy(alpha = 0.9f),
            outline = Color.Black.copy(alpha = 0.7f),
            primary = Color.Black,
            onPrimary = Color.White,
            secondary = Color.Black.copy(alpha = 0.9f),
            onSecondary = Color.White
        )
    }
}

private fun increaseLuminance(color: Color, amount: Float): Color {
    val hsl = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsl)
    hsl[2] = (hsl[2] + amount).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsl))
}

private fun decreaseLuminance(color: Color, amount: Float): Color {
    val hsl = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsl)
    hsl[2] = (hsl[2] - amount).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsl))
}

private class ColorHarmonizer(private val seedColor: Color) {
    private val seedHue = getHue(seedColor)
    
    fun harmonizePrimary(color: Color, darkTheme: Boolean): Color {
        return shiftHue(color, seedHue)
    }
    
    fun harmonizeOnPrimary(color: Color, darkTheme: Boolean): Color {
        return if (darkTheme) Color(0xFF1A1A1A) else Color.White
    }
    
    fun harmonizePrimaryContainer(color: Color, darkTheme: Boolean): Color {
        val shifted = shiftHue(color, seedHue)
        return if (darkTheme) {
            decreaseLuminance(shifted, 0.2f)
        } else {
            increaseLuminance(shifted, 0.3f)
        }
    }
    
    fun harmonizeOnPrimaryContainer(color: Color, darkTheme: Boolean): Color {
        return if (darkTheme) Color.White else Color(0xFF1A1A1A)
    }
    
    fun harmonizeSecondary(color: Color, darkTheme: Boolean): Color {
        return shiftHue(color, seedHue + 30f)
    }
    
    fun harmonizeOnSecondary(color: Color, darkTheme: Boolean): Color {
        return if (darkTheme) Color(0xFF1A1A1A) else Color.White
    }
    
    fun harmonizeSecondaryContainer(color: Color, darkTheme: Boolean): Color {
        val shifted = shiftHue(color, seedHue + 30f)
        return if (darkTheme) {
            decreaseLuminance(shifted, 0.2f)
        } else {
            increaseLuminance(shifted, 0.3f)
        }
    }
    
    fun harmonizeOnSecondaryContainer(color: Color, darkTheme: Boolean): Color {
        return if (darkTheme) Color.White else Color(0xFF1A1A1A)
    }
    
    fun harmonizeTertiary(color: Color, darkTheme: Boolean): Color {
        return shiftHue(color, seedHue + 60f)
    }
    
    fun harmonizeOnTertiary(color: Color, darkTheme: Boolean): Color {
        return if (darkTheme) Color(0xFF1A1A1A) else Color.White
    }
    
    fun harmonizeTertiaryContainer(color: Color, darkTheme: Boolean): Color {
        val shifted = shiftHue(color, seedHue + 60f)
        return if (darkTheme) {
            decreaseLuminance(shifted, 0.2f)
        } else {
            increaseLuminance(shifted, 0.3f)
        }
    }
    
    fun harmonizeOnTertiaryContainer(color: Color, darkTheme: Boolean): Color {
        return if (darkTheme) Color.White else Color(0xFF1A1A1A)
    }
    
    fun harmonizeSurfaceVariant(color: Color, darkTheme: Boolean): Color {
        val shifted = shiftHue(color, seedHue)
        return if (darkTheme) {
            decreaseLuminance(shifted, 0.1f)
        } else {
            shifted
        }
    }
    
    fun harmonizeOutline(color: Color, darkTheme: Boolean): Color {
        val shifted = shiftHue(color, seedHue)
        return if (darkTheme) {
            increaseLuminance(shifted, 0.1f)
        } else {
            shifted
        }
    }
    
    fun harmonizeOutlineVariant(color: Color, darkTheme: Boolean): Color {
        val shifted = shiftHue(color, seedHue)
        return if (darkTheme) {
            increaseLuminance(shifted, 0.05f)
        } else {
            shifted
        }
    }
    
    fun harmonizeInversePrimary(color: Color, darkTheme: Boolean): Color {
        return shiftHue(color, seedHue)
    }
    
    private fun shiftHue(color: Color, targetHue: Float): Color {
        val hsl = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsl)
        hsl[0] = targetHue % 360f
        return Color(android.graphics.Color.HSVToColor(hsl))
    }
    
    private fun getHue(color: Color): Float {
        val hsl = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsl)
        return hsl[0]
    }
}

private fun createLightColorScheme(): ColorScheme {
    return lightColorScheme(
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
}

private fun createDarkColorScheme(): ColorScheme {
    return darkColorScheme(
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
}

object M3SeedColors {
    val Purple = Color(0xFF6750A4)
    val Pink = Color(0xFFFF375F)
    val Orange = Color(0xFFFF9F0A)
    val Yellow = Color(0xFFFFD60A)
    val Green = Color(0xFF30D158)
    val Teal = Color(0xFF64D2FF)
    val Blue = Color(0xFF0A84FF)
    val Indigo = Color(0xFF5E5CE6)
    val Red = Color(0xFFFF453A)
    
    val MusicPresets = listOf(
        Purple to "Purple",
        Pink to "Pink",
        Orange to "Orange",
        Blue to "Blue",
        Teal to "Teal",
        Green to "Green"
    )
}
