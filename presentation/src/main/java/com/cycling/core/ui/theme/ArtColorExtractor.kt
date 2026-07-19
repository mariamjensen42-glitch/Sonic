package com.cycling.core.ui.theme

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts a dominant color from an album/artist artwork URI.
 *
 * Uses Coil to load the bitmap — handles all URI types (content://, file://, http://)
 * that the app's AsyncImage already supports.
 */
object ArtColorExtractor {

    suspend fun extractColorFromImageUrl(
        context: Context,
        imageUrl: String,
        fallbackColor: Color = SpotifyColors.Background
    ): Color {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = loadBitmapViaCoil(context, imageUrl)
                bitmap?.let { extractColorFromBitmap(it, fallbackColor) } ?: fallbackColor
            } catch (e: Exception) {
                fallbackColor
            }
        }
    }

    private suspend fun loadBitmapViaCoil(context: Context, imageUrl: String): Bitmap? {
        val loader = context.imageLoader
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(Size(50, 50))       // scale down for performance
            .memoryCacheKey("artcolor_$imageUrl")
            .build()

        val result = loader.execute(request)
        return result.drawable?.toBitmap()
    }

    private fun extractColorFromBitmap(bitmap: Bitmap, fallbackColor: Color): Color {
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()

        // Match reference project: mutedSwatch first
        val rgb = palette?.mutedSwatch?.rgb
            ?: palette?.darkVibrantSwatch?.rgb
            ?: palette?.vibrantSwatch?.rgb
            ?: palette?.dominantSwatch?.rgb
            ?: return fallbackColor

        return Color(rgb or (0xFF shl 24))
    }
}
