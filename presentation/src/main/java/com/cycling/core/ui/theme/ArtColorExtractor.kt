package com.cycling.core.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ArtColorExtractor {

    /**
     * 从本地音乐文件路径提取封面主色。
     * 使用 MediaMetadataRetriever 获取嵌入的封面图片，再用 Palette 取色。
     */
    suspend fun extractColorFromMusicFile(
        filePath: String,
        fallbackColor: Color = SpotifyColors.Background
    ): Color {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = getAlbumArtFromFile(filePath) ?: return@withContext fallbackColor
                val palette = Palette.from(bitmap).generate()
                val swatch = palette.vibrantSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.mutedSwatch
                    ?: palette.dominantSwatch
                if (swatch != null) Color(swatch.rgb or (0xFF shl 24)) else fallbackColor
            } catch (e: Exception) {
                fallbackColor
            }
        }
    }

    /** 保留给其他页面使用的旧方法，通过 URI 加载图片 */
    suspend fun extractColorFromImageUrl(
        context: Context,
        imageUrl: String,
        fallbackColor: Color = SpotifyColors.Background
    ): Color {
        return withContext(Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(imageUrl)
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap == null) return@withContext fallbackColor
                val palette = Palette.from(bitmap).generate()
                val swatch = palette.vibrantSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.mutedSwatch
                    ?: palette.dominantSwatch
                if (swatch != null) Color(swatch.rgb or (0xFF shl 24)) else fallbackColor
            } catch (e: Exception) {
                fallbackColor
            }
        }
    }

    private fun getAlbumArtFromFile(filePath: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            val data = retriever.embeddedPicture ?: return null
            return BitmapFactory.decodeByteArray(data, 0, data.size)
        } catch (e: Exception) {
            return null
        } finally {
            retriever.release()
        }
    }
}
