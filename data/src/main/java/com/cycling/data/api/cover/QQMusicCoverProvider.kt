package com.cycling.data.api.cover

import com.cycling.data.api.qqmusic.QQMusicSearchClient
import com.cycling.data.api.qqmusic.QQSearchSong
import com.cycling.domain.model.CoverCandidate
import com.cycling.domain.model.CoverSource
import com.cycling.domain.model.Song
import com.cycling.domain.repository.CoverArtProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * QQ Music as a cover source.
 *
 * The search result already carries the album's `mid`, and QQ's image CDN addresses covers by that
 * mid, so the URL is built locally — no second request.
 *
 * 搜索走 [QQMusicSearchClient]：老的 `client_search_cp` 已经下线（实测 HTTP 500），这里用的是
 * `musicu.fcg` 的统一搜索，并且带关键词缓存 —— QQ 对重复搜同一个词有风控，而本项目每次播放
 * 都会重新搜一次。
 */
@Singleton
class QQMusicCoverProvider @Inject constructor(
    private val searchClient: QQMusicSearchClient
) : CoverArtProvider {

    override val source: CoverSource = CoverSource.QQ_MUSIC

    override suspend fun search(song: Song): List<CoverCandidate> = withContext(Dispatchers.IO) {
        if (song.title.isBlank()) return@withContext emptyList()

        val keywords = listOf(
            "${song.artist} ${song.title}".trim(),
            song.title
        )
        qqCoverCandidates(searchClient.search(keywords, SEARCH_LIMIT), song)
    }

    private companion object {
        const val SEARCH_LIMIT = 10
    }
}

/**
 * 搜索结果 → 封面候选。
 *
 * 纯函数，不碰网络；见 `CoverProvidersTest`。
 */
internal fun qqCoverCandidates(
    songs: List<QQSearchSong>,
    song: Song
): List<CoverCandidate> = rankCoverCandidates(
    entries = songs,
    song = song,
    durationMsOf = { dto -> dto.durationMs.takeIf { it > 0L } },
    candidateOf = { dto ->
        dto.albumMid.takeIf { it.isNotBlank() }?.let { mid ->
            CoverCandidate(
                id = "qq#$mid",
                source = CoverSource.QQ_MUSIC,
                imageUrl = "${QQ_IMAGE_BASE}${mid}.jpg",
                album = dto.album.plainText().takeIf { it.isNotEmpty() },
                artist = dto.artist.plainText().takeIf { it.isNotEmpty() },
                title = dto.title.plainText().takeIf { it.isNotEmpty() },
                // QQ 的搜索结果没有 track/disc/genre 字段，专辑歌手也分不出来。
                year = yearOfEpochSeconds(dto.pubTimeSeconds),
                durationMs = dto.durationMs.takeIf { it > 0L }
            )
        }
    }
).take(MAX_COVER_CANDIDATES)

/** QQ's cover CDN: `T002R500x500M000` asks for a 500×500 JPEG for album mid [mid]. */
private const val QQ_IMAGE_BASE = "https://y.qq.com/music/photo_new/T002R500x500M000"
