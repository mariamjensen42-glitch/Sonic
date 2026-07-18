package com.cycling.data.repository

import com.cycling.data.local.scanner.MusicScanner
import com.cycling.domain.model.ScanProgress
import com.cycling.domain.repository.ScanProgressRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanProgressRepositoryImpl @Inject constructor(
    private val musicScanner: MusicScanner
) : ScanProgressRepository {
    override val scanProgress: Flow<ScanProgress> = musicScanner.scanProgress

    override fun resetProgress() {
        musicScanner.resetProgress()
    }
}
