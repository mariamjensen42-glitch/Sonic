package com.cycling.data.api.lyrics

import com.cycling.data.api.lyrics.dto.NetEaseLyricResponse
import com.cycling.data.api.lyrics.dto.NetEaseSearchResponse
import com.cycling.data.api.lyrics.dto.NetEaseSongDto
import com.cycling.domain.lyrics.provider.OnlineLyrics
import com.cycling.domain.lyrics.provider.OnlineLyricsProvider
import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.model.LyricsQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NetEase Cloud Music's public web endpoints.
 *
 * A search picks a song id, then a lyric lookup fetches that song's documents. The endpoints are
 * unofficial and rate-limited, so every response is treated as best-effort — anything unexpected
 * decodes to "nothing found" and lets the next provider run.
 *
 * The lyric endpoint can answer with four documents — line-level `lrc`, word-by-word `yrc` and a
 * translation for each — of which only some tracks have all. All four are requested and folded into
 * one blob by [mergeNetEaseLyrics]; a track with word-by-word lyrics gets those, and everyone else
 * still gets the timed translation.
 *
 * NetEase also lists one recording several times over — an album cut, a compilation, a remaster —
 * and the uploads do not all carry the word-by-word document, so picking a single id by length
 * lands on an entry that has only `lrc` as often as not. The entries within the length tolerance
 * are therefore walked in order until one answers word by word, with the first line-level answer
 * kept as a fallback: a track whose lyrics really are line-level everywhere still gets them, and
 * one that has them word by word somewhere gets those instead.
 *
 * [fetchCandidates] runs the same walk without the early exit, so the lyrics screen's version
 * picker can show the uploads playback would never reach.
 */
