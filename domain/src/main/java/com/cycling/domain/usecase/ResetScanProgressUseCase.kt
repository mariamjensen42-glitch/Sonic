package com.cycling.domain.usecase

import com.cycling.domain.repository.ScanProgressRepository
import javax.inject.Inject

class ResetScanProgressUseCase @Inject constructor(
    private val repository: ScanProgressRepository
) {
    operator fun invoke() {
        repository.resetProgress()
    }
}
