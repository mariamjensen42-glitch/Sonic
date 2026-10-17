package com.cycling.domain.usecase

import com.cycling.domain.model.ScanProgress
import com.cycling.domain.model.ScanResult
import com.cycling.domain.repository.MusicScannerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanMusicUseCase @Inject constructor(
    private val musicScannerRepository: MusicScannerRepository
) {
    suspend fun scan(): Result<ScanResult> = musicScannerRepository.scanMusic()

    fun progress(): Flow<ScanProgress> = musicScannerRepository.scanProgress

    fun resetProgress() = musicScannerRepository.resetProgress()
}
