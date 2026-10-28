package com.cycling.domain.repository

import com.cycling.domain.model.DownloadTask
import com.cycling.domain.model.Song
import com.cycling.domain.online.model.MusicQuality
import kotlinx.coroutines.flow.Flow

/**
 * 离线下载。
 *
 * 只暴露「任务状态」与四个动作：实现内部负责取链、断点续传、写标签、入库与前台通知，
 * 调用方（ViewModel）不需要知道文件落在哪。
 */
interface DownloadRepository {

    fun observeTasks(): Flow<List<DownloadTask>>

    /** 入队；同一个 songId 已有未完成任务时直接返回那条任务的 id。 */
    suspend fun enqueue(song: Song, quality: MusicQuality): Long

    suspend fun pause(taskId: Long)

    suspend fun resume(taskId: Long)

    /** 取消并删除已下载的部分。 */
    suspend fun cancel(taskId: Long)

    /** 从列表里移除记录（不删文件）。 */
    suspend fun remove(taskId: Long)

    suspend fun clearCompleted()

    /** 进程启动后调用一次：把上次被系统打断的任务打回队列并继续。幂等。 */
    suspend fun recoverPending()
}
