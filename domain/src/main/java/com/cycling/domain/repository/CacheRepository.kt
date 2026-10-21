package com.cycling.domain.repository

import com.cycling.domain.model.CacheUsage

interface CacheRepository {
    suspend fun usage(): CacheUsage

    /** 删掉 [id] 这个缓存目录，返回释放的字节数。 */
    suspend fun clearDiskCache(id: String): Long

    /** 清空歌词缓存表，返回清掉的条数。 */
    suspend fun clearLyricsCache(): Int

    /** 清空 AI 信息缓存表，返回清掉的条数。 */
    suspend fun clearAiInfoCache(): Int
}
