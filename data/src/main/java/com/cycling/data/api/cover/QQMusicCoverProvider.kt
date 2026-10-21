package com.cycling.data.api.cover

import com.cycling.data.api.cover.dto.QqSearchResponse
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
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * QQ Music's web search endpoint as a cover source.
 *
 * One request is enough: the result carries the album's `albummid`, and QQ's image CDN addresses
 * covers by that mid, so the URL is built locally. The endpoint answers with JSON when asked for
 * `format=json`, but some edges still wrap it in a callback prefix, which [parseQqCover] tolerates.
 */
@Singleton
class QQMusicCoverProvider @Inject constructor(
    sharedClient: OkHttpClient
) : CoverArtProvider {

    override val source: CoverSource = CoverSource.QQ_MUSIC

    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    override suspend fun search(song: Song): List<CoverCandidate> = withContext(Dispatchers.IO) {
        if (song.title.isBlank()) return@withContext emptyList()

        val url = "$BASE_URL/soso/fcgi-bin/client_search_cp".toHttpUrl().newBuilder()
            .addQueryParameter("w", "${song.artist} ${song.title}".trim())
            .addQueryParameter("format", "json")
            .addQueryParameter("n", SEARCH_LIMIT.toString())
            .addQueryParameter("p", "1")
            .addQueryParameter("cr", "1")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Referer", "$BASE_URL/")
            .build()

        try {
            val body = client.textOf(request) ?: return@withContext emptyList()
            parseQqCover(body, song)
        } catch (e: Exception) {
            Timber.w(e, "QQMusicCoverProvider: search failed for ${song.title}")
            emptyList()
        }
    }

    private companion object {
        const val BASE_URL = "https://c.y.qq.com"
        const val SEARCH_LIMIT = 10
        const val CALL_TIMEOUT_SECONDS = 15L
    }
}

/** Pure so it can be tested without a network; see `CoverProvidersTest`. */
internal fun parseQqCover(body: String, song: Song): List<CoverCandidate> {
    // Drop any JSONP wrapper or trailing `);` before the payload: the endpoint sometimes wraps
    // the JSON in a callback even when `format=json` was asked for.
    val payload = body.jsonObjectOrNull() ?: return emptyList()
    val response = runCatching {
        Json { ignoreUnknownKeys = true }.decodeFromString<QqSearchResponse>(payload)
    }.getOrNull() ?: return emptyList()

    val songs = response.data?.song?.list.orEmpty()
    return rankCoverCandidates(
        entries = songs,
        song = song,
        durationMsOf = { dto -> dto.interval.takeIf { it > 0 }?.let { it * 1000L } },
        candidateOf = { dto ->
            dto.albummid
                .takeIf { it.isNotBlank() }
                ?.let { mid ->
                    val durationMs = dto.interval.takeIf { it > 0 }?.let { it * 1000L }
                    CoverCandidate(
                        id = "qq#$mid",
                        source = CoverSource.QQ_MUSIC,
                        imageUrl = "${QQ_IMAGE_BASE}${mid}.jpg",
                        album = dto.albumname.plainText().takeIf { it.isNotEmpty() },
                        artist = dto.singer.joinToString(",") { it.name }
                            .plainText()
                            .takeIf { it.isNotEmpty() },
                        title = dto.songname.plainText().takeIf { it.isNotEmpty() },
                        // QQ's web search has no track/disc/genre fields, and its album artist is
                        // not distinguishable from the performers here.
                        year = yearOfEpochSeconds(dto.pubtime),
                        durationMs = durationMs
                    )
                }
        }
    ).take(MAX_COVER_CANDIDATES)
}

/** QQ's cover CDN: `T002R500x500M000` asks for a 500×500 JPEG for album mid [mid]. */
private const val QQ_IMAGE_BASE = "https://y.qq.com/music/photo_new/T002R500x500M000"
