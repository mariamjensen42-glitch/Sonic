package com.cycling.data.repository

import android.content.Context
import com.cycling.data.download.DownloadService
import com.cycling.data.download.DownloadTransfer
import com.cycling.data.local.dao.DownloadTaskDao
import com.cycling.data.local.entity.DownloadTaskEntity
import com.cycling.data.local.mediastore.MediaStoreWriter
import com.cycling.data.mapper.toDomain
import com.cycling.data.online.CachedPlaySourceResolver
import com.cycling.domain.model.DownloadState
import com.cycling.domain.model.DownloadTask
import com.cycling.domain.model.Song
import com.cycling.domain.model.TagUpdate
import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.model.MusicQuality
import com.cycling.domain.online.model.OnlineSource
import com.cycling.domain.online.model.PlaySource
import com.cycling.domain.repository.AudioMetadataRepository
import com.cycling.domain.repository.DownloadRepository
import com.cycling.domain.repository.SongRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * 下载队列的执行者。
 *
 * 按「状态机管状态、传输管字节」切分：
 * - 本类只决定「哪条任务该跑、跑完写什么状态」，文件由 [DownloadTransfer] 搬；
 * - 单并发：手机上抢带宽比并发更常见，串行还能让进度通知只有一条；
 * - 任务表是唯一真相：进度与状态先落 Room，UI 和通知都从库里读，进程被杀也不丢。
 */
