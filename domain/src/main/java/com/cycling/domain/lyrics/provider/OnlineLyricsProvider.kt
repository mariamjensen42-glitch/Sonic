package com.cycling.domain.lyrics.provider

import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.model.LyricsQuery

/**
 * One remote source of lyrics.
 *
 * Implementations live in `:data` because they need an HTTP client; `domain` only fixes the shape
 * so the repository can iterate the user's configured provider order without knowing any service's
 * request format. A provider is expected to be total: no lyrics is an empty list, not an exception,
 * and a failure the next provider could survive must be thrown so the caller can degrade down the
 * list.
 */
interface OnlineLyricsProvider {

    /** Which entry of [com.cycling.domain.repository.SettingsRepository.lyricsProviderOrder] this serves. */
    val provider: LyricsProvider

    /**
     * Every version of [query]'s lyrics this source can offer, best first — the first one is what
     * the player uses unless the user picks another.
     *
     * A source that publishes one recording several times over can answer with more than one: the
     * versions do not all carry the same documents, so "the lyrics" is not a single answer. The
     * list is what the lyrics screen's version picker shows, which is why the versions are kept
     * apart instead of collapsed into the best one.
     *
     * @throws Exception on transport or parse failure — the caller falls through to the next provider.
     */
    suspend fun fetchCandidates(query: LyricsQuery): List<OnlineLyrics>

    /** The version the player uses: the best of [fetchCandidates], or `null` when there is none. */
    suspend fun fetch(query: LyricsQuery): OnlineLyrics? = fetchCandidates(query).firstOrNull()
}

/**
 * Raw lyrics as they arrived, kept unparsed so the cache can store exactly what the provider sent.
 *
 * Carries no format hint on purpose: the local guesser already picks the richest parser the content
 * supports, and pinning the format a provider *claims* to use would silently downgrade word-by-word
 * lyrics to line-level ones.
 *
 * @property label what this particular version is, when the source can tell two apart — the album
 *   one of NetEase's many uploads of the same recording came from. Only ever shown to the user, so
 *   that choosing between versions is possible; it never reaches the parser.
 */
data class OnlineLyrics(
    val rawContent: String,
    val label: String? = null
)
