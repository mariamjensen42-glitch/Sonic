package com.cycling.data.api.lyrics

import com.cycling.data.api.cover.jsonObjectOrNull
import com.cycling.data.api.lyrics.dto.QqLyricResponse
import com.cycling.data.api.qqmusic.QQMusicRichLyricsClient
import com.cycling.data.api.qqmusic.QQMusicSearchClient
import com.cycling.data.api.qqmusic.QQMusicSession
import com.cycling.data.api.qqmusic.QQMusicSessionProvider
import com.cycling.data.api.qqmusic.QQSearchSong
import com.cycling.domain.lyrics.provider.OnlineLyrics
import com.cycling.domain.lyrics.provider.OnlineLyricsProvider
import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.model.LyricsQuery
import com.cycling.domain.online.model.OnlineSource
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * QQ Music's public web lyric endpoints.
 *
 * 两条取词路径，按「能不能拿到逐字」分层：
 *
 * 1. **[QQMusicRichLyricsClient]**（逐字）：`music.musichallSong.PlayLyricInfo` 的 QRC 字段，
 *    解出来是带每字时间轴的 YRC，译文与罗马音另给两份密文。会员曲目的完整歌词只有登录才给。
 * 2. **本类**（行级）：搜索拿到 `songmid`，再从 `fcg_query_lyric_new.fcg` 取普通 LRC。
 *    逐字拿不到时才走这里 —— 逐字时间轴比行级译文更值钱。
 *
 * 搜索走 [QQMusicSearchClient]（`musicu.fcg` 的统一搜索）。老的 `client_search_cp` 已经下线，
 * 实测直接回 HTTP 500 —— 那时本类连 `songmid` 都拿不到，逐字与行级两条路一起断，表现为
 * 「QQ 音乐全都取不到歌词」，而登录与否都救不了：登录影响的是「会员曲目给不给完整歌词」，
 * 不是「能不能找到这首歌」。
 *
 * 搜索始终匿名也行（登录与否结果一样），登录态失效则自动退回匿名重试一次。
 */
