package com.cycling.domain.model

import com.cycling.domain.online.model.MusicQuality

/** 下载任务的状态机。 */
enum class DownloadState {
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED;

    val isActive: Boolean get() = this == QUEUED || this == RUNNING

    val isFinished: Boolean get() = this == COMPLETED || this == FAILED
}

/**
 * 一首在线歌曲的离线下载任务。
 *
 * 只存「指针」与进度，不存可播放地址：地址由取链重新解析（并落 [com.cycling.domain.repository.PlayUrlCacheRepository]），
 * 否则任务放几天后重试会拿到一个早已过期的 URL。
 */
data class DownloadTask(
    val id: Long = 0,
    val songId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArt: String? = null,
    val quality: MusicQuality = MusicQuality.EXHIGH,
    val state: DownloadState = DownloadState.QUEUED,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    /** 落盘后的绝对路径（写入 MediaStore 的 DATA），完成后才有值。 */
    val filePath: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
) {
    /** 0f..1f；总长度未知（无 Content-Length）时返回 0。 */
    val progress: Float
        get() = if (totalBytes > 0) {
            (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
        } else {
            0f
        }
}
