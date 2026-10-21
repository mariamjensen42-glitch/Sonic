package com.cycling.data.api.lyrics

import com.cycling.data.api.lyrics.dto.KugouLyricCandidateDto
import com.cycling.data.api.lyrics.dto.KugouLyricDownloadResponse
import com.cycling.data.api.lyrics.dto.KugouLyricSearchResponse
import com.cycling.domain.lyrics.provider.OnlineLyrics
import com.cycling.domain.lyrics.provider.OnlineLyricsProvider
import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.model.LyricsQuery
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * Kugou Music's public web lyric endpoints.
 *
 * Two calls, no song id needed up front: the search endpoint takes the *keywords* and answers with
 * a list of uploaded lyric documents (`candidates`), each already carrying the id and access key
 * that the download endpoint wants. The download then serves a base64-encoded **LRC**, which the
 * project's `LrcParser` reads as line-level lyrics.
 *
 * The search is keyword-based, so the candidate list can contain covers and wrong edits — the
 * duration is what separates them, and [candidatesByDurationDistance] applies the same tight
 * tolerance the other providers use. A wrong-length lyric is worse than none.
 *
 * This is the same catalogue family as [com.cycling.data.api.cover.KugouCoverProvider] but a
 * different pair of hosts, and it only ever returns line-level lyrics: Kugou's word-by-word KRC
 * would need its own encrypted-container parser, which is not worth adding for a second-best
 * document. Line-level rows are re-checked after a day by the cache TTL, so a word-by-word source
 * can still win later.
 */
@Singleton
class KugouLyricsProvider @Inject constructor(
    sharedClient: OkHttpClient
) : OnlineLyricsProvider {

    override val provider: LyricsProvider = LyricsProvider.KUGOU

    private val json = Json { ignoreUnknownKeys = true }

    /** Derived from the shared client so the connection pool and disk cache are reused. */
    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /** Playback downloads one document; the search is the only extra request. */
    override suspend fun fetch(query: LyricsQuery): OnlineLyrics? =
        collect(query, limit = 1).firstOrNull()

    /** The version picker can list the other uploads, named after their singer. */
    override suspend fun fetchCandidates(query: LyricsQuery): List<OnlineLyrics> =
        collect(query, limit = MAX_LYRIC_CHOICES)

    private suspend fun collect(query: LyricsQuery, limit: Int): List<OnlineLyrics> {
        if (query.title.isBlank() || query.artist.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            val found = mutableListOf<OnlineLyrics>()
            var failure: Exception? = null

            for (candidate in searchCandidates(query).take(limit)) {
                val lrc = try {
                    download(candidate)
                } catch (e: Exception) {
                    // Another upload of the same recording may still answer; a failure is only
                    // remembered and rethrown when nothing came back at all.
                    failure = failure ?: e
                    continue
                } ?: continue

                found += OnlineLyrics(
                    rawContent = lrc,
                    label = candidate.singer.takeIf { it.isNotBlank() }
                )
            }

            if (found.isEmpty()) failure?.let { throw it }
            found
        }
    }

    private fun searchCandidates(query: LyricsQuery): List<KugouLyricCandidateDto> {
        val url = SEARCH_URL.toHttpUrl().newBuilder()
            .addQueryParameter("ver", "1")
            .addQueryParameter("man", "yes")
            .addQueryParameter("client", "mobi")
            .addQueryParameter("keyword", "${query.artist} ${query.title}".trim())
            // 本地时长也提交过去：服务端会顺手排一遍，本地再按同一容差筛一遍。
            .addQueryParameter("duration", query.durationMs.toString())
            .addQueryParameter("hash", "")
            .build()

        val body = get(url) ?: return emptyList()
        return parseKugouLyricCandidates(body, query)
    }

    private fun download(candidate: KugouLyricCandidateDto): String? {
        val url = DOWNLOAD_URL.toHttpUrl().newBuilder()
            .addQueryParameter("ver", "1")
            .addQueryParameter("client", "pc")
            .addQueryParameter("id", candidate.id)
            .addQueryParameter("accesskey", candidate.accesskey)
            .addQueryParameter("fmt", "lrc")
            .addQueryParameter("charset", "utf8")
            .build()

        val body = get(url) ?: return null
        val response = runCatching { json.decodeFromString<KugouLyricDownloadResponse>(body) }
            .onFailure { Timber.w(it, "KugouLyricsProvider: unreadable download response") }
            .getOrNull()
            ?: return null

        return decodeKugouLrc(response.content)
    }

    /** @return the response body, or `null` when the endpoint answered without one. */
    private fun get(url: HttpUrl): String? {
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        response.use {
            if (!it.isSuccessful) throw IOException("Kugou returned ${it.code}")
            return it.body?.string()?.takeIf { body -> body.isNotBlank() }
        }
    }

    private companion object {
        const val SEARCH_URL = "https://krcs.kugou.com/search"
        const val DOWNLOAD_URL = "https://lyrics.kugou.com/download"
        const val CALL_TIMEOUT_SECONDS = 15L

        /** How many uploads the version picker lists. Playback only ever downloads one. */
        const val MAX_LYRIC_CHOICES = 3
    }
}

/** Any LRC timing tag; used to tell a decoded document from base64 noise or a plain error body. */
private val KUGOU_TIMING_TAG = Regex("\\[\\d{1,2}:\\d{1,2}\\.\\d{2,3}]")

/**
 * The candidates worth downloading, closest in length first.
 *
 * Pure so it can be tested without a network; see `LyricsProvidersTest`.
 */
internal fun parseKugouLyricCandidates(
    body: String,
    query: LyricsQuery
): List<KugouLyricCandidateDto> {
    val response = runCatching {
        Json { ignoreUnknownKeys = true }.decodeFromString<KugouLyricSearchResponse>(body)
    }.getOrNull() ?: return emptyList()

    val usable = response.candidates.filter { it.id.isNotBlank() && it.accesskey.isNotBlank() }
    return candidatesByDurationDistance(usable, query, KugouLyricCandidateDto::durationMs)
}

/**
 * Decodes the base64 LRC the download endpoint returns.
 *
 * @return the LRC, or `null` when the field was empty, was not valid base64, or decoded to
 *   something without a single timing tag — a lyric-less track answers `200` with an empty payload.
 */
internal fun decodeKugouLrc(content: String?): String? {
    if (content.isNullOrBlank()) return null
    val decoded = runCatching {
        String(Base64.getDecoder().decode(content), Charsets.UTF_8)
    }.getOrNull() ?: return null
    return decoded.takeIf { it.contains(KUGOU_TIMING_TAG) }
}
