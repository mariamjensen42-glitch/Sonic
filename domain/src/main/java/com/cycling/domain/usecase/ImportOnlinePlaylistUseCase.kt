package com.cycling.domain.usecase

import com.cycling.domain.model.ImportedPlaylist
import com.cycling.domain.model.Song
import com.cycling.domain.online.OnlinePlaylistRepository
import com.cycling.domain.online.PlaylistFetchResult
import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.model.toDownloadSong
import com.cycling.domain.repository.PlaylistRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.scrape.ScrapeMatch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.flow.first

/** 一次导入的结果，给界面报数用。 */
sealed interface ImportOnlinePlaylistOutcome {

    /** @param localMatched 其中有多少首直接指到了本地文件（音质更好、也不走网络）。 */
    data class Imported(
        val playlistId: Long,
        val playlistName: String,
        val imported: Int,
        val localMatched: Int
    ) : ImportOnlinePlaylistOutcome

    data object InvalidLink : ImportOnlinePlaylistOutcome

    data object NotAccessible : ImportOnlinePlaylistOutcome

    data class Failed(val message: String? = null) : ImportOnlinePlaylistOutcome
}

/**
 * 把在线平台的歌单搬进本地歌单。
 *
 * 三个决定：
 *
 * 1. **不预先取链。** 几百首逐个跑音源脚本又慢又容易触发风控，而链本身几分钟就过期。
 *    这里只写指针（`onlineSourceKey` + `onlineSongId`），播到哪首才由
 *    `CachedPlaySourceResolver` 去取 —— 与「在线搜索里点一首歌就播放」是同一条路。
 * 2. **本地优先。** 能按标题 + 艺术家 + 时长在本地库里对上同一首的，直接指本地文件：
 *    不重复占一行、音质更好、断网也能放。判定复用 [ScrapeMatch] 的归一化规则 ——
 *    库里的文件名常带 `(Live)`、`feat. xxx`，不归一化会漏掉一半。
 * 3. **不覆盖已有歌单。** 同名时加序号，而不是静默替换：导入是要多出一张歌单，不是编辑它。
 */
