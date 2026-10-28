package com.cycling.data.local.mediastore

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * 把下载好的临时文件发布到公共音乐目录。
 *
 * 之所以不直接往 `Music/Sonic` 写：分区存储下不经过 MediaStore 的文件既不会出现在
 * 系统音乐库里，也会被其它扫描器当成野文件。走 [MediaStore.Audio.Media] 的
 * `IS_PENDING` 流程，写入期间对外不可见，写完才现身。
 */
@Singleton
class MediaStoreWriter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * @return 落盘后的绝对路径（即 MediaStore 的 `DATA` 列），失败返回 null。
     */
    suspend fun publish(
        source: File,
        displayName: String,
        mimeType: String,
        relativeDir: String = DEFAULT_RELATIVE_DIR
    ): String? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.RELATIVE_PATH, relativeDir)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val uri = runCatching { resolver.insert(collection, values) }.getOrNull()
        if (uri == null) {
            Timber.w("MediaStoreWriter: 无法创建条目 %s", displayName)
            return@withContext null
        }
        try {
            resolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { input -> input.copyTo(out) }
            } ?: error("openOutputStream 返回 null")
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
                null,
                null
            )
            val path = queryPath(uri) ?: uri.toString()
            // 刚写入的条目时长还是 0，扫描器要 `DURATION > 0` 才收；这里催一次元数据提取。
            runCatching {
                MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf(mimeType), null)
            }
            path
        } catch (e: Exception) {
            Timber.w(e, "MediaStoreWriter: 写入失败 %s", displayName)
            runCatching { resolver.delete(uri, null, null) }
            null
        }
    }

    private fun queryPath(uri: android.net.Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(MediaStore.Audio.Media.DATA), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull()

    companion object {
        private val DEFAULT_RELATIVE_DIR = "${Environment.DIRECTORY_MUSIC}/Sonic"
    }
}
