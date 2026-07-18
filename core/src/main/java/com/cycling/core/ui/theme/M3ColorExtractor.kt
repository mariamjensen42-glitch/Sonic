package com.cycling.core.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL

object M3ColorExtractor {
    suspend fun extractColorFromImageUrl(
        imageUrl: String,
        fallbackColor: ComposeColor = M3SeedColors.Purple
    ): ComposeColor {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val inputStream: InputStream = url.openStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                bitmap?.let { extractDominantColor(it, fallbackColor) } ?: fallbackColor
            } catch (e: Exception) {
                fallbackColor
            }
        }
    }

    fun extractColorFromBitmap(
        bitmap: Bitmap,
        fallbackColor: ComposeColor = M3SeedColors.Purple
    ): ComposeColor {
        return extractDominantColor(bitmap, fallbackColor)
    }

    fun extractColorFromResource(
        context: Context,
        resourceId: Int,
        fallbackColor: ComposeColor = M3SeedColors.Purple
    ): ComposeColor {
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            bitmap?.let { extractDominantColor(it, fallbackColor) } ?: fallbackColor
        } catch (e: Exception) {
            fallbackColor
        }
    }

    private fun extractDominantColor(
        bitmap: Bitmap,
        fallbackColor: ComposeColor
    ): ComposeColor {
        val scaledBitmap = scaleBitmap(bitmap, 100)
        
        return try {
            val palette = Palette.from(scaledBitmap)
                .maximumColorCount(16)
                .generate()
            
            val vibrantColor = palette.vibrantSwatch
            val dominantColor = palette.dominantSwatch
            val mutedColor = palette.mutedSwatch
            
            when {
                vibrantColor != null -> ComposeColor(vibrantColor.rgb)
                dominantColor != null -> ComposeColor(dominantColor.rgb)
                mutedColor != null -> ComposeColor(mutedColor.rgb)
                else -> fallbackColor
            }
        } catch (e: Exception) {
            fallbackColor
        }
    }

    fun extractColorPalette(
        bitmap: Bitmap,
        fallbackColors: List<ComposeColor> = M3SeedColors.MusicPresets.map { it.first }
    ): M3ExtractedPalette {
        val scaledBitmap = scaleBitmap(bitmap, 100)
        
        return try {
            val palette = Palette.from(scaledBitmap)
                .maximumColorCount(24)
                .generate()
            
            M3ExtractedPalette(
                dominant = palette.dominantSwatch?.rgb?.let { ComposeColor(it) }
                    ?: fallbackColors.getOrElse(0) { M3SeedColors.Purple },
                vibrant = palette.vibrantSwatch?.rgb?.let { ComposeColor(it) }
                    ?: fallbackColors.getOrElse(1) { M3SeedColors.Pink },
                darkVibrant = palette.darkVibrantSwatch?.rgb?.let { ComposeColor(it) }
                    ?: fallbackColors.getOrElse(2) { M3SeedColors.Orange },
                lightVibrant = palette.lightVibrantSwatch?.rgb?.let { ComposeColor(it) }
                    ?: fallbackColors.getOrElse(3) { M3SeedColors.Blue },
                muted = palette.mutedSwatch?.rgb?.let { ComposeColor(it) }
                    ?: fallbackColors.getOrElse(4) { M3SeedColors.Teal },
                darkMuted = palette.darkMutedSwatch?.rgb?.let { ComposeColor(it) }
                    ?: fallbackColors.getOrElse(5) { M3SeedColors.Green },
                lightMuted = palette.lightMutedSwatch?.rgb?.let { ComposeColor(it) }
                    ?: fallbackColors.getOrElse(0) { M3SeedColors.Purple }
            )
        } catch (e: Exception) {
            M3ExtractedPalette(
                dominant = fallbackColors.getOrElse(0) { M3SeedColors.Purple },
                vibrant = fallbackColors.getOrElse(1) { M3SeedColors.Pink },
                darkVibrant = fallbackColors.getOrElse(2) { M3SeedColors.Orange },
                lightVibrant = fallbackColors.getOrElse(3) { M3SeedColors.Blue },
                muted = fallbackColors.getOrElse(4) { M3SeedColors.Teal },
                darkMuted = fallbackColors.getOrElse(5) { M3SeedColors.Green },
                lightMuted = fallbackColors.getOrElse(0) { M3SeedColors.Purple }
            )
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        val ratio = maxOf(originalWidth, originalHeight).toFloat() / maxDimension
        
        if (ratio <= 1f) return bitmap
        
        val scaledWidth = (originalWidth / ratio).toInt()
        val scaledHeight = (originalHeight / ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false)
    }
}

