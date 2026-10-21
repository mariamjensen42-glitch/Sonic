package com.cycling.data.api.cover

import com.cycling.data.api.cover.dto.KugouSearchResponse
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
 * Kugou's web search endpoint as a cover source.
 *
 * The result already carries an `Image` template like
 * `http://imge.kugou.com/stdmusic/{size}/2020….jpg`, so one request is enough; the provider fills
 * the size placeholder with [COVER_IMAGE_SIZE] and upgrades the scheme, since the app blocks
 * cleartext traffic. The response keys are capitalised, unlike every other catalogue here.
 */
@Singleton
class KugouCoverProvider @Inject constructor(
    sharedClient: OkHttpClient
) : CoverArtProvider {

    override val source: CoverSource = CoverSource.KUGOU

    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    override suspend fun search(song: Song): List<CoverCandidate> = withContext(Dispatchers.IO) {
        if (song.title.isBlank()) return@withContext emptyList()

        val url = "$BASE_URL/song_search_v2".toHttpUrl().newBuilder()
            .addQueryParameter("keyword", "${song.artist} ${song.title}".trim())
            .addQueryParameter("page", "1")
            .addQueryParameter("pagesize", SEARCH_LIMIT.toString())
            .addQueryParameter("platform", "WebFilter")
            .addQueryParameter("userid", "-1")
            .addQueryParameter("clientver", "2000")
            .addQueryParameter("iscorrection", "1")
            .addQueryParameter("privilege_filter", "0")
            .addQueryParameter("filter", "10")
            .addQueryParameter("area_code", "1")
            .addQueryParameter("tagtype", "0")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Referer", "$BASE_URL/")
            .build()

        try {
            val body = client.textOf(request) ?: return@withContext emptyList()
            parseKugouCover(body, song)
        } catch (e: Exception) {
            Timber.w(e, "KugouCoverProvider: search failed for ${song.title}")
            emptyList()
        }
    }

    private companion object {
        const val BASE_URL = "https://songsearch.kugou.com"
        const val SEARCH_LIMIT = 10
        const val CALL_TIMEOUT_SECONDS = 15L
    }
}

/** Kugou's CDN takes the pixel size in the path; 500 is the largest the template offers. */
private const val KUGOU_COVER_IMAGE_SIZE = "500"

/** Pure so it can be tested without a network; see `CoverProvidersTest`. */
internal fun parseKugouCover(body: String, song: Song): List<CoverCandidate> {
    val payload = body.jsonObjectOrNull() ?: return emptyList()
    val response = runCatching {
        Json { ignoreUnknownKeys = true }.decodeFromString<KugouSearchResponse>(payload)
    }.getOrNull() ?: return emptyList()

    val songs = response.data?.lists.orEmpty()
    return rankCoverCandidates(
        entries = songs,
        song = song,
        durationMsOf = { dto -> dto.durationSeconds.takeIf { it > 0 }?.let { it * 1000L } },
        candidateOf = { dto ->
            dto.image
                .takeIf { it.isNotBlank() }
                ?.replace("{size}", KUGOU_COVER_IMAGE_SIZE)
                ?.asHttpsUrl()
                ?.let { imageUrl ->
                    val durationMs = dto.durationSeconds.takeIf { it > 0 }?.let { it * 1000L }
                    CoverCandidate(
                        id = "kugou#$imageUrl",
                        source = CoverSource.KUGOU,
                        imageUrl = imageUrl,
                        album = dto.albumName.plainText().takeIf { it.isNotEmpty() },
                        artist = dto.singerName.plainText().takeIf { it.isNotEmpty() },
                        title = dto.songName.plainText().takeIf { it.isNotEmpty() },
                        // The search endpoint carries no year/track/genre fields.
                        durationMs = durationMs
                    )
                }
        }
    ).take(MAX_COVER_CANDIDATES)
}
