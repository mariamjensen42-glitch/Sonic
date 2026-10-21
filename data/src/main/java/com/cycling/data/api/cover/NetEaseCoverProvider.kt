package com.cycling.data.api.cover

import com.cycling.data.api.cover.dto.NetEaseDetailResponse
import com.cycling.data.api.lyrics.dto.NetEaseSearchResponse
import com.cycling.domain.model.CoverCandidate
import com.cycling.domain.model.CoverSource
import com.cycling.domain.model.Song
import com.cycling.domain.repository.CoverArtProvider
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * NetEase Cloud Music's public web endpoints, used as a cover source.
 *
 * Two requests per search: the search itself only returns a `picId`, and the CDN path for an
 * image is derived from an encrypted hash that cannot be computed locally, so the ids have to be
 * exchanged for real `picUrl`s through `/api/song/detail`. The search is issued as GET because
 * the same endpoint accepts it that way and the provider then shares one code path with the rest.
 */
@Singleton
class NetEaseCoverProvider @Inject constructor(
    sharedClient: OkHttpClient
) : CoverArtProvider {

    override val source: CoverSource = CoverSource.NETEASE

    private val json = Json { ignoreUnknownKeys = true }

    /** Derived from the shared client so the connection pool and disk cache are reused. */
    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    override suspend fun search(song: Song): List<CoverCandidate> = withContext(Dispatchers.IO) {
        if (song.title.isBlank()) return@withContext emptyList()

        val ids = try {
            searchSongIds(song)
        } catch (e: Exception) {
            Timber.w(e, "NetEaseCoverProvider: search failed for ${song.title}")
            return@withContext emptyList()
        }
        if (ids.isEmpty()) return@withContext emptyList()

        try {
            val body = client.textOf(
                Request.Builder()
                    .url(detailUrl(ids))
                    .headers(apiHeaders())
                    .build()
            ) ?: return@withContext emptyList()
            parseNetEaseCover(body, song)
        } catch (e: Exception) {
            Timber.w(e, "NetEaseCoverProvider: detail failed for ${song.title}")
            emptyList()
        }
    }

    private fun searchSongIds(song: Song): List<Long> {
        val url = "$BASE_URL/api/search/get/web".toHttpUrl().newBuilder()
            .addQueryParameter("s", searchTerm(song))
            .addQueryParameter("type", "1")
            .addQueryParameter("offset", "0")
            .addQueryParameter("limit", SEARCH_LIMIT.toString())
            .addQueryParameter("total", "true")
            .build()

        val body = client.textOf(Request.Builder().url(url).headers(apiHeaders()).build())
            ?: return emptyList()
        val response = runCatching { json.decodeFromString<NetEaseSearchResponse>(body) }
            .onFailure { Timber.w(it, "NetEaseCoverProvider: unreadable search response") }
            .getOrNull()
            ?: return emptyList()

        return response.result?.songs.orEmpty()
            .map { it.id }
            .filter { it > 0 }
            .take(MAX_DETAIL_LOOKUPS)
    }

    private fun detailUrl(ids: List<Long>): String =
        "$BASE_URL/api/song/detail".toHttpUrl().newBuilder()
            .addQueryParameter("ids", ids.joinToString(",", prefix = "[", postfix = "]"))
            .build()
            .toString()

    private fun searchTerm(song: Song): String = "${song.artist} ${song.title}".trim()

    /** The endpoints reject requests that do not look like they came from the web player. */
    private fun apiHeaders(): Headers = Headers.Builder()
        .add("Referer", "$BASE_URL/")
        .add("Cookie", "appver=2.0.2")
        .build()

    private companion object {
        const val BASE_URL = "https://music.163.com"
        const val SEARCH_LIMIT = 10
        const val CALL_TIMEOUT_SECONDS = 15L

        /** One detail request accepts a list, so this is simply how many ids fit in it. */
        const val MAX_DETAIL_LOOKUPS = 6
    }
}

/** Pure so it can be tested without a network; see `CoverProvidersTest`. */
internal fun parseNetEaseCover(body: String, song: Song): List<CoverCandidate> {
    val response = runCatching {
        Json { ignoreUnknownKeys = true }.decodeFromString<NetEaseDetailResponse>(body)
    }.getOrNull() ?: return emptyList()

    return rankCoverCandidates(
        entries = response.songs,
        song = song,
        durationMsOf = { dto -> dto.duration.takeIf { it > 0 } },
        candidateOf = { dto ->
            dto.album?.picUrl
                ?.takeIf { it.isNotBlank() }
                ?.asHttpsUrl()
                ?.let { imageUrl ->
                    CoverCandidate(
                        id = "netease#${dto.id}",
                        source = CoverSource.NETEASE,
                        imageUrl = imageUrl,
                        album = dto.album?.name?.plainText()?.takeIf { it.isNotEmpty() },
                        artist = dto.artists.joinToString(", ") { it.name }
                            .plainText()
                            .takeIf { it.isNotEmpty() },
                        title = dto.name.plainText().takeIf { it.isNotEmpty() },
                        albumArtist = dto.album?.artist?.name
                            ?.plainText()
                            ?.takeIf { it.isNotEmpty() },
                        year = yearOfEpochMillis(dto.album?.publishTime ?: 0),
                        // NetEase has no genre on a track; the album's tags are not the track's
                        // genre, and guessing one would be worse than leaving it blank.
                        genre = null,
                        trackNumber = dto.no.takeIf { it > 0 }?.toString(),
                        discNumber = normalizeOrdinal(dto.disc),
                        durationMs = dto.duration.takeIf { it > 0 }
                    )
                }
        }
    ).take(MAX_COVER_CANDIDATES)
}
