package com.cycling.data.api.lyrics

import com.cycling.data.api.lyrics.dto.AmllSongItemDto
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

/**
 * Orders AMLL's search results so the submission belonging to the local file's release comes first.
 *
 * The artist has already been verified (see [selectAmllCandidates]); what is left is *which release*
 * a document was timed against — a single, a compilation, a live take. Album is the one field that
 * can still separate them, and an exact match is strong evidence that the local file and the
 * document describe the same audio.
 *
 * This ranks rather than filters: a single whose album tag differs from the album it also appears
 * on is still the same recording, and rejecting it would lose the lyrics entirely. The sort is
 * stable, so whatever the album cannot separate keeps the order the server ranked it in — which is
 * also why this provider needs no equivalent of [candidatesByDurationDistance]: AMLL's length field
 * is the last lyric timestamp, not the track's length, so comparing it to the local duration would
 * reject well-timed documents for any track with an instrumental outro.
 */
internal fun rankAmllCandidates(
    candidates: List<AmllSongItemDto>,
    query: LyricsQuery
): List<AmllSongItemDto> = candidates.sortedByDescending { it.albumRank(query.album) }

/** 2 = same album, 1 = one title contains the other, 0 = no usable signal. */
private fun AmllSongItemDto.albumRank(album: String): Int {
    if (album.isBlank()) return 0
    val names = albumNames.filter { it.isNotBlank() }
    return when {
        names.any { it.equals(album, ignoreCase = true) } -> 2
        names.any { it.contains(album, ignoreCase = true) || album.contains(it, ignoreCase = true) } -> 1
        else -> 0
    }
}

/**
 * The AMLL submissions worth keeping for [query]: same artist, release-album first.
 *
 * The provider searches by **title only** and filters here, because the API's own `artistName`
 * parameter proved unreliable: a search for `musicName=Lemon&artistName=米津玄師` answers empty
 * while `artistName=米津` finds the document whose `artistNames` is exactly that name — so the
 * server-side AND would silently drop lyrics it has. Title-only search is safe here because the
 * artist is verified before anything is downloaded.
 *
 * Filtering is not optional: a common title ("Hello", "晴天") is shared by many recordings, and
 * serving another artist's lyrics is worse than serving none.
 */
internal fun selectAmllCandidates(
    candidates: List<AmllSongItemDto>,
    query: LyricsQuery
): List<AmllSongItemDto> = candidates
    .filter { it.id > 0 && it.matchesArtist(query.artist) }
    .let { rankAmllCandidates(it, query) }

/**
 * Whether any of [AmllSongItemDto.artistNames] names the same artist as [artist].
 *
 * Both sides are split on the separators tags use for collaborations (`feat.`, `&`, `、` …) and
 * stripped to letters/digits, so "Taylor Swift feat. Brendon Urie" matches an `artistNames`
 * entry of either name. Partial containment only counts from three characters up, so a short
 * name cannot swallow a longer unrelated one.
 */
private fun AmllSongItemDto.matchesArtist(artist: String): Boolean {
    val wanted = artistTokens(artist)
    if (wanted.isEmpty()) return false
    val offered = artistNames.flatMap { artistTokens(it) }
    return wanted.any { want ->
        offered.any { have ->
            want == have ||
                (want.length >= MIN_PARTIAL_ARTIST_LENGTH &&
                    have.length >= MIN_PARTIAL_ARTIST_LENGTH &&
                    (want.contains(have) || have.contains(want)))
        }
    }
}

/**
 * 多歌手标签的分隔符，以及 feat / ft / with 这类连接词。
 *
 * 连接词后面要单独允许一个点（`feat.`），不能在词尾 `\b` 与点之间做边界判断 ——
 * "." 不是单词字符，词尾边界根本不会出现在它后面。
 */
private val ARTIST_SEPARATORS = Regex(
    "[、,，&/;；+·]|\\b(?:feat|ft|with)\\b\\.?",
    RegexOption.IGNORE_CASE
)

/** 只保留字母与数字：大小写、空格、括号和标点都不该让同一个歌手匹配失败。 */
private val NON_NAME_CHARS = Regex("[^\\p{L}\\p{N}]")

/** 歌手名过短时不做包含判断，避免 "Jo" 命中 "Jolin"。 */
private const val MIN_PARTIAL_ARTIST_LENGTH = 3

private fun artistTokens(raw: String): List<String> = raw
    .split(ARTIST_SEPARATORS)
    .map { it.lowercase().replace(NON_NAME_CHARS, "") }
    .filter { it.isNotEmpty() }
