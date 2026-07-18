package com.cycling.data.repository

import com.cycling.data.local.scanner.MusicScanner
import com.cycling.domain.model.ScanResult
import com.cycling.domain.repository.MusicScannerRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicScannerRepositoryImpl @Inject constructor(
    private val musicScanner: MusicScanner
) : MusicScannerRepository {
    override suspend fun scanMusic(): Result<ScanResult> {
        Timber.d("scanMusic: starting scan")
        val startTime = System.currentTimeMillis()
        return musicScanner.scanMusic().also { result ->
            val duration = System.currentTimeMillis() - startTime
            result.getOrNull()?.let {
                Timber.d("scanMusic: completed in ${duration}ms, found ${it.songsFound} songs, ${it.albumsFound} albums, ${it.artistsFound} artists")
            } ?: result.exceptionOrNull()?.let {
                Timber.e(it, "scanMusic: failed after ${duration}ms")
            }
        }
    }
}
