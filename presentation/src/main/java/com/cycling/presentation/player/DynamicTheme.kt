package com.cycling.presentation.player

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DominantColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

suspend fun extractDominantColors(bitmap: Bitmap): DominantColors {
    return withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap)
            .maximumColorCount(16)
            .generate()

        val vibrantSwatch = palette.vibrantSwatch
        val darkVibrantSwatch = palette.darkVibrantSwatch
        val mutedSwatch = palette.mutedSwatch
        val darkMutedSwatch = palette.darkMutedSwatch
        val lightVibrantSwatch = palette.lightVibrantSwatch

        val primary = vibrantSwatch?.let { Color(it.rgb) }
            ?: darkVibrantSwatch?.let { Color(it.rgb) }
            ?: mutedSwatch?.let { Color(it.rgb) }
            ?: Color(0xFF1a1a2e)

        val secondary = darkVibrantSwatch?.let { Color(it.rgb) }
            ?: darkMutedSwatch?.let { Color(it.rgb) }
            ?: Color(0xFF16213e)

        val tertiary = mutedSwatch?.let { Color(it.rgb) }
            ?: lightVibrantSwatch?.let { Color(it.rgb) }
            ?: Color(0xFF0f0f23)

        DominantColors(primary, secondary, tertiary)
    }
}

@Composable
fun rememberDominantColors(
    imageUrl: String?,
    defaultColors: DominantColors = DominantColors(
        primary = Color(0xFF1a1a2e),
        secondary = Color(0xFF16213e),
        tertiary = Color(0xFF0f0f23)
    )
): DominantColors {
    var dominantColors by remember { mutableStateOf(defaultColors) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(imageUrl) {
        if (imageUrl != null) {
            try {
                val loader = ImageLoader.Builder(context).build()
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()

                val result = loader.execute(request) as? SuccessResult
                val bitmap = result?.image?.toBitmap()

                if (bitmap != null) {
                    dominantColors = extractDominantColors(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            dominantColors = defaultColors
        }
    }

    return dominantColors
}
