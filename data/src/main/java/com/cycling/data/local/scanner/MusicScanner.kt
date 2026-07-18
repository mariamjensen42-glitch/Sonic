package com.cycling.data.local.scanner

import android.content.Context
import com.cycling.data.local.dao.SongDao
import com.cycling.data.local.mediastore.MediaStoreHelper
import com.cycling.data.mapper.toEntity
import com.cycling.domain.model.ScanProgress
import com.cycling.domain.model.ScanResult
import com.cycling.domain.model.ScanStep
import com.cycling.domain.repository.ExcludedFolderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaStoreHelper: MediaStoreHelper,
    private val songDao: SongDao,
    private val excludedFolderRepository: ExcludedFolderRepository
) {
    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    suspend fun scanMusic(): Result<ScanResult> = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            Timber.d("scanMusic: starting scan")
            
            _scanProgress.value = ScanProgress(
                isScanning = true,
                currentStep = ScanStep.QUERYING_MEDIASTORE
            )
            Timber.d("scanMediaStore: querying MediaStore for songs")
            
            val excludedPaths = excludedFolderRepository.getExcludedPaths()
            Timber.d("scanMediaStore: excluded paths = $excludedPaths")
            val songs = mediaStoreHelper.queryAllSongs(excludedPaths)
            Timber.d("scanMediaStore: found ${songs.size} songs")
            
            _scanProgress.value = ScanProgress(
                isScanning = true,
                currentStep = ScanStep.SAVING_TO_DATABASE,
                totalSongs = songs.size
            )
            Timber.d("saveSongsToDatabase: starting to save ${songs.size} songs to database")
            
            songDao.deleteAllSongs()
            Timber.d("saveSongsToDatabase: deleted all existing songs")
            
            val songsWithBitrate = songs.map { song ->
                async {
                    val bitrate = mediaStoreHelper.extractBitrate(song.path)
                    song.copy(bitrate = bitrate)
                }
            }.awaitAll()
            
            var processed = 0
            songsWithBitrate.chunked(BATCH_SIZE).forEach { chunk ->
                songDao.insertSongs(chunk.map { it.toEntity() })
                processed += chunk.size
                Timber.d("saveSongsToDatabase: saved batch, progress = $processed/${songs.size}")
                _scanProgress.value = ScanProgress(
                    isScanning = true,
                    currentStep = ScanStep.SAVING_TO_DATABASE,
                    songsProcessed = processed,
                    totalSongs = songs.size
                )
            }
            Timber.d("saveSongsToDatabase: completed saving $processed songs")
            
            val albums = mediaStoreHelper.queryAllAlbums()
            val artists = mediaStoreHelper.queryAllArtists()
            Timber.d("scanMusic: found ${albums.size} albums, ${artists.size} artists")
            
            val duration = System.currentTimeMillis() - startTime
            
            _scanProgress.value = ScanProgress(
                isScanning = false,
                currentStep = ScanStep.COMPLETED,
                songsProcessed = songs.size,
                totalSongs = songs.size
            )
            
            Timber.d("scanMusic: completed in ${duration}ms")
            Result.success(
                ScanResult(
                    songsFound = songs.size,
                    albumsFound = albums.size,
                    artistsFound = artists.size,
                    duration = duration,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "scanMusic: error during scan")
            _scanProgress.value = ScanProgress(
                isScanning = false,
                currentStep = ScanStep.ERROR
            )
            Result.failure(e)
        }
    }

    fun resetProgress() {
        Timber.d("resetProgress: resetting scan progress")
        _scanProgress.value = ScanProgress()
    }

    companion object {
        private const val BATCH_SIZE = 50
    }
}
