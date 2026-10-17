package com.cycling.domain.repository

import com.cycling.domain.model.ScanProgress
import com.cycling.domain.model.ScanResult
import kotlinx.coroutines.flow.Flow

interface MusicScannerRepository {
    suspend fun scanMusic(): Result<ScanResult>

    val scanProgress: Flow<ScanProgress>

    fun resetProgress()
}
