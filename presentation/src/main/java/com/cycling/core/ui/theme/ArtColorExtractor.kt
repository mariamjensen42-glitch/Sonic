package com.cycling.core.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * 封面取色。
 *
 * 只取一颗**种子色**，而不是直接拿封面上的几个原始颜色去铺界面 ——
 * 原始颜色无法保证对比度与色阶关系；交给 [SonTonalScheme] 生成完整色调板后，
 * 任何一个封面都能得到一套可读、协调的配色。
 */
object ArtColorExtractor {

    /** 种子色最低彩度。低于此值说明封面接近灰阶，取出来的色相不稳定。 */
    private const val MIN_SEED_CHROMA = 12.0

    /** 从本地音频文件的内嵌封面提取种子色。 */
    suspend fun seedFromMusicFile(filePath: String): Color? = withContext(Dispatchers.IO) {
        val bitmap = readEmbeddedArtwork(filePath) ?: return@withContext null
        seedFromBitmap(bitmap)
    }

    /** 从任意位图提取种子色。 */
    suspend fun seedFromBitmap(bitmap: Bitmap): Color? = withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap)
            .maximumColorCount(24)
            .generate()

        // 按 CAM16 彩度挑最"有颜色"的那一档，保证种子能撑起一套有性格的配色
        val candidates = listOfNotNull(
            palette.vibrantSwatch,
            palette.lightVibrantSwatch,
            palette.darkVibrantSwatch,
            palette.mutedSwatch,
            palette.dominantSwatch
        )
        val seed = candidates
            .map { Color(it.rgb or (0xFF shl 24)) }
            .maxByOrNull { Hct.fromColor(it).chroma }
            ?: return@withContext null

        seed.saturatedForScheme()
    }

    private fun Color.saturatedForScheme(): Color {
        val hct = Hct.fromColor(this)
        if (hct.chroma >= MIN_SEED_CHROMA) return this
        // 近灰封面：保留明度与色相，把彩度抬到可用下限
        return Hct.from(hct.hue, MIN_SEED_CHROMA, hct.tone).toColor()
    }

    private fun readEmbeddedArtwork(filePath: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val data = retriever.embeddedPicture ?: return null
            BitmapFactory.decodeByteArray(data, 0, data.size)
        } catch (e: Exception) {
            Timber.w(e, "ArtColorExtractor: 读取内嵌封面失败")
            null
        } finally {
            retriever.release()
        }
    }
}

