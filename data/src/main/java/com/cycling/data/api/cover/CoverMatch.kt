package com.cycling.data.api.cover

import com.cycling.domain.model.CoverCandidate
import com.cycling.domain.model.Song
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * How far a provider's track length may sit from the local file before the entry is obviously a
 * different recording. More forgiving than the lyrics tolerance: a wrong-length cover is visible
 * in the picker and the user simply picks another, whereas a wrong-length lyric would be silently
 * played against the wrong audio.
 */
internal const val COVER_DURATION_TOLERANCE_MS = 10_000L

/** How many candidates one source may contribute. */
internal const val MAX_COVER_CANDIDATES = 6

/** `安<em>静</em>` → `安静`: some catalogues mark the matched words up in search results. */
private val HighlightTag = Regex("</?em>")

internal fun String.plainText(): String = replace(HighlightTag, "").trim()

/**
 * `3/12` when the total is known, `3` otherwise, `null` when the source gave nothing.
 *
 * Catalogues disagree about whether a position is "the third track" or "3 of 12", so both are
 * accepted shaped into one form the edit form can round-trip.
 */
internal fun formatOrdinal(number: Int, total: Int): String? = when {
    number <= 0 -> null
    total > 0 -> "$number/$total"
    else -> number.toString()
}

/** `"03"` → `"3"`; anything not numeric is returned trimmed as-is. */
internal fun normalizeOrdinal(text: String): String? {
    val trimmed = text.trim()
    return trimmed.takeIf { it.isNotEmpty() }?.let { it.toIntOrNull()?.toString() ?: it }
}

/** The local-time year of a millisecond timestamp, or `null` when the source did not date it. */
internal fun yearOfEpochMillis(millis: Long): String? {
    if (millis <= 0) return null
    return runCatching {
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).year.toString()
    }.getOrNull()
}

/** Same as [yearOfEpochMillis] for sources that publish seconds instead. */
internal fun yearOfEpochSeconds(seconds: Long): String? =
    if (seconds <= 0) null else yearOfEpochMillis(seconds * 1000)

/**
 * The app blocks cleartext traffic (`targetSdk` 36, no `usesCleartextTraffic`), and some of these
 * catalogues still publish `http://` image URLs. Upgrading is not cosmetic: an `http` cover URL
 * would simply fail to download.
 */
internal fun String.asHttpsUrl(): String =
    if (startsWith("http://")) "https://" + removePrefix("http://") else this

/**
 * The `{…}` slice of [this], tolerating a JSONP wrapper and trailing `);`.
 *
 * QQ Music's endpoint sometimes wraps its JSON in a callback even when `format=json` was asked
 * for, and handing the trailing `);` to the JSON decoder would make an otherwise good response
 * unreadable.
 */
internal fun String.jsonObjectOrNull(): String? {
    val start = indexOf('{')
    val end = lastIndexOf('}')
    return if (start >= 0 && end > start) substring(start, end + 1) else null
}

/**
 * The single HTTP entry point for the cover sources.
 *
 * @return the response body, or `null` for an empty one. A non-2xx code throws: unlike a lyrics
 *   lookup, where a provider answering "nothing" should just be skipped, a transport failure here
 *   is worth logging before the repository falls through to the next source.
 */
internal fun OkHttpClient.textOf(request: Request): String? {
    val response = newCall(request).execute()
    response.use {
        if (!it.isSuccessful) throw IOException("cover source returned ${it.code}")
        return it.body?.string()?.takeIf { body -> body.isNotBlank() }
    }
}

/**
 * Ranks [entries] by how close their length is to the local track's, turns them into candidates,
 * and marks the first one as the provider's best guess when it is close enough to be plausible.
 *
 * Entries the provider could not date are ranked last rather than dropped — a catalogue entry
 * without a length is still a possible match, and the picker shows the user enough context to
 * judge it. Duplicate image URLs are collapsed: the same album can be listed once per track.
 * When even the closest entry is beyond [COVER_DURATION_TOLERANCE_MS], nothing is highlighted as
 * the best match, because a wrong guess is worse than none for a cover the user will see.
 */
internal fun <T> rankCoverCandidates(
    entries: List<T>,
    song: Song,
    durationMsOf: (T) -> Long?,
    candidateOf: (T) -> CoverCandidate?
): List<CoverCandidate> {
    val ranked = entries.mapNotNull { entry ->
        val distance = durationMsOf(entry)?.takeIf { it > 0 }?.let { abs(it - song.duration) }
        candidateOf(entry)?.let { candidate -> candidate to distance }
    }
        .sortedBy { (_, distance) -> distance ?: Long.MAX_VALUE }
        .distinctBy { (candidate, _) -> candidate.imageUrl }

    val candidates = ranked.map { (candidate, _) -> candidate }
    if (candidates.isEmpty()) return emptyList()

    val bestDistance = ranked.first().second
    val bestIsPlausible = song.duration <= 0 ||
        bestDistance == null ||
        bestDistance <= COVER_DURATION_TOLERANCE_MS

    return candidates.mapIndexed { index, candidate ->
        if (index == 0 && bestIsPlausible) candidate.copy(isBestMatch = true) else candidate
    }
}
