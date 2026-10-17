package com.cycling.data.repository

import android.content.Context
import android.net.Uri
import com.cycling.data.local.dao.PlaylistDao
import com.cycling.data.local.dao.SongDao
import com.cycling.data.local.entity.PlaylistEntity
import com.cycling.domain.model.BackupImportResult
import com.cycling.domain.model.PlaylistImportResult
import com.cycling.domain.repository.LibraryTransferRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * File-based backup and M3U exchange.
 *
 * Deliberately file-first rather than a cloud feature: the library is local, so the one thing a
 * reinstall, a device change or a failed scan must not take away is the playlists and favourites.
 * Everything is matched against the current library instead of being inserted: a backup restores
 * *user data onto existing songs*, it never creates songs that are not on disk.
 */
@Singleton
class LibraryTransferRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao
) : LibraryTransferRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun exportBackup(targetUri: String): Int = withContext(Dispatchers.IO) {
        val songs = songDao.getAllSongs().first()
        val playlists = playlistDao.getAllPlaylists().first().mapNotNull { entity ->
            val trackIds = playlistDao.getPlaylistSongIds(entity.id)
            val paths = trackIds.mapNotNull { songDao.getSongById(it)?.path }
            BackupPlaylist(
                name = entity.name,
                description = entity.description,
                songPaths = paths
            )
        }

        val file = BackupFile(
            songs = songs.map { song ->
                BackupSong(
                    path = song.path,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    durationMs = song.duration,
                    favorite = song.isFavorite,
                    playCount = song.playCount,
                    lastPlayedAt = song.lastPlayedAt
                )
            },
            playlists = playlists
        )

        writeText(targetUri, json.encodeToString(BackupFile.serializer(), file))
        Timber.d("exportBackup: wrote ${songs.size} songs and ${playlists.size} playlists")
        songs.size
    }

    override suspend fun importBackup(sourceUri: String): BackupImportResult =
        withContext(Dispatchers.IO) {
            val content = readText(sourceUri)
            val file = json.decodeFromString(BackupFile.serializer(), content)
            val library = songDao.getAllSongs().first()
            val byPath = library.associateBy { normalize(it.path) }

            var matched = 0
            file.songs.forEach { saved ->
                val song = byPath[normalize(saved.path)] ?: return@forEach
                matched++
                songDao.updateFavorite(song.id, saved.favorite)
                if (saved.playCount > 0 || saved.lastPlayedAt != null) {
                    songDao.restorePlayStats(song.id, saved.playCount, saved.lastPlayedAt)
                }
            }

            var restoredPlaylists = 0
            file.playlists.forEach { saved ->
                val trackIds = saved.songPaths.mapNotNull { path ->
                    byPath[normalize(path)]?.id
                }
                if (trackIds.isEmpty()) return@forEach
                val now = System.currentTimeMillis() / 1000
                val playlistId = playlistDao.insertPlaylist(
                    PlaylistEntity(
                        name = saved.name,
                        dateAdded = now,
                        dateModified = now,
                        description = saved.description
                    )
                )
                playlistDao.addSongsToPlaylist(playlistId, trackIds)
                restoredPlaylists++
            }

            Timber.d("importBackup: matched $matched songs, restored $restoredPlaylists playlists")
            BackupImportResult(matchedSongs = matched, restoredPlaylists = restoredPlaylists)
        }

    override suspend fun exportPlaylistM3u(playlistId: Long, targetUri: String): Int =
        withContext(Dispatchers.IO) {
            val ids = playlistDao.getPlaylistSongIds(playlistId)
            val songs = ids.mapNotNull { songDao.getSongById(it) }
            val builder = StringBuilder("#EXTM3U\n")
            songs.forEach { song ->
                val seconds = max(0, song.duration / 1000)
                builder.append("#EXTINF:").append(seconds).append(',')
                    .append(song.artist).append(" - ").append(song.title).append('\n')
                builder.append(song.path).append('\n')
            }
            writeText(targetUri, builder.toString())
            Timber.d("exportPlaylistM3u: wrote ${songs.size} tracks for playlistId=$playlistId")
            songs.size
        }

    override suspend fun importM3u(
        sourceUri: String,
        playlistName: String?
    ): PlaylistImportResult = withContext(Dispatchers.IO) {
        val content = readText(sourceUri)
        val library = songDao.getAllSongs().first()
        val byPath = library.associateBy { normalize(it.path) }
        val byFileName = library.groupBy { it.path.substringAfterLast('/').lowercase() }
        val byTitle = library.groupBy { it.title.trim().lowercase() }

        val matched = LinkedHashSet<Long>()
        var missing = 0
        var pendingTitle: String? = null

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    // `#EXTINF:214,Artist - Title`; the title part is only a fallback matcher.
                    pendingTitle = line.substringAfter(',').trim().substringAfterLast(" - ").trim()
                }

                line.startsWith("#") -> Unit
                else -> {
                    val song = byPath[normalize(line)]
                        ?: byFileName[line.substringAfterLast('/').lowercase()]?.firstOrNull()
                        ?: pendingTitle?.let { byTitle[it.lowercase()]?.firstOrNull() }

                    if (song != null) {
                        matched += song.id
                    } else {
                        missing++
                    }
                    pendingTitle = null
                }
            }
        }

        val name = playlistName
            ?: sourceUri.substringAfterLast('/').substringBeforeLast('.').takeIf { it.isNotBlank() }
            ?: "导入的歌单"
        val now = System.currentTimeMillis() / 1000
        val playlistId = playlistDao.insertPlaylist(
            PlaylistEntity(name = name, dateAdded = now, dateModified = now)
        )
        playlistDao.addSongsToPlaylist(playlistId, matched.toList())

        Timber.d("importM3u: imported ${matched.size}, missing $missing from $sourceUri")
        PlaylistImportResult(playlistName = name, imported = matched.size, missing = missing)
    }

    private fun readText(uri: String): String =
        context.contentResolver.openInputStream(Uri.parse(uri))?.bufferedReader()?.use { it.readText() }
            ?: throw IOException("Cannot open $uri for reading")

    private fun writeText(uri: String, text: String) {
        context.contentResolver.openOutputStream(Uri.parse(uri), "wt")?.bufferedWriter()?.use {
            it.write(text)
        } ?: throw IOException("Cannot open $uri for writing")
    }

    /** Windows and Android may disagree on separators; compare without them or case. */
    private fun normalize(path: String): String = path.replace('\\', '/').lowercase()

    @Serializable
    private data class BackupFile(
        val version: Int = 1,
        val songs: List<BackupSong> = emptyList(),
        val playlists: List<BackupPlaylist> = emptyList()
    )

    @Serializable
    private data class BackupSong(
        val path: String,
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val favorite: Boolean,
        val playCount: Int,
        val lastPlayedAt: Long?
    )

    @Serializable
    private data class BackupPlaylist(
        val name: String,
        val description: String? = null,
        val songPaths: List<String> = emptyList()
    )
}
