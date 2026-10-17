package com.cycling.domain.repository

import com.cycling.domain.model.BackupImportResult
import com.cycling.domain.model.PlaylistImportResult

/**
 * Backup, restore and M3U exchange.
 *
 * Every entry point takes a `content://` (or `file://`) URI chosen by the user through SAF and
 * returns counts instead of throwing for per-song misses — a library that has moved some files
 * should import what it can, not fail the whole operation.
 */
interface LibraryTransferRepository {

    /** Writes every playlist, favourite and play count as JSON. Returns the song count written. */
    suspend fun exportBackup(targetUri: String): Int

    /** Restores favourites, play counts and playlists from a backup file. */
    suspend fun importBackup(sourceUri: String): BackupImportResult

    /** Writes one playlist as an M3U8 file. Returns the number of tracks written. */
    suspend fun exportPlaylistM3u(playlistId: Long, targetUri: String): Int

    /**
     * Imports an M3U/M3U8 file as a new playlist. Entries are matched by absolute path first,
     * then by file name and `#EXTINF` title, so a library whose root moved still imports.
     */
    suspend fun importM3u(sourceUri: String, playlistName: String?): PlaylistImportResult
}
