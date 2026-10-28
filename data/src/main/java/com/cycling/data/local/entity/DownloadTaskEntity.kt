package com.cycling.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 下载任务表。
 *
 * 独立于 `songs`：任务描述的是「一首在线歌 → 一个文件」的过程，和曲库行不是一回事
 * （在线歌本身已经在 `songs` 里，下载完成后文件会被扫描成一条新的本地记录）。
 */
@Entity(tableName = "download_task")
data class DownloadTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArt: String?,
    val onlineSourceKey: String?,
    val onlineSongId: String?,
    /** [com.cycling.domain.online.model.MusicQuality] 的枚举名。 */
    val quality: String,
    /** [com.cycling.domain.model.DownloadState] 的枚举名。 */
    val state: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val filePath: String?,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long
)
