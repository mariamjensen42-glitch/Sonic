package com.cycling.domain.usecase

import com.cycling.domain.model.ScanResult
import com.cycling.domain.repository.MusicScannerRepository
import javax.inject.Inject

class ScanMusicUseCase @Inject constructor(
    private val musicScannerRepository: MusicScannerRepository
) {
    suspend operator fun invoke(): Result<ScanResult> {
        return musicScannerRepository.scanMusic()
    }
}
