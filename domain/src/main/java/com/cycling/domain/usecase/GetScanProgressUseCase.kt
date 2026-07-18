package com.cycling.domain.usecase

import com.cycling.domain.model.ScanProgress
import com.cycling.domain.repository.ScanProgressRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetScanProgressUseCase @Inject constructor(
    private val repository: ScanProgressRepository
) {
    operator fun invoke(): Flow<ScanProgress> {
        return repository.scanProgress
    }
}