@Singleton
class ImportOnlinePlaylistUseCase @Inject constructor(
    private val onlinePlaylistRepository: OnlinePlaylistRepository,
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository
) {

    suspend operator fun invoke(
        shareText: String,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> }
    ): ImportOnlinePlaylistOutcome {
        val fetched = when (val result = onlinePlaylistRepository.fetchPlaylist(shareText)) {
            is PlaylistFetchResult.Success -> result.playlist
            PlaylistFetchResult.InvalidLink -> return ImportOnlinePlaylistOutcome.InvalidLink
            PlaylistFetchResult.NotAccessible -> return ImportOnlinePlaylistOutcome.NotAccessible
            is PlaylistFetchResult.Failed -> return ImportOnlinePlaylistOutcome.Failed(result.message)
        }
        return persist(fetched, onProgress)
    }

    /**
     * 导入账号里已有的一张歌单（列表里点的那一行）。
     *
     * 与 [invoke] 的落库逻辑完全一致，只是 id 已经拿到、不必再从文本里挖 ——
     * 所以两者共用 [persist]，避免「链接导入」和「列表导入」在匹配规则上分叉。
     */
    suspend fun importById(
        playlistId: String,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> }
    ): ImportOnlinePlaylistOutcome {
        val fetched = when (val result = onlinePlaylistRepository.fetchPlaylistById(playlistId)) {
            is PlaylistFetchResult.Success -> result.playlist
            PlaylistFetchResult.InvalidLink -> return ImportOnlinePlaylistOutcome.InvalidLink
            PlaylistFetchResult.NotAccessible -> return ImportOnlinePlaylistOutcome.NotAccessible
            is PlaylistFetchResult.Failed -> return ImportOnlinePlaylistOutcome.Failed(result.message)
        }
        return persist(fetched, onProgress)
    }

    /** 歌单已拿到手之后的落库流程：建歌单 → 逐首对本地 → 写指针 → 关联。 */
    private suspend fun persist(
        fetched: ImportedPlaylist,
        onProgress: suspend (done: Int, total: Int) -> Unit
    ): ImportOnlinePlaylistOutcome {
        // 同一首歌在一张歌单里可能出现两次（不同版本共享 mid 的情况很少，但平台会给重复项）
        val tracks = fetched.tracks.distinctBy { it.source to it.id }
        if (tracks.isEmpty()) return ImportOnlinePlaylistOutcome.NotAccessible

        val playlistName = uniqueName(fetched)
        val playlistId = playlistRepository.createPlaylist(playlistName, fetched.description)

        // 本地索引一次建好：逐首去查 `searchSongs` 是 `LIKE '%x%'` 全表扫，几百首会慢到几十秒
        val localIndex = localSongsByTitle()
        val songIds = ArrayList<Long>(tracks.size)
        var localMatched = 0
        tracks.forEachIndexed { index, track ->
            val local = findLocalMatch(track, localIndex)
            if (local != null) {
                songIds += local.id
                localMatched++
            } else {
                // 只写指针：可播的 URL 每次播放时由音源脚本现取，入库的地址会过期
                val online = track.toDownloadSong()
                songRepository.upsertOnlineSong(online)
                songIds += online.id
            }
            // 逐首回调会让界面每 16ms 重组一次；每十首报一次足够了
            if ((index + 1) % PROGRESS_STEP == 0 || index == tracks.lastIndex) {
                onProgress(index + 1, tracks.size)
            }
        }

        val imported = playlistRepository.addSongsToPlaylist(playlistId, songIds)
        return ImportOnlinePlaylistOutcome.Imported(
            playlistId = playlistId,
            playlistName = playlistName,
            imported = imported,
            localMatched = localMatched
        )
    }

    /**
     * 同名歌单不覆盖，改成 `名字（2）`、`名字（3）`……
     *
     * 只查一次全量歌单列表：几十张的量级，比每次导入都去数据库里逐字比对便宜。
     */
    private suspend fun uniqueName(playlist: ImportedPlaylist): String {
        val existing = playlistRepository.getAllPlaylists().first().map { it.name }.toSet()
        if (playlist.name !in existing) return playlist.name
        var index = 2
        while ("${playlist.name}（$index）" in existing) index++
        return "${playlist.name}（$index）"
    }

    /**
     * 本地曲目按「归一化标题」建索引。
     *
     * 宁可多占一次全量列表的内存（几万首也就几十 MB，导入是一次性操作），也不逐首去查询：
     * `songs` 表上按 `title` 是 `LIKE '%x%'`，走不了索引，几百次下来就是几十秒的卡顿。
     */
    private suspend fun localSongsByTitle(): Map<String, List<Song>> =
        runCatching { songRepository.getAllSongs().first() }
            .getOrDefault(emptyList())
            .asSequence()
            .filter { !it.isOnline }
            .groupBy { ScrapeMatch.normalize(it.title) }
            .filterKeys { it.isNotEmpty() }

    /**
     * 本地库里找同一首。
     *
     * 按「归一化标题 → 艺术家 → 时长」三层精判。时长容差 3 秒：现场版和前奏偏移常差这么多，
     * 再放宽就会匹配到别的录音，那比不匹配更糟（用户点开是另一首歌）。
     */
    private fun findLocalMatch(track: MusicInfo, index: Map<String, List<Song>>): Song? {
        val targetTitle = ScrapeMatch.normalize(track.title.trim())
        if (targetTitle.isEmpty()) return null
        val candidates = index[targetTitle] ?: return null
        val targetArtist = ScrapeMatch.normalize(track.artist.substringBefore(','))
        val expectedMs = track.durationSeconds * 1000

        return candidates.asSequence()
            .filter { song ->
                if (targetArtist.isEmpty()) return@filter true
                val localArtist = ScrapeMatch.normalize(song.artist)
                localArtist.isNotEmpty() &&
                    (localArtist.contains(targetArtist) || targetArtist.contains(localArtist))
            }
            .filter { song ->
                expectedMs <= 0L || abs(song.duration - expectedMs) <= DURATION_TOLERANCE_MS
            }
            .minByOrNull { if (expectedMs <= 0L) 0L else abs(it.duration - expectedMs) }
    }

    private companion object {
        const val PROGRESS_STEP = 10
        const val DURATION_TOLERANCE_MS = 3_000L
    }
}
