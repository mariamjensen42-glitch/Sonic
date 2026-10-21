package com.cycling.data.repository

import android.net.Uri
import com.cycling.data.local.dao.LyricsIndexDao
import com.cycling.data.local.dao.SongDao
import com.cycling.data.local.dao.SongStateDao
import com.cycling.data.local.mediastore.MediaStoreHelper
import com.cycling.data.local.saf.SafFolderBrowser
import com.cycling.data.local.scanner.SongLibraryWriter
import com.cycling.data.mapper.toDomain
import com.cycling.data.mapper.toEntity
import com.cycling.domain.model.FolderContent
import com.cycling.domain.model.FolderItem
import com.cycling.domain.model.FolderRoot
import com.cycling.domain.model.LibraryStats
import com.cycling.domain.model.LyricsMatch
import com.cycling.domain.model.Song
import com.cycling.domain.repository.ExcludedFolderRepository
import com.cycling.domain.repository.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val songStateDao: SongStateDao,
    private val lyricsIndexDao: LyricsIndexDao,
    private val mediaStoreHelper: MediaStoreHelper,
    private val songLibraryWriter: SongLibraryWriter,
    private val excludedFolderRepository: ExcludedFolderRepository,
    private val safFolderBrowser: SafFolderBrowser
) : SongRepository {

    override fun getAllSongs(): Flow<List<Song>> {
        Timber.d("getAllSongs: called")
        return songDao.getAllSongs().map { entities ->
            Timber.d("getAllSongs: found ${entities.size} songs")
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSongById(id: Long): Song? {
        val song = songDao.getSongById(id)?.toDomain()
        Timber.d("getSongById: id=$id, found=${song != null}")
        return song
    }

    override fun getSongsByAlbum(albumId: Long): Flow<List<Song>> {
        Timber.d("getSongsByAlbum: albumId=$albumId")
        return songDao.getSongsByAlbum(albumId).map { entities ->
            Timber.d("getSongsByAlbum: found ${entities.size} songs for albumId=$albumId")
            entities.map { it.toDomain() }
        }
    }

    override fun getSongsByArtist(artistId: Long): Flow<List<Song>> {
        Timber.d("getSongsByArtist: artistId=$artistId")
        return songDao.getSongsByArtist(artistId).map { entities ->
            Timber.d("getSongsByArtist: found ${entities.size} songs for artistId=$artistId")
            entities.map { it.toDomain() }
        }
    }

    override fun searchSongs(query: String): Flow<List<Song>> {
        Timber.d("searchSongs: query=$query")
        return songDao.searchSongs(query).map { entities ->
            Timber.d("searchSongs: found ${entities.size} results for query=$query")
            entities.map { it.toDomain() }
        }
    }

    override fun searchLyrics(query: String): Flow<List<LyricsMatch>> {
        if (query.isBlank()) return flowOf(emptyList())
        return lyricsIndexDao.search(query).map { rows ->
            rows.map { row ->
                LyricsMatch(
                    song = row.song.toDomain(),
                    snippet = snippetAround(row.content, query)
                )
            }
        }
    }

    override suspend fun refreshSongs() {
        Timber.d("refreshSongs: starting refresh")
        // Goes through the same writer as MusicScanner: the old delete-then-reinsert wiped
        // favourites, play counts and (via the playlist_songs cascade) every playlist.
        // Previously this also ignored excluded folders, unlike a real scan.
        val excludedPaths = excludedFolderRepository.getExcludedPaths()
        val songs = mediaStoreHelper.queryAllSongs(excludedPaths)
        val written = songLibraryWriter.write(songs.map { it.toEntity() })
        Timber.d("refreshSongs: completed, $written songs")
    }

    override suspend fun getSongCount(): Int {
        return songDao.getSongCount()
    }

    override fun getFavoriteSongs(): Flow<List<Song>> {
        return songDao.getFavoriteSongs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMostPlayedSongs(): Flow<List<Song>> {
        return songDao.getMostPlayedSongs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentlyPlayedSongs(): Flow<List<Song>> {
        return songDao.getRecentlyPlayedSongs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun upsertOnlineSong(song: Song) {
        val sourceKey = song.onlineSourceKey ?: return
        val songId = song.onlineSongId ?: return
        songDao.upsertOnlineSong(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            duration = song.duration,
            mimeType = song.mimeType,
            albumArt = song.albumArt,
            onlineSourceKey = sourceKey,
            onlineSongId = songId,
            now = System.currentTimeMillis()
        )
    }

    override suspend fun toggleFavorite(songId: Long): Boolean {
        val song = songDao.getSongById(songId)
        if (song == null) {
            Timber.w("toggleFavorite: song not found, songId=$songId")
            return false
        }
        val newFavoriteStatus = !song.isFavorite
        songDao.updateFavorite(songId, newFavoriteStatus)
        Timber.d("toggleFavorite: songId=$songId, newFavoriteStatus=$newFavoriteStatus")
        return newFavoriteStatus
    }

    override suspend fun setFavoriteBatch(songIds: List<Long>, isFavorite: Boolean) {
        if (songIds.isEmpty()) return
        Timber.d("setFavoriteBatch: ${songIds.size} songs, isFavorite=$isFavorite")
        songDao.updateFavoriteBatch(songIds, isFavorite)
    }

    override suspend fun incrementPlayCount(songId: Long) {
        Timber.d("incrementPlayCount: songId=$songId")
        songDao.incrementPlayCount(songId, System.currentTimeMillis())
    }

    override suspend fun updateLastPlayedAt(songId: Long) {
        songDao.updateLastPlayedAt(songId, System.currentTimeMillis())
    }

    override suspend fun getLyricsOffset(songId: Long): Long =
        songStateDao.get(songId)?.lyricsOffsetMs ?: 0L

    override suspend fun setLyricsOffset(songId: Long, offsetMs: Long) {
        songStateDao.upsertLyricsOffset(songId, offsetMs, System.currentTimeMillis())
    }

    override suspend fun getLibraryStats(): LibraryStats = withContext(Dispatchers.IO) {
        val songsWithoutBitrate = songDao.getSongsWithoutBitrate()
        if (songsWithoutBitrate.isNotEmpty()) {
            songsWithoutBitrate.forEach { song ->
                val bitrate = mediaStoreHelper.extractBitrate(song.path)
                songDao.updateBitrate(song.id, bitrate)
            }
        }
        
        val stats = songDao.getLibraryStats()
        LibraryStats(
            totalSongs = stats.totalSongs,
            hrCount = stats.hrCount,
            sqCount = stats.sqCount,
            hqCount = stats.hqCount,
            othersCount = stats.othersCount
        )
    }

    override suspend fun updateSongInfo(songId: Long, title: String?, artist: String?, album: String?): Boolean {
        Timber.d("updateSongInfo: songId=$songId, title=$title, artist=$artist, album=$album")
        val song = songDao.getSongById(songId)
        if (song == null) {
            Timber.w("updateSongInfo: song not found, songId=$songId")
            return false
        }

        val updatedTitle = title ?: song.title
        val updatedArtist = artist ?: song.artist
        val updatedAlbum = album ?: song.album

        songDao.updateSongInfo(songId, updatedTitle, updatedArtist, updatedAlbum)
        Timber.i("updateSongInfo: successfully updated song info for songId=$songId")
        return true
    }

    private companion object {
        /**
         * 媒体库目录树模式的 rootUri 标记。SAF 模式的 rootUri 是 content:// tree uri，
         * 永不等于它；UI 借此区分当前浏览来源。
         */
        const val LIBRARY_ROOT = "library"

        const val SNIPPET_LEAD = 18
        const val SNIPPET_TAIL = 42
        const val SNIPPET_LENGTH = 60
    }

    override suspend fun getFolderRoots(): List<FolderRoot> =
        safFolderBrowser.roots()

    override suspend fun addFolderRoot(treeUri: String): Boolean =
        safFolderBrowser.addRoot(Uri.parse(treeUri))

    /**
     * 直接文件系统浏览：授权一棵 SAF 目录树，之后按 document id 逐级下钻。
     * 不经 MediaStore / Room，媒体库未扫描的音频同样可见、可播。
     */
    override suspend fun getFolderContent(rootUri: String, documentId: String): FolderContent =
        safFolderBrowser.children(Uri.parse(rootUri), documentId)

    /**
     * 媒体库目录树：从 Room 里已扫描歌曲的路径推导，不需要额外授权，
     * 但未入库的音频看不见（与 SAF 方式互补，供用户选择）。
     */
    override suspend fun getLibraryFolderContent(path: String): FolderContent = withContext(Dispatchers.IO) {
        Timber.d("getLibraryFolderContent: path=$path")

        val allPaths = songDao.getAllSongPaths()
        // 树从存储根 "/" 开始：空路径即根，其余统一去尾斜杠。前缀一律带 "/"，
        // 否则浏览 /Music 会把 /Music2 下的歌一并收进来
        val base = if (path.isEmpty() || path == "/") "/" else path.trimEnd('/')
        val prefix = if (base == "/") "/" else "$base/"

        val folders = mutableMapOf<String, Int>()

        allPaths.forEach { songPath ->
            if (songPath.startsWith(prefix)) {
                val relativePath = songPath.removePrefix(prefix)
                val slash = relativePath.indexOf('/')
                if (slash > 0) {
                    val dirName = relativePath.substring(0, slash)
                    // base 是 "/" 时不能再拼一个 "/"，否则得到 "//storage"
                    val folderPath = if (base == "/") "/$dirName" else "$base/$dirName"
                    folders[folderPath] = (folders[folderPath] ?: 0) + 1
                }
            }
        }

        val folderItems = folders.map { (folderPath, count) ->
            FolderItem(
                name = folderPath.substringAfterLast("/"),
                path = folderPath,
                songCount = count
            )
        }.sortedBy { it.name.lowercase() }

        val songs = if (base == "/") {
            emptyList()
        } else {
            songDao.getSongsByPathPrefix(prefix)
                .filter { songEntity ->
                    val relativePath = songEntity.path.removePrefix(prefix)
                    !relativePath.contains("/")
                }
                .map { it.toDomain() }
        }

        val parentPath = if (base == "/") null else {
            val parts = base.split("/")
            if (parts.size > 1) {
                parts.dropLast(1).joinToString("/")
            } else {
                ""
            }
        }

        Timber.d("getLibraryFolderContent: found ${folderItems.size} folders, ${songs.size} songs")

        FolderContent(
            folders = folderItems,
            songs = songs,
            currentPath = base,
            parentPath = parentPath,
            rootUri = LIBRARY_ROOT
        )
    }

    /** A short window around the first match, so the row can show why the song matched. */
    private fun snippetAround(content: String, query: String): String {
        if (content.isBlank()) return ""
        val index = content.indexOf(query, ignoreCase = true)
        if (index < 0) return content.take(SNIPPET_LENGTH)
        val start = (index - SNIPPET_LEAD).coerceAtLeast(0)
        val end = (index + query.length + SNIPPET_TAIL).coerceAtMost(content.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < content.length) "…" else ""
        return prefix + content.substring(start, end).trim() + suffix
    }
}
