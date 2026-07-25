package com.cycling.sonic.core.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val colorCache = LruCache<String, Color>(64)

private fun cacheKey(imageUrl: String?, filePath: String?): String =
    imageUrl ?: filePath ?: ""

@Composable
actual fun rememberDominantColor(
    imageUrl: String?,
    filePath: String?,
    fallbackColor: Color
): Color {
    if (imageUrl == null && filePath == null) return fallbackColor
    val context = LocalContext.current
    val key = remember(imageUrl, filePath) { cacheKey(imageUrl, filePath) }

    val cached = remember { colorCache.get(key) }
    var dominantColor by remember { mutableStateOf(cached ?: fallbackColor) }

    LaunchedEffect(imageUrl, filePath) {
        if (colorCache.get(key) != null) return@LaunchedEffect
        dominantColor = withContext(Dispatchers.IO) {
            try {
                val bitmap = tryLoadBitmapFromUri(context, imageUrl)
                    ?: filePath?.let { getAlbumArtFromFile(it) }
                if (bitmap == null) return@withContext fallbackColor
                val palette = Palette.from(bitmap).maximumColorCount(16).generate()
                val swatch = palette.vibrantSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.mutedSwatch
                    ?: palette.dominantSwatch
                val color = if (swatch != null) Color(swatch.rgb or (0xFF shl 24)) else fallbackColor
                colorCache.put(key, color)
                color
            } catch (_: Exception) {
                fallbackColor
            }
        }
    }
    return dominantColor
}

private fun tryLoadBitmapFromUri(context: Context, uriString: String?): Bitmap? {
    if (uriString == null) return null
    return try {
        val uri = android.net.Uri.parse(uriString)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
                    .also { if (!it.isRecycled) return it }
            } catch (_: Exception) { }
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (_: Exception) {
        null
    }
}

private fun getAlbumArtFromFile(filePath: String): Bitmap? {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(filePath)
        val data = retriever.embeddedPicture ?: return null
        return BitmapFactory.decodeByteArray(data, 0, data.size)
    } catch (_: Exception) {
        return null
    } finally {
        retriever.release()
    }
}
