package com.cycling.data.api.lyrics

import com.cycling.data.api.lyrics.dto.LrclibTrackDto
import com.cycling.domain.lyrics.provider.OnlineLyrics
import com.cycling.domain.model.LyricsQuery
import kotlin.math.abs

/**
 * How far a candidate track's length may differ from the local file's and still be trusted.
 *
 * A track matched to the wrong recording is worse than no lyrics at all, so this is deliberately
 * tight. Real differences come from re-encodes and intros/outros trimmed by a second or two.
 */
internal const val DURATION_TOLERANCE_MS = 5_000L

/**
 * The candidates worth asking about, closest in length first, or empty when none is close enough.
 *
 * Duration is the only signal shared by every provider that does not depend on how title and
 * artist happen to be spelled — a cover, a live version and a remaster all share a name, and only
 * duration separates them. A provider that publishes the same recording many times over — as
 * NetEase does — therefore gets its entries back in this order, because the closest entry is not
 * always the one carrying the richest document. Ordering is stable, so entries that tie keep the
 * order the provider ranked them in.
 *
 * A candidate whose length the provider did not report is ranked last rather than rejected: when
 * MediaStore also failed to date the local track there is nothing to compare against, and a
 * plausible match is better than none. `sortedBy` would treat a null key as the *smallest*, so the
 * sentinel has to be applied here rather than left to the comparison.
 */
internal fun <T> candidatesByDurationDistance(
    candidates: List<T>,
    query: LyricsQuery,
    durationMsOf: (T) -> Long?
): List<T> {
    val ranked = candidates.sortedBy { durationDistance(it, query, durationMsOf) ?: Long.MAX_VALUE }
    if (query.durationMs <= 0) return ranked
    return ranked.filter {
        (durationDistance(it, query, durationMsOf) ?: return@filter false) <= DURATION_TOLERANCE_MS
    }
}

/** The candidate whose length is closest to the local track's, or `null` when none is close enough. */
internal fun <T> bestDurationMatch(
    candidates: List<T>,
    query: LyricsQuery,
    durationMsOf: (T) -> Long?
): T? = candidatesByDurationDistance(candidates, query, durationMsOf).firstOrNull()

/** How far [candidate] is from the local track's length, or `null` when the provider did not say. */
private fun <T> durationDistance(candidate: T, query: LyricsQuery, durationMsOf: (T) -> Long?): Long? =
    durationMsOf(candidate)?.let { abs(it - query.durationMs) }

/**
 * Picks the one candidate worth downloading, or `null` if the provider has nothing usable.
 *
 * Candidates without *synced* lyrics are dropped: `plainLyrics` has no timestamps, and the player
 * can only render a `SyncedLyrics`. An instrumental is skipped for the same reason a blank one is —
 * there is nothing to display.
 */
internal fun selectLrclibLyrics(candidates: List<LrclibTrackDto>, query: LyricsQuery): OnlineLyrics? {
    val usable = candidates.filter { !it.instrumental && !it.syncedLyrics.isNullOrBlank() }
    val match = bestDurationMatch(usable, query, LrclibTrackDto::durationMs) ?: return null
    return OnlineLyrics(rawContent = match.syncedLyrics!!)
}
