package com.cycling.data.api.lyrics

import com.cycling.data.api.cover.dto.QqSearchResponse
import com.cycling.data.api.cover.dto.QqSongDto
import com.cycling.data.api.cover.jsonObjectOrNull
import com.cycling.data.api.lyrics.dto.QqLyricResponse
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

/**
 * QQ Music's public web lyric endpoints.
 *
 * The search is the same `client_search_cp` call the cover provider already uses (and it is the
 * only search that works for both), so the DTO is shared; the lyric is fetched by `songmid` from
 * `fcg_query_lyric_new.fcg`. Both endpoints want a `y.qq.com` referer — without it the lyric host
 * answers with an error body.
 *
 * The lyric comes back as a plain **LRC**, optionally with a second LRC holding the translation on
 * the same timestamps. The project's LRC parser pairs adjacent lines with an equal start into
 * original + translation, so [mergeQqTranslation] interleaves the two documents instead of
 * concatenating them — concatenating would turn every translation into an extra lyric line.
 *
 * Like Kugou this is line-level only: QQ's word-by-word QRC is an encrypted container and not worth
 * a parser for a second-best document.
 */
@Singleton
class QQMusicLyricsProvider @Inject constructor(
    sharedClient: OkHttpClient
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

    private suspend fun collect(query: LyricsQuery, limit: Int): List<OnlineLyrics> {
        if (query.title.isBlank() || query.artist.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            val found = mutableListOf<OnlineLyrics>()
            var failure: Exception? = null

            for (song in searchCandidates(query).take(limit)) {
                val lrc = try {
                    fetchLyric(song.songmid)
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

    private fun searchCandidates(query: LyricsQuery): List<QqSongDto> {
        val url = SEARCH_URL.toHttpUrl().newBuilder()
            .addQueryParameter("w", "${query.artist} ${query.title}".trim())
            .addQueryParameter("format", "json")
            .addQueryParameter("n", SEARCH_LIMIT.toString())
            .addQueryParameter("p", "1")
            .addQueryParameter("cr", "1")
            .build()

        val body = get(url, SEARCH_REFERER) ?: return emptyList()
        return parseQqLyricCandidates(body, query)
    }

    /** @return the merged LRC, or `null` when QQ has no lyric for [songmid]. */
    private fun fetchLyric(songmid: String): String? {
        val url = LYRIC_URL.toHttpUrl().newBuilder()
            .addQueryParameter("songmid", songmid)
            .addQueryParameter("format", "json")
            // 明文 LRC；部分边缘节点仍返回 base64，解析时两样都认。
            .addQueryParameter("nobase64", "1")
            .addQueryParameter("g_tk", "5381")
            .addQueryParameter("loginUin", "0")
            .addQueryParameter("hostUin", "0")
            .addQueryParameter("platform", "yqq")
            .addQueryParameter("needNewCode", "0")
            .build()

        val body = get(url, LYRIC_REFERER) ?: return null
        return parseQqLyrics(body)
    }

    /** @return the response body, or `null` when the endpoint answered without one. */
    private fun get(url: HttpUrl, referer: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Referer", referer)
            .build()

        val response = client.newCall(request).execute()
        response.use {
            if (!it.isSuccessful) throw IOException("QQ Music returned ${it.code}")
            return it.body?.string()?.takeIf { body -> body.isNotBlank() }
        }
    }

    private companion object {
        const val SEARCH_URL = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp"
        const val LYRIC_URL = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg"

        /** Both hosts want a web-player referer; the lyric host rejects requests without one. */
        const val SEARCH_REFERER = "https://c.y.qq.com/"
        const val LYRIC_REFERER = "https://y.qq.com/portal/player.html"

        const val SEARCH_LIMIT = 10
        const val CALL_TIMEOUT_SECONDS = 15L

        /** How many matches the version picker lists. Playback only ever downloads one. */
        const val MAX_LYRIC_CHOICES = 3
    }
}

/** Any LRC timing tag; used to tell a decoded document from base64 noise or an error body. */
private val QQ_TIMING_TAG = Regex("\\[\\d{1,2}:\\d{1,2}\\.\\d{2,3}]")

/**
 * The search hits worth asking for a lyric, closest in length first.
 *
 * The keyword search also answers covers and unrelated songs, so the length is what separates
 * them — the same tight tolerance the other providers use. Entries without a `songmid` cannot be
 * downloaded at all and are dropped first.
 *
 * Pure so it can be tested without a network; see `LyricsProvidersTest`.
 */
internal fun parseQqLyricCandidates(body: String, query: LyricsQuery): List<QqSongDto> {
    val payload = body.jsonObjectOrNull() ?: return emptyList()
    val response = runCatching {
        Json { ignoreUnknownKeys = true }.decodeFromString<QqSearchResponse>(payload)
    }.getOrNull() ?: return emptyList()

    val usable = response.data?.song?.list.orEmpty().filter { it.songmid.isNotBlank() }
    return candidatesByDurationDistance(usable, query) { song ->
        song.interval.takeIf { it > 0 }?.let { it * 1000L }
    }
}

/**
 * Reads the lyric response into one merged LRC, or `null` when QQ has no lyrics for the song.
 *
 * `retcode` / `code` are checked because failures arrive as HTTP 200, and both text fields are
 * sniffed: `nobase64=1` is honoured by most edges but not all.
 */
internal fun parseQqLyrics(body: String): String? {
    val payload = body.jsonObjectOrNull() ?: return null
    val response = runCatching {
        Json { ignoreUnknownKeys = true }.decodeFromString<QqLyricResponse>(payload)
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
