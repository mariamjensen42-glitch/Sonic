package com.cycling.data.api.cover

import com.cycling.data.api.cover.dto.ITunesSearchResponse
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
 * Apple's iTunes Search API as a cover source — the only documented, key-less catalogue here.
 *
 * It is the fallback for Western catalogues where the Chinese services are thin. `artworkUrl100`
 * embeds the pixel size in the path (`…/100x100bb.jpg`), so asking for a larger copy is a string
 * replacement rather than a second request.
 */
@Singleton
class ITunesCoverProvider @Inject constructor(
    sharedClient: OkHttpClient
) : CoverArtProvider {

    override val source: CoverSource = CoverSource.ITUNES

    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    override suspend fun search(song: Song): List<CoverCandidate> = withContext(Dispatchers.IO) {
        if (song.title.isBlank()) return@withContext emptyList()

        val url = "$BASE_URL/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", "${song.artist} ${song.title}".trim())
            .addQueryParameter("entity", "song")
            .addQueryParameter("limit", SEARCH_LIMIT.toString())
            .addQueryParameter("country", "cn")
            .build()

        try {
            val body = client.textOf(Request.Builder().url(url).build())
                ?: return@withContext emptyList()
            parseITunesCover(body, song)
        } catch (e: Exception) {
            Timber.w(e, "ITunesCoverProvider: search failed for ${song.title}")
            emptyList()
        }
    }

    private companion object {
        const val BASE_URL = "https://itunes.apple.com"
        const val SEARCH_LIMIT = 10
        const val CALL_TIMEOUT_SECONDS = 15L
    }
}

/** Pure so it can be tested without a network; see `ITunesCoverProviderTest`. */
internal fun parseITunesCover(body: String, song: Song): List<CoverCandidate> {
    val response = runCatching {
        Json { ignoreUnknownKeys = true }.decodeFromString<ITunesSearchResponse>(body)
    }.getOrNull() ?: return emptyList()

    return rankCoverCandidates(
        entries = response.results,
        song = song,
        durationMsOf = { dto -> dto.trackTimeMillis.takeIf { it > 0 } },
        candidateOf = { dto ->
            dto.artworkUrl100
                .takeIf { it.isNotBlank() }
                ?.replace(THUMBNAIL_SIZE, LARGE_SIZE)
                ?.asHttpsUrl()
                ?.let { imageUrl ->
                    CoverCandidate(
                        id = "itunes#$imageUrl",
                        source = CoverSource.ITUNES,
                        imageUrl = imageUrl,
                        album = dto.collectionName.plainText().takeIf { it.isNotEmpty() },
                        artist = dto.artistName.plainText().takeIf { it.isNotEmpty() },
                        title = dto.trackName.plainText().takeIf { it.isNotEmpty() },
                        // The search API exposes one artist per track; for an album result that is
                        // the collection artist, so it doubles as the album artist.
                        albumArtist = dto.artistName.plainText().takeIf { it.isNotEmpty() },
                        year = dto.releaseDate.take(4)
                            .takeIf { year -> year.length == 4 && year.all { it.isDigit() } },
                        genre = dto.primaryGenreName.plainText().takeIf { it.isNotEmpty() },
                        trackNumber = formatOrdinal(dto.trackNumber, dto.trackCount),
                        discNumber = formatOrdinal(dto.discNumber, dto.discCount),
                        durationMs = dto.trackTimeMillis.takeIf { it > 0 }
                    )
                }
        }
    ).take(MAX_COVER_CANDIDATES)
}

/** The size segment Apple embeds in `artworkUrl100`, and the one worth downloading instead. */
private const val THUMBNAIL_SIZE = "100x100bb"
private const val LARGE_SIZE = "600x600bb"
