package com.cycling.core.ui.theme

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import timber.log.Timber

/**
 * 封面位图读取：动态取色与桌面小组件共用同一条路径。
 *
 * `Song.albumArt` 是 MediaStore 的 albums 行（`content://media/external/audio/albums/<id>`），
 * 直接 `openInputStream` 会抛 `no such column: _data`（见 `MediaStoreBitmapLoader`），
 * 必须借一首该专辑的曲目走 `ContentResolver.loadThumbnail` —— 这也正是通知栏和小组件
 * 唯一能拿到封面的方式。再不行才退到按图片流读，最后退到音频文件里的内嵌封面。
 *
 * 只读缩略图：取色需要的是「封面上有哪些颜色」，不是原图分辨率，256px 足够且省内存
 * （原图 3000px 解码成 ARGB_8888 要 36 MB，切歌时白付这笔开销）。
 */
internal object CoverArtworkLoader {

    /** 缩略图边长（像素）。取色与小组件都用它。 */
    const val DEFAULT_SIZE_PX = 256

    /**
     * 依次尝试：albums 行 → 曲目缩略图 / 直接读 URI → 音频文件里的内嵌封面。
     * 全部失败返回 null。
     */
    fun load(
        context: Context,
        albumArt: String?,
        filePath: String? = null,
        sizePx: Int = DEFAULT_SIZE_PX
    ): Bitmap? {
        if (!albumArt.isNullOrBlank()) {
            loadFromUri(context, Uri.parse(albumArt), sizePx)?.let { return it }
        }
        return filePath?.let { loadEmbeddedArtwork(it, sizePx) }
    }

    private fun loadFromUri(context: Context, uri: Uri, sizePx: Int): Bitmap? {
        val resolver = context.contentResolver
        val thumbnailSource = albumArtThumbnailSource(context, uri) ?: uri
        runCatching {
            resolver.loadThumbnail(thumbnailSource, Size(sizePx, sizePx), null)
        }.onFailure {
            Timber.d(it, "CoverArtworkLoader: 缩略图失败，改按图片流读 %s", uri)
        }.getOrNull()?.let { return it }

        return runCatching {
            resolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }.onFailure {
            Timber.w(it, "CoverArtworkLoader: 封面读取失败 %s", uri)
        }.getOrNull()
    }

    /**
     * albums 行本身不是媒体行，只能拿一首属于它的曲目去要缩略图；
     * 其它封面（播放列表、歌手图等）原样请求即可。
     */
    private fun albumArtThumbnailSource(context: Context, uri: Uri): Uri? {
        if (uri.scheme != "content") return null
        if (!uri.toString().startsWith("${MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI}/")) return null
        val albumId = runCatching { ContentUris.parseId(uri) }.getOrNull() ?: return null

        return runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media._ID),
                "${MediaStore.Audio.Media.ALBUM_ID} = ?",
                arrayOf(albumId.toString()),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        cursor.getLong(0)
                    )
                } else {
                    null
                }
            }
        }.onFailure {
            Timber.w(it, "CoverArtworkLoader: 查专辑曲目失败 %s", uri)
        }.getOrNull()
    }

    /** MediaStore 读不到封面时（文件在库外、封面只在标签里）读内嵌图，按目标尺寸降采样。 */
    private fun loadEmbeddedArtwork(filePath: String, sizePx: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val data = retriever.embeddedPicture ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
            val longestSide = maxOf(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (longestSide / (sample * 2) >= sizePx * 2) sample *= 2
            BitmapFactory.decodeByteArray(
                data,
                0,
                data.size,
                BitmapFactory.Options().apply { inSampleSize = sample }
            )
        } catch (e: Exception) {
            Timber.w(e, "CoverArtworkLoader: 读取内嵌封面失败 %s", filePath)
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
