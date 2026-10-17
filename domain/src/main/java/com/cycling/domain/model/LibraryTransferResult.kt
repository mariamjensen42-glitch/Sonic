package com.cycling.domain.model

/** What an M3U import managed to match. */
data class PlaylistImportResult(
    val playlistName: String,
    val imported: Int,
    /** Entries whose file is not in the library (moved, deleted, or from another device). */
    val missing: Int
)

/** What a backup restore changed. */
data class BackupImportResult(
    val matchedSongs: Int,
    val restoredPlaylists: Int
)
