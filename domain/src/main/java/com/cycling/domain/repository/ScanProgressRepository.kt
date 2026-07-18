package com.cycling.domain.repository

import com.cycling.domain.model.ScanProgress
import kotlinx.coroutines.flow.Flow

interface ScanProgressRepository {
    val scanProgress: Flow<ScanProgress>
    fun resetProgress()
}
