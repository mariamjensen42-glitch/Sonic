package com.cycling.domain.repository

import com.cycling.domain.model.ScanResult

interface MusicScannerRepository {
    suspend fun scanMusic(): Result<ScanResult>
}
