package com.cycling.data.api.lyrics

import com.cycling.data.api.lyrics.dto.AmllGetResponseDto
import com.cycling.data.api.lyrics.dto.AmllSearchResponseDto
import com.cycling.data.api.lyrics.dto.AmllSongItemDto
import com.cycling.domain.lyrics.provider.OnlineLyrics
import com.cycling.domain.lyrics.provider.OnlineLyricsProvider
import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.model.LyricsQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * The [AMLL TTML DB](https://amll.dev) — a curated, community-aligned library of word-by-word TTML
 * lyrics, served by a documented public API.
 *
 * Two calls are needed: the search endpoint never includes the lyric body, so a document is found
 * by title (the artist is verified locally — the API's own `artistName` filter misses documents it
 * has, see `selectAmllCandidates`) and then downloaded by id. Both endpoints are documented and
 * cheap, and a document fetched by id never changes, so a hit is worth caching for as long as the
 * lyrics cache keeps anything.
 *
 * Unlike NetEase there is no line-level fallback to order: the library's submissions are word by
 * word, so "the lyrics" is a single best document and [fetch] takes it. What the search still has
 * to resolve is which *submission* the local file came from — `selectAmllCandidates` /
 * `rankAmllCandidates` do that from the artist, the platform ids the document declares, the album
 * tag and the title's exactness.
 *
 * Three things this provider deliberately does to get as much out of the library as possible:
 * - **搜索按曲名变体逐条试**（[amllSearchTerms]）：去噪曲名没结果时再拿标签原文搜一次 ——
 *   词库索引的是提交者登记的写法，`Song (feat. X)` 这类别名只有原文能命中。命中时仍只有一次请求。
 * - **多份候选依次下载**：排名第一的那份可能是纯乐器、占位或损坏的文档。播放最多试
 *   [PLAYBACK_DOWNLOAD_ATTEMPTS] 份，直到拿到一份真的带时间轴的 TTML。
 * - **下载后回头核时长**：`amllExceedsTrack` 只在「歌词比整首歌还长」时否决 —— 那是现场版 /
 *   加长版的文档，不可能是本地这条录音。
 */
@Singleton
class AmllLyricsProvider @Inject constructor(
    sharedClient: OkHttpClient
) : OnlineLyricsProvider {

    override val provider: LyricsProvider = LyricsProvider.AMLL

    private val json = Json { ignoreUnknownKeys = true }

    /** Derived from the shared client so the connection pool and disk cache are reused. */
    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * Playback wants one document, so downloads stop at the first usable one: the common case is
     * one search plus one document, and a track the library has nothing for costs only the search.
     */
    override suspend fun fetch(query: LyricsQuery): OnlineLyrics? =
        collect(query, wanted = 1, maxDownloads = PLAYBACK_DOWNLOAD_ATTEMPTS).firstOrNull()

    /** The version picker can list the other submissions, named after their contributors. */
    override suspend fun fetchCandidates(query: LyricsQuery): List<OnlineLyrics> =
        collect(query, wanted = MAX_LYRIC_CHOICES, maxDownloads = MAX_DOCUMENT_DOWNLOADS)

    /**
     * @param wanted how many *usable* documents the caller needs.
     * @param maxDownloads how many documents may be downloaded trying to reach [wanted] — the slack
     *   is what lets a broken top-ranked submission be skipped instead of ending the lookup.
     */
    private suspend fun collect(query: LyricsQuery, wanted: Int, maxDownloads: Int): List<OnlineLyrics> {
        // 只有曲名是必需的：歌手缺标签时由 `selectAmllCandidates` 用旁证决定采不采信，
        // 而不是整首歌直接跳过 —— 那会让一整类文件的 AMLL 歌词永远取不到。
        if (query.title.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            val outcome = candidates(query, wanted)
            val found = mutableListOf<OnlineLyrics>()
            var failure = outcome.failure
            var downloads = 0

            for (song in outcome.candidates) {
                if (downloads >= maxDownloads) break
                downloads++

                val ttml = try {
                    fetchLyrics(song.id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Another submission of the same recording may still answer, so a failure here
                    // is only remembered. It is rethrown at the end, when nothing came back at all
                    // and the caller has to be sent on to the next provider.
                    failure = failure ?: e
                    continue
                } ?: continue

                // 乐器版、只填了元数据的空文档、以及歌词比整首歌还长的（现场版）都是「这份不能播」。
                // 换下一份，别让一次坏候选把这首歌的歌词判死。
                if (!amllHasTimedLines(ttml) || amllExceedsTrack(ttml, query.durationMs)) continue

                found += OnlineLyrics(rawContent = ttml, label = song.label())
                if (found.size >= wanted) break
            }

            if (found.isEmpty()) failure?.let { throw it }
            found
        }
    }

    /**
     * The library's submissions for the track, best first, gathered over the title variants.
     *
     * 第二条变体只在第一条没凑够 [wanted] 时才请求：命中时成本与以前一样（一次 /search），
     * 漏掉时才多付一次。某一条变体请求失败也只是被记住，另一条照样能试。
     */
    private fun candidates(query: LyricsQuery, wanted: Int): SearchOutcome {
        val seen = LinkedHashMap<Long, AmllSongItemDto>()
        var failure: Exception? = null

        for (term in amllSearchTerms(query)) {
            val matched = selectAmllCandidates(seen.values.toList(), query)
            if (matched.size >= wanted) break

            try {
                // 变体之间会重复命中同一份文档；按 id 留下先到的那份，顺序即服务端排名。
                for (item in searchOnce(term)) if (item.id !in seen) seen[item.id] = item
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failure = failure ?: e
            }
        }

        return SearchOutcome(selectAmllCandidates(seen.values.toList(), query), failure)
    }

    /** One `/search` round trip; the results are unfiltered — the caller selects. */
    private fun searchOnce(term: String): List<AmllSongItemDto> {
        val url = baseUrl("search").apply {
            addQueryParameter("musicName", term)
            addQueryParameter("pageSize", SEARCH_PAGE_SIZE.toString())
        }.build()

        val body = get(url) ?: return emptyList()
        return runCatching { json.decodeFromString<AmllSearchResponseDto>(body) }
            .onFailure { Timber.w(it, "AmllLyricsProvider: unreadable /search response") }
            .getOrNull()
            ?.data
            ?.items
            .orEmpty()
    }

    /** @return the TTML document, or `null` when the library has nothing for [id]. */
    private fun fetchLyrics(id: Long): String? {
        val url = baseUrl("get").apply { addQueryParameter("id", id.toString()) }.build()

        val body = get(url) ?: return null
        return runCatching { json.decodeFromString<AmllGetResponseDto>(body) }
            .onFailure { Timber.w(it, "AmllLyricsProvider: unreadable /get response") }
            .getOrNull()
            ?.data
            ?.lyrics
            ?.takeIf { it.isNotBlank() }
    }

    /** What tells this submission apart from the others of the same recording. */
    private fun AmllSongItemDto.label(): String? =
        authorUsernames.firstOrNull { it.isNotBlank() }
            ?: albumNames.firstOrNull { it.isNotBlank() }

    private fun baseUrl(endpoint: String): HttpUrl.Builder =
        "$BASE_URL/v1/lyrics/$endpoint".toHttpUrl().newBuilder()

    /**
     * @return the response body, or `null` for 404 — which for `/get` means "no such lyrics" rather
     *   than a failure, and must let the next provider try instead of aborting the lookup.
     */
    private fun get(url: HttpUrl): String? {
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        response.use {
            return when {
                it.code == 404 -> null
                !it.isSuccessful -> throw IOException("AMLL returned ${it.code}")
                else -> it.body?.string()
            }
        }
    }

    /** 搜索的结果连同上一次失败：两个都要，调用方才知道「没搜到」与「没搜成」的区别。 */
    private data class SearchOutcome(
        val candidates: List<AmllSongItemDto>,
        val failure: Exception?
    )

    private companion object {
        const val BASE_URL = "https://api.amll.dev"
        const val CALL_TIMEOUT_SECONDS = 15L

        /**
         * 按曲名搜索时最多取多少条。词库的检索是**子串**匹配，泛用曲名（`Love Story`、`晴天`）
         * 会带回翻唱与别的录音，真正筛出「同一个歌手 / 同一份母带」靠的是 `selectAmllCandidates`；
         * 给到 40 是为了让同名曲目多的标题也有机会轮到正确的条目（服务端每页上限 100）。
         */
        const val SEARCH_PAGE_SIZE = 40

        /**
         * How many submissions the version picker lists. Larger than the single document playback
         * needs, because the point of the picker is to offer the ones playback would not reach —
         * contributors time the same recording slightly differently.
         */
        const val MAX_LYRIC_CHOICES = 3

        /** 播放最多下载几份候选：第三份是「前两份都是空文档 / 乐器版」时的兜底。 */
        const val PLAYBACK_DOWNLOAD_ATTEMPTS = 3

        /** 面板要凑够 [MAX_LYRIC_CHOICES] 份可用的，多留两份给被跳过的那几个。 */
        const val MAX_DOCUMENT_DOWNLOADS = 5
    }
}

/** TTML 的每一行歌词都是带 `begin` 的 `<p>`。 */
private val TIMED_TTML_LINE = Regex("<p\\b[^>]*\\bbegin\\s*=")

/** `<body dur="04:25.541">` —— TTML 自己声明的歌词长度。 */
private val TTML_BODY_DURATION = Regex("<body\\b[^>]*\\bdur=\"([^\"]+)\"")

/** 歌词比整首歌长出这么多才算「不是这条录音」：留够片头片尾与母带差异的余量。 */
private const val EXCEEDING_DURATION_MARGIN_MS = 20_000L

/**
 * 这份文档有没有可播的行。
 *
 * 纯乐器与占位文档只有 `<head>` 元数据，没有一行 `<p>`；把它当歌词显示出来就是一片空白 ——
 * 比"没有歌词"更糟，因为面板会认为这一家已经答过了。
 */
internal fun amllHasTimedLines(raw: String): Boolean = TIMED_TTML_LINE.containsMatchIn(raw)

/**
 * 文档声明的歌词是否**超出**本地音频。
 *
 * AMLL 的 `dur` 是最后一句歌词的结束时间而不是音轨长度，所以两者不能直接比：带长尾奏的曲子
 * 会比 `dur` 长得多。但反过来的判定是硬的 —— 歌词本身都比整首歌长了，那一定是另一次录音
 * （现场版、加长版、另一份母带）。所以只做这一个方向，并留 [EXCEEDING_DURATION_MARGIN_MS] 余量。
 */
internal fun amllExceedsTrack(raw: String, durationMs: Long): Boolean {
    if (durationMs <= 0) return false
    val declared = TTML_BODY_DURATION.find(raw)?.groupValues?.get(1) ?: return false
    val endMs = parseTtmlClock(declared) ?: return false
    return endMs > durationMs + EXCEEDING_DURATION_MARGIN_MS
}

/** TTML 的时钟时间：`mm:ss.mmm` 或 `hh:mm:ss.mmm`，转成毫秒。 */
internal fun parseTtmlClock(value: String): Long? {
    val parts = value.trim().split(':')
    if (parts.size !in 2..3) return null
    val seconds = parts.last().toDoubleOrNull() ?: return null
    val minutes = parts[parts.size - 2].toLongOrNull() ?: return null
    val hours = if (parts.size == 3) parts[0].toLongOrNull() ?: return null else 0L
    return (hours * 3600 + minutes * 60) * 1000 + (seconds * 1000).toLong()
}
