package com.cycling.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cycling.data.local.entity.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadTaskDao {

    @Query("SELECT * FROM download_task ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_task WHERE id = :id")
    suspend fun get(id: Long): DownloadTaskEntity?

    @Query("SELECT * FROM download_task WHERE songId = :songId AND state IN ('QUEUED', 'RUNNING', 'PAUSED') LIMIT 1")
    suspend fun findActiveBySong(songId: Long): DownloadTaskEntity?

    /** 队列里最该跑的一条：先入先跑，失败的跳过（失败要用户点重试）。 */
    @Query("SELECT * FROM download_task WHERE state = 'QUEUED' ORDER BY createdAt ASC LIMIT 1")
    suspend fun nextQueued(): DownloadTaskEntity?

    @Query("SELECT * FROM download_task WHERE state = 'RUNNING' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun runningTask(): DownloadTaskEntity?

    @Query("SELECT COUNT(*) FROM download_task WHERE state IN ('QUEUED', 'RUNNING')")
    suspend fun activeCount(): Int

    /**
     * 只推进度，不碰 state。
     *
     * 进度每 400ms 写一次，而用户随时可能按暂停 —— 整行 `@Update` 会把刚写进去的 PAUSED
     * 覆盖回 RUNNING，队列就永远停在那儿了。
     */
    @Query(
        """
        UPDATE download_task
        SET bytesDownloaded = :bytesDownloaded, totalBytes = :totalBytes, updatedAt = :now
        WHERE id = :id
        """
    )
    suspend fun updateProgress(id: Long, bytesDownloaded: Long, totalBytes: Long, now: Long)

    @Insert
    suspend fun insert(task: DownloadTaskEntity): Long

    @Update
    suspend fun update(task: DownloadTaskEntity)

    @Query("DELETE FROM download_task WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM download_task WHERE state = 'COMPLETED'")
    suspend fun deleteCompleted()

    /** 取消队列：上一次运行被系统杀掉后，残留的 RUNNING 行要回到 QUEUED。 */
    @Query("UPDATE download_task SET state = 'QUEUED', updatedAt = :now WHERE state = 'RUNNING'")
    suspend fun resetRunning(now: Long)
}
