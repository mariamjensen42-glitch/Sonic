package com.cycling.core.ui.theme

import androidx.compose.ui.graphics.Color

object M3Colors {
    val Primary = Color(0xFF6750A4)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFEADDFF)
    val OnPrimaryContainer = Color(0xFF21005D)
    
    val Secondary = Color(0xFF625B71)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFE8DEF8)
    val OnSecondaryContainer = Color(0xFF1D192B)
    
    val Tertiary = Color(0xFF7D5260)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFFFFD8E4)
    val OnTertiaryContainer = Color(0xFF31111D)
    
    val Error = Color(0xFFB3261E)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFF9DEDC)
    val OnErrorContainer = Color(0xFF410E0B)
    
    val Background = Color(0xFFFFFBFE)
    val OnBackground = Color(0xFF1C1B1F)
    
    val Surface = Color(0xFFFFFBFE)
    val OnSurface = Color(0xFF1C1B1F)
    
    val SurfaceVariant = Color(0xFFE7E0EC)
    val OnSurfaceVariant = Color(0xFF49454F)
    
    val Outline = Color(0xFF79747E)
    val OutlineVariant = Color(0xFFCAC4D0)
    
    val InverseSurface = Color(0xFF313033)
    val InverseOnSurface = Color(0xFFF4EFF4)
    val InversePrimary = Color(0xFFD0BCFF)
    
    val SurfaceDim = Color(0xFFDED8E1)
    val SurfaceBright = Color(0xFFFFFBFE)
    
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFF7F2FA)
    val SurfaceContainer = Color(0xFFF3EDF7)
    val SurfaceContainerHigh = Color(0xFFECE6F0)
    val SurfaceContainerHighest = Color(0xFFE6E0E9)
    
    val Scrim = Color(0xFF000000)
}

object M3DarkColors {
    val Primary = Color(0xFFD0BCFF)
    val OnPrimary = Color(0xFF381E72)
    val PrimaryContainer = Color(0xFF4F378B)
    val OnPrimaryContainer = Color(0xFFEADDFF)
    
    val Secondary = Color(0xFFCCC2DC)
    val OnSecondary = Color(0xFF332D41)
    val SecondaryContainer = Color(0xFF4A4458)
    val OnSecondaryContainer = Color(0xFFE8DEF8)
    
    val Tertiary = Color(0xFFEFB8C8)
    val OnTertiary = Color(0xFF492532)
    val TertiaryContainer = Color(0xFF633B48)
    val OnTertiaryContainer = Color(0xFFFFD8E4)
    
    val Error = Color(0xFFF2B8B5)
    val OnError = Color(0xFF601410)
    val ErrorContainer = Color(0xFF8C1D18)
    val OnErrorContainer = Color(0xFFF9DEDC)
    
    val Background = Color(0xFF1C1B1F)
    val OnBackground = Color(0xFFE6E1E5)
    
    val Surface = Color(0xFF1C1B1F)
    val OnSurface = Color(0xFFE6E1E5)
    
    val SurfaceVariant = Color(0xFF49454F)
    val OnSurfaceVariant = Color(0xFFCAC4D0)
    
    val Outline = Color(0xFF938F99)
    val OutlineVariant = Color(0xFF49454F)
    
    val InverseSurface = Color(0xFFE6E1E5)
    val InverseOnSurface = Color(0xFF313033)
    val InversePrimary = Color(0xFF6750A4)
    
    val SurfaceDim = Color(0xFF141218)
    val SurfaceBright = Color(0xFF3B383E)
    
    val SurfaceContainerLowest = Color(0xFF0F0D13)
    val SurfaceContainerLow = Color(0xFF1D1B20)
    val SurfaceContainer = Color(0xFF211F26)
    val SurfaceContainerHigh = Color(0xFF2B2930)
    val SurfaceContainerHighest = Color(0xFF36343B)
    
    val Scrim = Color(0xFF000000)
}

object M3SemanticColors {
    val Pink = Color(0xFFFF375F)
    val Orange = Color(0xFFFF9F0A)
    val Yellow = Color(0xFFFFD60A)
    val Green = Color(0xFF30D158)
    val Teal = Color(0xFF64D2FF)
    val Blue = Color(0xFF0A84FF)
    val Indigo = Color(0xFF5E5CE6)
    val Purple = Color(0xFFBF5AF2)
    val Red = Color(0xFFFF453A)

    val gradientPinkPurple = listOf(Pink, Purple)
    val gradientBlueTeal = listOf(Blue, Teal)
    val gradientGreenTeal = listOf(Green, Teal)
    val gradientOrangePink = listOf(Orange, Pink)
}

object M3GradientColors {
    val pinkToPurple = listOf(M3SemanticColors.Pink, M3SemanticColors.Purple)
    val blueToTeal = listOf(M3SemanticColors.Blue, M3SemanticColors.Teal)
    val greenToTeal = listOf(M3SemanticColors.Green, M3SemanticColors.Teal)
    val orangeToPink = listOf(M3SemanticColors.Orange, M3SemanticColors.Pink)
    val indigoToPurple = listOf(M3SemanticColors.Indigo, M3SemanticColors.Purple)
    val redToOrange = listOf(M3SemanticColors.Red, M3SemanticColors.Orange)

    fun createGradient(
        startColor: Color,
        endColor: Color,
        steps: Int = 2
    ): List<Color> {
        if (steps <= 2) return listOf(startColor, endColor)

        val gradient = mutableListOf<Color>()
        for (i in 0 until steps) {
            val fraction = i.toFloat() / (steps - 1)
            gradient.add(
                Color(
                    red = startColor.red + (endColor.red - startColor.red) * fraction,
                    green = startColor.green + (endColor.green - startColor.green) * fraction,
                    blue = startColor.blue + (endColor.blue - startColor.blue) * fraction,
                    alpha = startColor.alpha + (endColor.alpha - startColor.alpha) * fraction
                )
            )
        }
        return gradient
    }

    fun withOpacity(color: Color, opacity: Float): Color {
        return color.copy(alpha = opacity)
    }
}
