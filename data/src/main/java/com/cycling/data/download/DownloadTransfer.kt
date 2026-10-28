package com.cycling.data.download

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * 单文件传输：**只负责把字节搬到目标文件**，不懂任务状态、不懂音源。
 *
 * 断点续传靠 `.part` 文件 + `Range` 请求；服务器不认 Range（回 200 而不是 206）时从头重来 ——
 * 假装续传会拼出一个坏掉的音频。
 */
@Singleton
class DownloadTransfer @Inject constructor(
    sharedClient: OkHttpClient
) {

    private val client = sharedClient.newBuilder()
        // 整首歌可能几百 MB，默认的 callTimeout 会把大文件误杀。
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    /**
     * @param onProgress (已下载字节, 总字节；未知为 0)
     * @return 下载完成的临时文件
     */
    suspend fun download(
        url: String,
        headers: Map<String, String>,
        target: File,
        onProgress: suspend (downloaded: Long, total: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val scope = coroutineContext
        val existing = if (target.exists()) target.length() else 0L
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (key, value) -> requestBuilder.header(key, value) }
        if (existing > 0) requestBuilder.header("Range", "bytes=$existing-")

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("响应体为空")
            val resuming = response.code == 206 && existing > 0
            val total = parseTotal(response.header("Content-Range"), body.contentLength(), existing)
            if (!resuming) {
                target.parentFile?.mkdirs()
                target.delete()
            }
            val start = if (resuming) existing else 0L
            body.byteStream().use { input ->
                append(input, target, scope, start, total, onProgress)
            }
        }
        Timber.d("DownloadTransfer: 完成 %s，%d 字节", target.name, target.length())
        target
    }

    private suspend fun append(
        input: InputStream,
        target: File,
        scope: CoroutineContext,
        startBytes: Long,
        total: Long,
        onProgress: suspend (Long, Long) -> Unit
    ) {
        val buffer = ByteArray(BUFFER_SIZE)
        var downloaded = startBytes
        var lastReport = 0L
        FileOutputStream(target, true).use { out ->
            while (true) {
                scope.ensureActive()
                val read = input.read(buffer)
                if (read <= 0) break
                out.write(buffer, 0, read)
                downloaded += read
                val now = System.currentTimeMillis()
                if (now - lastReport >= PROGRESS_INTERVAL_MS) {
                    lastReport = now
                    onProgress(downloaded, total)
                }
            }
        }
        onProgress(downloaded, total)
    }

    /** `Content-Range: bytes 100-999/1000` 里的总数优先，其次 Content-Length + 已有字节。 */
    private fun parseTotal(contentRange: String?, contentLength: Long, existing: Long): Long {
        contentRange?.substringAfterLast('/')?.toLongOrNull()?.let { if (it > 0) return it }
        return if (contentLength > 0) contentLength + existing else 0L
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val PROGRESS_INTERVAL_MS = 400L
    }
}