@Singleton
class QQMusicLyricsProvider @Inject constructor(
    sharedClient: OkHttpClient,
    private val sessionProvider: QQMusicSessionProvider,
    private val richLyrics: QQMusicRichLyricsClient,
    private val searchClient: QQMusicSearchClient
) : OnlineLyricsProvider {

    override val provider: LyricsProvider = LyricsProvider.QQ_MUSIC

    /** Derived from the shared client so the connection pool and disk cache are reused. */
    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /** Playback downloads one document; the search is the only extra request. */
    override suspend fun fetch(query: LyricsQuery): OnlineLyrics? =
        collect(query, limit = 1).firstOrNull()

    /** The version picker can list the other matches. */
    override suspend fun fetchCandidates(query: LyricsQuery): List<OnlineLyrics> =
        collect(query, limit = MAX_LYRIC_CHOICES)

    /**
     * 有登录态就带着 Cookie 取一次；什么都没取到再匿名重试一次。
     *
     * 登录后 QQ 才会给会员曲目的完整歌词，但登录态一旦失效，这个接口不报 HTTP 错误，只回一个
     * 非零 `retcode` —— 表现就是「歌词凭空消失」。匿名再走一遍能把这种情况救回来，
     * 代价只有登录用户遇到无歌词曲目时的一次重复搜索。
     */
    private suspend fun collect(query: LyricsQuery, limit: Int): List<OnlineLyrics> {
        // 只有曲名是必需的：缺歌手标签时仍按曲名检索，是不是同一首由 [rankCandidates] 判定。
        if (query.title.isBlank()) return emptyList()

        val session = sessionProvider.current()
        val identity = session?.let { "uin=${it.uin}" } ?: "anonymous"
        Timber.d("QQMusicLyricsProvider: \"${query.title}\" as $identity")
        val signedIn = collectWith(query, limit, session)
        if (signedIn.isEmpty() && session != null) {
            Timber.d("QQMusicLyricsProvider: nothing as $identity, retrying anonymously")
            return collectWith(query, limit, session = null)
        }
        return signedIn
    }

    private suspend fun collectWith(
        query: LyricsQuery,
        limit: Int,
        session: QQMusicSession?
    ): List<OnlineLyrics> {
        return withContext(Dispatchers.IO) {
            val found = mutableListOf<OnlineLyrics>()
            var failure: Exception? = null

            // 在线 QQ 曲目自带 songmid，不必再搜一次；本地歌曲只能靠搜索定位。
            val knownMid = query.qqSongMid()
            if (knownMid != null) {
                fetchRichLyrics(knownMid, query)?.let { return@withContext listOf(OnlineLyrics(it)) }
            }

            val candidates = searchCandidates(query).take(limit)
            for ((index, song) in candidates.withIndex()) {
                // 逐字要多打两三次请求（换 id + 多档重试），只对第一个候选试：
                // 搜索的首位命中率足够高，为第 2、3 个候选再付这份成本不划算。
                if (knownMid == null && index == 0) {
                    // 搜索结果里就带着数字 id，直接给逐字客户端 —— 省掉一次「songmid 换 id」的请求，
                    // 也让 QRC 最全的 Mei 档能直接用上。
                    fetchRichLyrics(song.songMid, query, song.songId.takeIf { it > 0L })
                        ?.let { return@withContext listOf(OnlineLyrics(it)) }
                }

                val lrc = try {
                    fetchLyric(song.songMid, session)
                } catch (e: Exception) {
                    // Another match may still answer; a failure is only remembered and rethrown
                    // when nothing came back at all.
                    failure = failure ?: e
                    continue
                } ?: continue

                found += OnlineLyrics(rawContent = lrc)
            }

            if (found.isEmpty()) failure?.let { throw it }
            found
        }
    }

    /**
     * 逐字歌词：拿不到就返回 `null`，由调用方回落到行级 LRC。
     *
     * 这里吞掉异常而不是往上抛 —— 逐字是「更好」，行级是「有」。为了一次 QRC 解不开
     * 就让整首歌没有歌词，是把增强项当成了硬依赖。
     *
     * @param songId 搜索结果里带来的数字 id；有了它就不用再查一次 `fcg_play_single_song.fcg`。
     */
    private suspend fun fetchRichLyrics(
        songMid: String,
        query: LyricsQuery,
        songId: Long? = null
    ): String? =
        try {
            richLyrics.fetch(
                songMid = songMid,
                numericSongId = songId,
                title = query.title,
                artist = query.artist,
                album = query.album,
                durationMs = query.durationMs
            )?.takeIf { it.isNotBlank() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.d(e, "QQMusicLyricsProvider: 逐字取词失败 songmid=$songMid")
            null
        }

    /** 在线 QQ 曲目的 `songmid`；本地歌曲没有这个指针，返回 `null`。 */
    private fun LyricsQuery.qqSongMid(): String? = onlineSongId
        ?.takeIf { it.isNotBlank() && onlineSourceKey == OnlineSource.QQ.lxKey }

    /**
     * 平台里与这首歌对得上的几首，按可信度排序。
     *
     * 关键词给三种写法：主关键词（歌手 + 曲名）、只曲名、换过顺序的写法。QQ 的风控是**按关键词**
     * 算的，换一种写法就是另一个词，所以主关键词被挡时（`code = 2001`）换一种往往就能搜到。
     */
    private suspend fun searchCandidates(query: LyricsQuery): List<QQSearchSong> {
        val keywords = listOf(
            query.searchKeywords(),
            query.searchTitle,
            "${query.searchTitle} ${query.searchArtist}".trim()
        )
        val songs = searchClient.search(keywords, SEARCH_LIMIT)
        return rankCandidates(
            songs,
            query,
            QQSearchSong::title,
            QQSearchSong::artist,
            { song -> song.durationMs.takeIf { it > 0L } }
        )
    }

    /**
     * @param session 已登录的 QQ 音乐会话；为 null 时按游客取。
     * @return the merged LRC, or `null` when QQ has no lyric for [songmid].
     */
    private fun fetchLyric(songmid: String, session: QQMusicSession?): String? {
        val url = LYRIC_URL.toHttpUrl().newBuilder()
            .addQueryParameter("songmid", songmid)
            .addQueryParameter("format", "json")
            // 明文 LRC；部分边缘节点仍返回 base64，解析时两样都认。
            .addQueryParameter("nobase64", "1")
            .addQueryParameter("g_tk", "5381")
            // 登录态只靠 Cookie 传递：这一支接口带 Cookie 就能返回会员曲目的完整歌词，
            // 而 loginUin 保持 0 反而更稳（MeloX 的实现在登录时也是这么发的）。
            .addQueryParameter("loginUin", "0")
            .addQueryParameter("hostUin", "0")
            .addQueryParameter("platform", "yqq")
            .addQueryParameter("needNewCode", "0")
            .build()

        val body = get(url, LYRIC_REFERER, session?.cookie) ?: return null
        return parseQqLyrics(body)
    }

    /** @return the response body, or `null` when the endpoint answered without one. */
    private fun get(url: HttpUrl, referer: String, cookie: String? = null): String? {
        val request = Request.Builder()
            .url(url)
            .header("Referer", referer)
            .apply { if (!cookie.isNullOrBlank()) header("Cookie", cookie) }
            .build()

        val response = client.newCall(request).execute()
        response.use {
            if (!it.isSuccessful) throw IOException("QQ Music returned ${it.code}")
            return it.body?.string()?.takeIf { body -> body.isNotBlank() }
        }
    }

    private companion object {
        const val LYRIC_URL = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg"

        /** The lyric host rejects requests without a web-player referer. */
        const val LYRIC_REFERER = "https://y.qq.com/portal/player.html"

        const val SEARCH_LIMIT = 10
        const val CALL_TIMEOUT_SECONDS = 15L

        /** How many matches the version picker lists. Playback only ever downloads one. */
        const val MAX_LYRIC_CHOICES = 3
    }
}

/** Any LRC timing tag; used to tell a decoded document from base64 noise or an error body. */
private val QQ_TIMING_TAG = Regex("\\[\\d{1,2}:\\d{1,2}\\.\\d{2,3}]")

private val json = Json { ignoreUnknownKeys = true }

/**
 * Reads the lyric response into one merged LRC, or `null` when QQ has no lyrics for the song.
 *
 * `retcode` / `code` are checked because failures arrive as HTTP 200, and both text fields are
 * sniffed: `nobase64=1` is honoured by most edges but not all.
 */
internal fun parseQqLyrics(body: String): String? {
    val payload = body.jsonObjectOrNull() ?: return null
    val response = runCatching {
        json.decodeFromString<QqLyricResponse>(payload)
    }.getOrNull() ?: return null

    if (response.retcode != 0 || response.code != 0) return null

    val lyric = decodeQqText(response.lyric) ?: return null
    return mergeQqTranslation(lyric, decodeQqText(response.trans))
}

/** QQ serves either plain LRC or base64-encoded LRC depending on the edge that answered. */
private fun decodeQqText(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val text = if (raw.contains(QQ_TIMING_TAG)) {
        raw
    } else {
        runCatching { String(Base64.getDecoder().decode(raw), Charsets.UTF_8) }.getOrNull()
            ?: return null
    }
    return text.takeIf { it.contains(QQ_TIMING_TAG) }
}

/**
 * Interleaves QQ's translation LRC into the original one.
 *
 * The parser treats a line that follows another with the same start as its translation, so each
 * translated line has to sit **directly under** the line it translates; concatenating the two
 * documents would instead produce every translation as a separate lyric line.
 *
 * Pure so it can be tested without a network; see `LyricsProvidersTest`.
 */
internal fun mergeQqTranslation(lyric: String, translation: String?): String {
    if (translation.isNullOrBlank()) return lyric

    val translatedByTag = translation.lines()
        .mapNotNull { line -> QQ_TIMING_TAG.find(line)?.value?.let { tag -> tag to line } }
        .toMap()
    if (translatedByTag.isEmpty()) return lyric

    val merged = StringBuilder()
    lyric.lines().forEachIndexed { index, line ->
        if (index > 0) merged.append('\n')
        merged.append(line)

        val tag = QQ_TIMING_TAG.find(line)?.value ?: return@forEachIndexed
        val translated = translatedByTag[tag] ?: return@forEachIndexed
        if (translated == line) return@forEachIndexed
        merged.append('\n').append(translated)
    }
    return merged.toString()
}