data class M3ExtractedPalette(
    val dominant: ComposeColor,
    val vibrant: ComposeColor,
    val darkVibrant: ComposeColor,
    val lightVibrant: ComposeColor,
    val muted: ComposeColor,
    val darkMuted: ComposeColor,
    val lightMuted: ComposeColor
) {
    fun toColorList(): List<ComposeColor> = listOf(
        dominant,
        vibrant,
        darkVibrant,
        lightVibrant,
        muted,
        darkMuted,
        lightMuted
    )
    
    fun getBestSeedColor(): ComposeColor {
        return when {
            vibrant != ComposeColor.Transparent -> vibrant
            dominant != ComposeColor.Transparent -> dominant
            lightVibrant != ComposeColor.Transparent -> lightVibrant
            else -> muted
        }
    }
}

object M3ColorUtils {
    @ColorInt
    fun composeColorToInt(color: ComposeColor): Int {
        return color.toArgb()
    }

    fun intToComposeColor(@ColorInt color: Int): ComposeColor {
        return ComposeColor(color)
    }

    fun blendColors(color1: ComposeColor, color2: ComposeColor, ratio: Float): ComposeColor {
        val inverseRatio = 1f - ratio
        val r = color1.red * inverseRatio + color2.red * ratio
        val g = color1.green * inverseRatio + color2.green * ratio
        val b = color1.blue * inverseRatio + color2.blue * ratio
        val a = color1.alpha * inverseRatio + color2.alpha * ratio
        return ComposeColor(r, g, b, a)
    }

    fun lighten(color: ComposeColor, amount: Float): ComposeColor {
        val hsl = FloatArray(3)
        Color.colorToHSV(color.toArgb(), hsl)
        hsl[2] = (hsl[2] + amount).coerceIn(0f, 1f)
        return ComposeColor(Color.HSVToColor(hsl))
    }

    fun darken(color: ComposeColor, amount: Float): ComposeColor {
        val hsl = FloatArray(3)
        Color.colorToHSV(color.toArgb(), hsl)
        hsl[2] = (hsl[2] - amount).coerceIn(0f, 1f)
        return ComposeColor(Color.HSVToColor(hsl))
    }

    fun saturate(color: ComposeColor, amount: Float): ComposeColor {
        val hsl = FloatArray(3)
        Color.colorToHSV(color.toArgb(), hsl)
        hsl[1] = (hsl[1] + amount).coerceIn(0f, 1f)
        return ComposeColor(Color.HSVToColor(hsl))
    }

    fun desaturate(color: ComposeColor, amount: Float): ComposeColor {
        val hsl = FloatArray(3)
        Color.colorToHSV(color.toArgb(), hsl)
        hsl[1] = (hsl[1] - amount).coerceIn(0f, 1f)
        return ComposeColor(Color.HSVToColor(hsl))
    }

    fun adjustHue(color: ComposeColor, degrees: Float): ComposeColor {
        val hsl = FloatArray(3)
        Color.colorToHSV(color.toArgb(), hsl)
        hsl[0] = (hsl[0] + degrees) % 360f
        if (hsl[0] < 0) hsl[0] += 360f
        return ComposeColor(Color.HSVToColor(hsl))
    }

    fun getComplementaryColor(color: ComposeColor): ComposeColor {
        return adjustHue(color, 180f)
    }

    fun getAnalogousColors(color: ComposeColor): List<ComposeColor> {
        return listOf(
            adjustHue(color, -30f),
            color,
            adjustHue(color, 30f)
        )
    }

    fun getTriadicColors(color: ComposeColor): List<ComposeColor> {
        return listOf(
            color,
            adjustHue(color, 120f),
            adjustHue(color, 240f)
        )
    }

    fun getSplitComplementaryColors(color: ComposeColor): List<ComposeColor> {
        return listOf(
            color,
            adjustHue(color, 150f),
            adjustHue(color, 210f)
        )
    }

    fun calculateLuminance(color: ComposeColor): Double {
        val r = color.red.toDouble()
        val g = color.green.toDouble()
        val b = color.blue.toDouble()
        
        fun channel(c: Double): Double {
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }

    fun calculateContrastRatio(color1: ComposeColor, color2: ComposeColor): Double {
        val l1 = calculateLuminance(color1)
        val l2 = calculateLuminance(color2)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    fun meetsWCAGAA(foreground: ComposeColor, background: ComposeColor): Boolean {
        return calculateContrastRatio(foreground, background) >= 4.5
    }

    fun meetsWCAGAAA(foreground: ComposeColor, background: ComposeColor): Boolean {
        return calculateContrastRatio(foreground, background) >= 7.0
    }

    private fun Double.pow(exp: Double): Double = Math.pow(this, exp)
}
