package com.cycling.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 「排除录音」开关。开着的扫描会把 [com.cycling.domain.scanner.RecordingFilter] 判定为录音的
 * 文件踢出曲库，关掉则照旧收进来。
 */
interface RecordingFilterRepository {

    val excludeRecordings: Flow<Boolean>

    suspend fun setExcludeRecordings(enabled: Boolean)

    /** 扫描时的同步取值，避免在 IO 循环里挂 Flow。 */
    suspend fun isExcludeRecordingsEnabled(): Boolean
}
