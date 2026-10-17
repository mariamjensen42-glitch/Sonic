package com.cycling.data.local.scanner

import androidx.room.withTransaction
import com.cycling.data.local.dao.SongDao
import com.cycling.data.local.database.AppDatabase
import com.cycling.data.local.entity.SongEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single writer for MediaStore scan results.
 *
 * Previously both [MusicScanner] and `SongRepositoryImpl.refreshSongs` did
 * `deleteAllSongs()` followed by a bulk re-insert. That destroyed user data twice over:
 * every `isFavorite` / `playCount` / `lastPlayedAt` was reset to its column default, and because
 * `PRAGMA foreign_keys = ON` plus `playlist_songs`'s `ON DELETE CASCADE`, every playlist was
 * emptied as well. Routing both call sites through this one class is what makes the fix hold —
 * there is no longer a second place that can reintroduce the wipe.
 */
@Singleton
class SongLibraryWriter @Inject constructor(
    private val database: AppDatabase,
    private val songDao: SongDao
) {

    /**
     * Upserts [scanned] and prunes rows whose file is no longer in MediaStore.
     *
     * Per-song user data survives because [SongDao.upsertScannedSong] omits those columns from
     * its `DO UPDATE SET`. Returns the number of songs written.
     */
    suspend fun write(
        scanned: List<SongEntity>,
        onProgress: suspend (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): Int = database.withTransaction {
        scanned.forEachIndexed { index, song ->
            songDao.upsert(song)
            val processed = index + 1
            if (processed % PROGRESS_INTERVAL == 0 || processed == scanned.size) {
                onProgress(processed, scanned.size)
            }
        }

        val livePaths = scanned.mapTo(HashSet(scanned.size)) { it.path }
        songDao.getAllSongPaths()
            .filterNot { it in livePaths }
            .chunked(DELETE_CHUNK_SIZE)
            .forEach { stale -> songDao.deleteSongsByPaths(stale) }

        scanned.size
    }

    private suspend fun SongDao.upsert(song: SongEntity) = upsertScannedSong(
        id = song.id,
        title = song.title,
        artist = song.artist,
        album = song.album,
        duration = song.duration,
        path = song.path,
        albumId = song.albumId,
        artistId = song.artistId,
        dateAdded = song.dateAdded,
        dateModified = song.dateModified,
        size = song.size,
        mimeType = song.mimeType,
        albumArt = song.albumArt,
        isFavorite = song.isFavorite,
        playCount = song.playCount,
        lastPlayedAt = song.lastPlayedAt,
        bitrate = song.bitrate,
        rating = song.rating,
        customAlbumArt = song.customAlbumArt
    )

    companion object {
        private const val PROGRESS_INTERVAL = 50

        /**
         * `IN (…)` binds one variable per element and Android 11's SQLite 3.28 caps out at 999.
         */
        private const val DELETE_CHUNK_SIZE = 500
    }
}
