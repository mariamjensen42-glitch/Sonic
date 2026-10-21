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
 * to resolve is which *release* the local file came from, which [selectAmllCandidates] does from
 * the album tag after checking the artist.
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
     * Playback wants one document, so only the best-ranked submission is downloaded: the common
     * case is the search plus one document, and a track the library has nothing for costs only the
     * search.
     */
    override suspend fun fetch(query: LyricsQuery): OnlineLyrics? =
        collect(query, limit = 1).firstOrNull()

    /** The version picker can list the other submissions, named after their contributors. */
    override suspend fun fetchCandidates(query: LyricsQuery): List<OnlineLyrics> =
        collect(query, limit = MAX_LYRIC_CHOICES)

    private suspend fun collect(query: LyricsQuery, limit: Int): List<OnlineLyrics> {
        // Both fields are required: the provider never searches by title alone, because the library
        // has several songs per common title and picking one by title would hand the user another
        // song's lyrics. A track without an artist tag misses here and is served by NetEase, which
        // searches free text and therefore still copes with one.
        if (query.title.isBlank() || query.artist.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            val found = mutableListOf<OnlineLyrics>()
            var failure: Exception? = null

            for (song in search(query).take(limit)) {
                val ttml = try {
                    fetchLyrics(song.id)
                } catch (e: Exception) {
                    // Another submission of the same recording may still answer, so a failure here
                    // is only remembered. It is rethrown at the end, when nothing came back at all
                    // and the caller has to be sent on to the next provider.
                    failure = failure ?: e
                    continue
                } ?: continue

                found += OnlineLyrics(rawContent = ttml, label = song.label())
            }

            if (found.isEmpty()) failure?.let { throw it }
            found
        }
    }

    /** The library's submissions for the track, the one for the local release first. */
    private fun search(query: LyricsQuery): List<AmllSongItemDto> {
        val url = baseUrl("search").apply {
            addQueryParameter("musicName", query.title)
            addQueryParameter("pageSize", SEARCH_PAGE_SIZE.toString())
        }.build()

        val body = get(url) ?: return emptyList()
        val items = runCatching { json.decodeFromString<AmllSearchResponseDto>(body) }
            .onFailure { Timber.w(it, "AmllLyricsProvider: unreadable /search response") }
            .getOrNull()
            ?.data
            ?.items
            .orEmpty()

        // 只按曲名搜索，歌手在本地校验：服务端的 artistName 过滤对部分歌手会漏掉它自己的文档
        // （见 `selectAmllCandidates` 的说明），而本地校验过之后标题重名也不会串到别人的歌词。
        return selectAmllCandidates(items, query)
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

    private companion object {
        const val BASE_URL = "https://api.amll.dev"
        const val CALL_TIMEOUT_SECONDS = 15L

        /**
         * 按曲名搜索时最多取多少条。标题重名的曲子会一起返回，真正筛出「同一个歌手」靠的是
         * `selectAmllCandidates`；给到 20 是为了让同名曲目多的标题也有机会轮到正确的条目
         * （服务端每页上限 100）。
         */
        const val SEARCH_PAGE_SIZE = 20

        /**
         * How many submissions the version picker lists. Larger than the single document playback
         * needs, because the point of the picker is to offer the ones playback would not reach —
         * contributors time the same recording slightly differently.
         */
        const val MAX_LYRIC_CHOICES = 3
    }
}
