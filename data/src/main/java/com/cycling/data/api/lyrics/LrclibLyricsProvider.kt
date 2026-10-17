package com.cycling.data.api.lyrics

import com.cycling.data.api.lyrics.dto.LrclibTrackDto
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
 * [LRCLIB](https://lrclib.net) — a free, documented, community-run lyrics database.
 *
 * Preferred for Western catalogues. Two calls are tried: `/api/get` matches on all four fields at
 * once and answers 404 when it has no exact hit, so the broader `/api/search` is the fallback
 * rather than the first attempt — the exact one is a single indexed row.
 */
@Singleton
class LrclibLyricsProvider @Inject constructor(
    sharedClient: OkHttpClient
) : OnlineLyricsProvider {

    override val provider: LyricsProvider = LyricsProvider.LRCLIB

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Derived from the shared client so the connection pool and disk cache are reused; the timeout
     * is shortened because lyrics are fetched while a track is already playing and a slow answer is
     * worse than none.
     */
    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * LRCLIB answers with one document per track — line-level only, and one row per recording — so
     * there is never more than one version to offer.
     */
    override suspend fun fetchCandidates(query: LyricsQuery): List<OnlineLyrics> {
        if (query.title.isBlank() || query.artist.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            listOfNotNull(exactMatch(query) ?: search(query))
                .onEach { Timber.d("LrclibLyricsProvider: served ${query.cacheKey}") }
        }
    }

    /** One indexed lookup, but only when every field LRCLIB matches on is actually known. */
    private fun exactMatch(query: LyricsQuery): OnlineLyrics? {
        if (query.album.isBlank() || query.durationMs <= 0) return null

        val url = baseUrl("get")
            .addQueryParameter("track_name", query.title)
            .addQueryParameter("artist_name", query.artist)
            .addQueryParameter("album_name", query.album)
            .addQueryParameter("duration", (query.durationMs / 1000).toString())
            .build()

        val body = get(url) ?: return null
        val track = runCatching { json.decodeFromString<LrclibTrackDto>(body) }
            .onFailure { Timber.w(it, "LrclibLyricsProvider: unreadable /get response") }
            .getOrNull()
            ?: return null

        return selectLrclibLyrics(listOf(track), query)
    }

    /** The free-text fallback; returns many near-misses, so the length match does the real work. */
    private fun search(query: LyricsQuery): OnlineLyrics? {
        val url = baseUrl("search")
            .addQueryParameter("track_name", query.title)
            .addQueryParameter("artist_name", query.artist)
            .build()

        val body = get(url) ?: return null
        val tracks = runCatching { json.decodeFromString<List<LrclibTrackDto>>(body) }
            .onFailure { Timber.w(it, "LrclibLyricsProvider: unreadable /search response") }
            .getOrNull()
            ?: return null

        return selectLrclibLyrics(tracks, query)
    }

    private fun baseUrl(endpoint: String): HttpUrl.Builder =
        "$BASE_URL/api/$endpoint".toHttpUrl().newBuilder()

    /**
     * @return the response body, or `null` for 404 — which for both endpoints means "no such track"
     *   rather than a failure, and must let the next provider try instead of aborting the lookup.
     */
    private fun get(url: HttpUrl): String? {
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        response.use {
            return when {
                it.code == 404 -> null
                !it.isSuccessful -> throw IOException("LRCLIB returned ${it.code}")
                else -> it.body?.string()
            }
        }
    }

    private companion object {
        const val BASE_URL = "https://lrclib.net"
        const val CALL_TIMEOUT_SECONDS = 15L
    }
}