@Singleton
class NetEaseLyricsProvider @Inject constructor(
    sharedClient: OkHttpClient
) : OnlineLyricsProvider {

    override val provider: LyricsProvider = LyricsProvider.NETEASE

    private val json = Json { ignoreUnknownKeys = true }

    /** Derived from the shared client so the connection pool and disk cache are reused. */
    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    override suspend fun fetch(query: LyricsQuery): OnlineLyrics? =
        collect(query, limit = MAX_LYRIC_LOOKUPS, stopAtWordByWord = true).firstOrNull()

    override suspend fun fetchCandidates(query: LyricsQuery): List<OnlineLyrics> =
        collect(query, limit = MAX_LYRIC_CHOICES, stopAtWordByWord = false)

    /**
     * Walks the track's entries and keeps the ones that answered, word-by-word ones first.
     *
     * @param stopAtWordByWord stop as soon as a word-by-word answer arrives. That is what playback
     *   wants — it makes the common case a single request — while the version picker wants the
     *   rest of the list too, since a later entry may be a different mastering the user prefers.
     */
    private suspend fun collect(
        query: LyricsQuery,
        limit: Int,
        stopAtWordByWord: Boolean
    ): List<OnlineLyrics> {
        if (query.title.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            val wordByWord = mutableListOf<OnlineLyrics>()
            val lineLevel = mutableListOf<OnlineLyrics>()
            var failure: Exception? = null

            for (song in candidatesFor(query, limit)) {
                val document = try {
                    fetchLyric(song.id)
                } catch (e: Exception) {
                    // Another entry of the same track may still answer, so a failure here is only
                    // remembered. It is rethrown at the end, when nothing came back at all and the
                    // caller has to be sent on to the next provider.
                    failure = failure ?: e
                    continue
                } ?: continue

                val entry = OnlineLyrics(document.content, song.label())
                if (document.wordByWord) {
                    wordByWord += entry
                    if (stopAtWordByWord) break
                } else {
                    lineLevel += entry
                }
            }

            // Word-by-word versions head the list, so the first element is always the best version
            // this source can offer and [fetch] can simply take it.
            val found = wordByWord + lineLevel
            if (found.isEmpty()) failure?.let { throw it }
            found
        }
    }

    /**
     * The provider's entries for the track, the most plausible first, capped at [limit].
     *
     * 排序不再只看时长：曲名对不上的（翻唱、同名歌、伴奏）在这里就被剔掉，见 [rankCandidates]。
     * 本地没有时长时也能排 —— 曲名与歌手已经足够判断是不是同一首。
     */
    private fun candidatesFor(query: LyricsQuery, limit: Int): List<NetEaseSongDto> {
        val body = search(query) ?: return emptyList()
        val songs = runCatching { json.decodeFromString<NetEaseSearchResponse>(body) }
            .onFailure { Timber.w(it, "NetEaseLyricsProvider: unreadable search response") }
            .getOrNull()
            ?.result
            ?.songs
            .orEmpty()

        return rankCandidates(
            songs,
            query,
            NetEaseSongDto::name,
            NetEaseSongDto::artistNames,
            NetEaseSongDto::durationMs
        )
            .filter { it.id > 0 }
            .take(limit)
    }

    /** What tells this upload apart from the others of the same recording, when the provider said. */
    private fun NetEaseSongDto.label(): String? =
        album?.name?.takeIf { it.isNotBlank() } ?: artistNames.takeIf { it.isNotBlank() }

    private fun fetchLyric(songId: Long): NetEaseLyrics? {
        val url = "$BASE_URL/api/song/lyric".toHttpUrl().newBuilder()
            .addQueryParameter("id", songId.toString())
            // -1 asks each document for its full text rather than a truncated preview — a document
            // that is not asked for this way comes back empty, not short.
            .addQueryParameter("lv", "-1")
            .addQueryParameter("kv", "-1")
            .addQueryParameter("tv", "-1")
            .addQueryParameter("yv", "-1")
            .addQueryParameter("ytv", "-1")
            .build()

        val body = request(Request.Builder().url(url).headers(apiHeaders()).build()) ?: return null
        val response = runCatching { json.decodeFromString<NetEaseLyricResponse>(body) }
            .onFailure { Timber.w(it, "NetEaseLyricsProvider: unreadable lyric response") }
            .getOrNull()
            ?: return null

        // Tracks with no lyrics still answer 200, with no lyric document at all — another entry of
        // the same track may yet have them, so this is a `null` for the caller to walk past.
        val content = mergeNetEaseLyrics(response) ?: return null
        return NetEaseLyrics(content, response.hasWordByWord())
    }

    private fun search(query: LyricsQuery): String? {
        // NetEase ranks "artist title" better than the reverse, which matters when the title is a
        // common word and the artist is what disambiguates it. 关键词用的是去噪后的曲名与主歌手
        // （见 [searchKeywords]），标签里的 "(Live)"、"- Remastered" 只会把结果筛掉。
        val form = FormBody.Builder()
            .add("s", query.searchKeywords())
            .add("type", "1")
            .add("offset", "0")
            .add("limit", SEARCH_LIMIT.toString())
            .add("total", "true")
            .build()

        val httpRequest = Request.Builder()
            .url("$BASE_URL/api/search/get/web")
            .headers(apiHeaders())
            .post(form)
            .build()

        return request(httpRequest)
    }

    /**
     * The endpoints reject requests that do not look like they came from the web player — without
     * these headers they answer with an error body instead of results.
     */
    private fun apiHeaders(): Headers = Headers.Builder()
        .add("Referer", "$BASE_URL/")
        .add("Cookie", "appver=2.0.2")
        .build()

    /** @return the response body, or `null` when the endpoint reports "nothing found". */
    private fun request(request: Request): String? {
        val response = client.newCall(request).execute()
        response.use {
            if (!it.isSuccessful) throw IOException("NetEase returned ${it.code}")
            val body = it.body?.string()
            // A non-JSON body is what a rate limit or a blocked request looks like from here.
            if (body.isNullOrBlank() || !body.trimStart().startsWith("{")) {
                Timber.w("NetEaseLyricsProvider: unexpected response body")
                return null
            }
            return body
        }
    }

    /**
     * A merged lyric blob, plus whether the endpoint sent it word by word — the one thing that
     * tells the caller whether another entry of the same track is still worth asking about.
     */
    private data class NetEaseLyrics(val content: String, val wordByWord: Boolean)

    private companion object {
        const val BASE_URL = "https://music.163.com"
        const val SEARCH_LIMIT = 10
        const val CALL_TIMEOUT_SECONDS = 15L

        /**
         * How many entries of the same track are asked for their lyrics.
         *
         * NetEase publishes one recording many times over and only some uploads carry the
         * word-by-word document, so the entry closest in length is not always the best one. The
         * entries are walked in order of length, which keeps the common case down to a single
         * request — the closest entry is usually the one with word-by-word lyrics too — while the
         * cap keeps a track that has none anywhere, the only case that pays for every step, to a
         * handful of requests.
         */
        const val MAX_LYRIC_LOOKUPS = 3

        /**
         * How many entries the version picker is willing to list.
         *
         * Larger than [MAX_LYRIC_LOOKUPS] because there is nothing to stop at — the point of the
         * picker is to show the versions playback would not reach — but still bounded, since every
         * entry costs a request and a track can be published dozens of times.
         */
        const val MAX_LYRIC_CHOICES = 6
    }
}