@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: DownloadTaskDao,
    private val resolver: CachedPlaySourceResolver,
    private val transfer: DownloadTransfer,
    private val mediaStoreWriter: MediaStoreWriter,
    private val audioMetadataRepository: AudioMetadataRepository,
    private val songRepository: SongRepository,
    private val httpClient: OkHttpClient
) : DownloadRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()

    private var pumpJob: Job? = null
    private var runningJob: Job? = null

    override fun observeTasks(): Flow<List<DownloadTask>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun enqueue(song: Song, quality: MusicQuality): Long {
        dao.findActiveBySong(song.id)?.let { existing ->
            Timber.d("Download: %s 已在队列中（id=%d）", song.title, existing.id)
            return existing.id
        }
        val now = System.currentTimeMillis()
        val id = dao.insert(
            DownloadTaskEntity(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                albumArt = song.albumArt,
                onlineSourceKey = song.onlineSourceKey,
                onlineSongId = song.onlineSongId,
                quality = quality.name,
                state = DownloadState.QUEUED.name,
                bytesDownloaded = 0,
                totalBytes = 0,
                filePath = null,
                errorMessage = null,
                createdAt = now,
                updatedAt = now
            )
        )
        Timber.i("Download: 入队 %s quality=%s", song.title, quality.name)
        kick()
        return id
    }

    override suspend fun pause(taskId: Long) {
        val task = dao.get(taskId) ?: return
        if (task.state == DownloadState.COMPLETED.name) return
        val wasRunning = task.state == DownloadState.RUNNING.name
        updateState(task, DownloadState.PAUSED)
        if (wasRunning) runningJob?.cancel()
        refreshNotification()
    }

    override suspend fun resume(taskId: Long) {
        val task = dao.get(taskId) ?: return
        if (task.state == DownloadState.COMPLETED.name) return
        updateState(task, DownloadState.QUEUED)
        kick()
    }

    override suspend fun cancel(taskId: Long) {
        val task = dao.get(taskId) ?: return
        if (task.state == DownloadState.RUNNING.name) runningJob?.cancel()
        partFile(task.id).delete()
        dao.delete(taskId)
        refreshNotification()
    }

    override suspend fun remove(taskId: Long) {
        dao.delete(taskId)
    }

    override suspend fun clearCompleted() {
        dao.deleteCompleted()
    }

    override suspend fun recoverPending() {
        dao.resetRunning(System.currentTimeMillis())
        kick()
    }

    private fun kick() {
        scope.launch {
            lock.withLock {
                if (pumpJob?.isActive == true) return@withLock
                pumpJob = scope.launch { pump() }
            }
        }
    }

    private suspend fun pump() {
        while (true) {
            val next = dao.nextQueued() ?: break
            val job = scope.launch { runTask(next) }
            runningJob = job
            job.join()
            runningJob = null
            refreshNotification()
        }
        DownloadService.stop(context)
    }

    private suspend fun runTask(entity: DownloadTaskEntity) {
        val started = entity.copy(
            state = DownloadState.RUNNING.name,
            errorMessage = null,
            updatedAt = System.currentTimeMillis()
        )
        dao.update(started)
        refreshNotification()

        try {
            val source = resolveSource(started)
            if (source == null) {
                fail(started, "取链失败：音源没有给出可播放地址")
                return
            }
            val part = partFile(started.id)
            transfer.download(source.url, source.headers, part) { downloaded, total ->
                dao.updateProgress(started.id, downloaded, total, System.currentTimeMillis())
                refreshNotification()
            }
            // 标签写入是按**扩展名**挑 writer 的（`isTagWriteSupported`），而临时文件叫
            // `task-7.part` —— 直接拿它去写会被判成「这个容器没有 writer」，标题和封面就被静默丢掉了。
            // 先补上真实后缀（改名，不复制），之后的标签与封面才写得进去。
            val (extension, mime) = mediaTypeOf(source.url, started.quality)
            val container = renameToContainer(part, extension)
            writeTags(started, container)
            publish(started, container, extension, mime)
        } catch (e: CancellationException) {
            // 暂停/取消已经写库，这里不覆盖状态。
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Download: %s 失败", started.title)
            fail(started, e.message ?: "下载失败")
        }
    }

    private suspend fun resolveSource(entity: DownloadTaskEntity): PlaySource? {
        val source = OnlineSource.fromLxKey(entity.onlineSourceKey ?: return null) ?: return null
        val songId = entity.onlineSongId ?: return null
        val quality = runCatching { MusicQuality.valueOf(entity.quality) }
            .getOrDefault(MusicQuality.EXHIGH)
        val track = MusicInfo(
            source = source,
            id = songId,
            title = entity.title,
            artist = entity.artist,
            album = entity.album,
            coverUrl = entity.albumArt
        )
        return resolver.resolve(entity.songId, track, quality)
    }

    /** 下载后补标签：不补的话文件在曲库里就是「平台歌名 + 空专辑」。 */
    private suspend fun writeTags(entity: DownloadTaskEntity, file: File) {
        if (!audioMetadataRepository.isTagWriteSupported(file.absolutePath)) {
            Timber.w("Download: %s 的容器没有标签 writer（.%s）", entity.title, file.extension)
            return
        }
        val result = runCatching {
            audioMetadataRepository.updateAudioTags(
                filePath = file.absolutePath,
                tagUpdate = TagUpdate(
                    title = entity.title,
                    artist = entity.artist,
                    album = entity.album
                )
            )
        }.getOrNull()
        if (result?.isSuccess != true) {
            Timber.w("Download: %s 标签写入未生效（文件仍可播放）", entity.title)
        }
        embedCover(entity, file)
    }

    /**
     * 把音源给的封面写进文件。
     *
     * 曲库里看到的封面是 `content://media/.../albums/<id>` —— MediaStore **扫描时**从文件内嵌
     * 图片生成的缩略图。文件里没有图，列表里就只有占位色。所以收尾必须把图写进标签，
     * 而不是指望入库之后再补。
     */
    private suspend fun embedCover(entity: DownloadTaskEntity, file: File) {
        val bytes = fetchCoverBytes(entity.albumArt)
        if (bytes == null) {
            Timber.d("Download: %s 没有可用的封面", entity.title)
            return
        }
        val ok = runCatching {
            audioMetadataRepository.writeCoverArtwork(file.absolutePath, bytes).getOrDefault(false)
        }.getOrDefault(false)
        if (!ok) {
            Timber.w("Download: %s 封面写入未生效（文件仍可播放）", entity.title)
        }
    }

    /** 封面多是几十 KB 的小图；超过上限说明拿到的不是图片，直接放弃。 */
    private suspend fun fetchCoverBytes(url: String?): ByteArray? = withContext(Dispatchers.IO) {
        val target = url?.takeIf {
            it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true)
        } ?: return@withContext null
        runCatching {
            httpClient.newCall(Request.Builder().url(target).build()).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body ?: return@use null
                if (body.contentLength() > MAX_COVER_BYTES) return@use null
                val bytes = body.bytes()
                bytes.takeIf { it.isNotEmpty() && it.size <= MAX_COVER_BYTES }
            }
        }.getOrNull()
    }

    private suspend fun publish(
        entity: DownloadTaskEntity,
        file: File,
        extension: String,
        mime: String
    ) {
        val displayName = "${sanitize(entity.artist)} - ${sanitize(entity.title)}$extension"
        val path = mediaStoreWriter.publish(file, displayName, mime)
        if (path == null) {
            fail(entity, "写入媒体库失败")
            return
        }
        file.delete()
        val latest = dao.get(entity.id) ?: entity
        dao.update(
            latest.copy(
                state = DownloadState.COMPLETED.name,
                filePath = path,
                updatedAt = System.currentTimeMillis()
            )
        )
        // 让曲库里立刻出现这首歌。
        runCatching { songRepository.refreshSongs() }
            .onFailure { Timber.w(it, "Download: 下载后刷新曲库失败") }
        Timber.i("Download: 完成 %s → %s", entity.title, path)
    }

    private suspend fun fail(entity: DownloadTaskEntity, message: String) {
        val latest = dao.get(entity.id) ?: entity
        dao.update(
            latest.copy(
                state = DownloadState.FAILED.name,
                errorMessage = message,
                updatedAt = System.currentTimeMillis()
            )
        )
        refreshNotification()
    }

    private suspend fun updateState(task: DownloadTaskEntity, state: DownloadState) {
        dao.update(task.copy(state = state.name, updatedAt = System.currentTimeMillis()))
    }

    /**
     * `.part` → `.mp3` 之类的真实后缀。
     *
     * 同一目录内改名是原子的；改不动（极少见，例如文件被别的进程占着）就退化成复制。
     */
    private fun renameToContainer(part: File, extension: String): File {
        val target = File(part.parentFile, "${part.nameWithoutExtension}$extension")
        if (target.exists()) target.delete()
        if (part.renameTo(target)) return target
        return runCatching { part.copyTo(target, overwrite = true) }.getOrNull()
            ?.also { part.delete() }
            ?: part
    }

    private fun partFile(taskId: Long): File {
        val dir = (context.getExternalFilesDir("downloads") ?: File(context.filesDir, "downloads"))
            .apply { mkdirs() }
        return File(dir, "task-$taskId.part")
    }

    /** 前台通知只反映「当前正在跑的那条」，没有就收掉服务。 */
    private suspend fun refreshNotification() {
        val running = dao.runningTask()
        if (running == null) {
            DownloadService.stop(context)
            return
        }
        val percent = if (running.totalBytes > 0) {
            (running.bytesDownloaded * 100 / running.totalBytes).toInt().coerceIn(0, 100)
        } else {
            0
        }
        DownloadService.start(
            context = context,
            title = running.title,
            text = if (running.totalBytes > 0) "已下载 $percent%" else "正在解析下载链接…",
            progress = percent,
            indeterminate = running.totalBytes <= 0
        )
    }

    private fun mediaTypeOf(url: String, quality: String): Pair<String, String> {
        val fromUrl = url.substringBefore('?').substringAfterLast('.', "").lowercase()
        return when (fromUrl) {
            "flac" -> ".flac" to "audio/flac"
            "m4a", "mp4" -> ".m4a" to "audio/mp4"
            "ogg" -> ".ogg" to "audio/ogg"
            "wav" -> ".wav" to "audio/wav"
            "mp3" -> ".mp3" to "audio/mpeg"
            else -> {
                val lossless = runCatching {
                    MusicQuality.valueOf(quality).rank >= MusicQuality.LOSSLESS.rank
                }.getOrDefault(false)
                if (lossless) ".flac" to "audio/flac" else ".mp3" to "audio/mpeg"
            }
        }
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "unknown" }

    private companion object {
        /** 封面图上限 8MB：正常专辑图远小于它，超过多半是拿到了 HTML 错误页。 */
        const val MAX_COVER_BYTES = 8L * 1024 * 1024
    }
}
