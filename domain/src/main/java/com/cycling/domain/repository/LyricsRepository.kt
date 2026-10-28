package com.cycling.domain.repository

import com.cycling.domain.model.Lyrics
import com.cycling.domain.model.LyricsCandidate
import com.cycling.domain.model.LyricsCandidatesBatch
import com.cycling.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface LyricsRepository {

    /**
     * Lyrics for [song], in descending order of authority: a sidecar file, then embedded tags,
     * then a previously downloaded copy, then — when the user allows it — an online provider.
     *
     * Takes the whole [Song] rather than its path because the online stage has to identify the
     * track by metadata; a path only identifies the file.
     */
    suspend fun getLyrics(song: Song): Lyrics

    /**
     * Resolves [song] again as though nothing had ever been downloaded for it.
     *
     * This is what a manual "fetch the lyrics" tap means: whatever was downloaded before — a copy
     * that turned out to be the wrong recording, or a remembered "the providers had nothing" — is
     * forgotten first, so the answer is not simply the cached one read back. A sidecar file or an
     * embedded tag still wins, because those are the user's own and cannot be stale.
     */
    suspend fun refreshLyrics(song: Song): Lyrics

    /**
     * Every version of [song]'s lyrics the online sources can offer, one batch per source as each
     * one answers.
     *
     * Empty — no batches at all — when the user has online lyrics switched off. Versions a single
     * source repeats are collapsed into one, since choosing between them would change nothing; the
     * same document offered by two catalogues is kept under each of them, because the picker groups
     * by platform and hiding one would make "use that platform's copy" impossible. Local sources are
     * not included — see [LyricsCandidate].
     *
     * The flow completes when every source has answered, so a collector that only wants the final
     * answer can `toList()`. It costs a network round trip per source, so this is only for the
     * picker, never for playback.
     */
    fun findLyricsCandidates(song: Song): Flow<LyricsCandidatesBatch>

    /**
     * Uses [candidate] for [song] from now on, replacing whatever was cached for it.
     *
     * The choice outranks a sidecar file and an embedded tag: it is the user saying "not that one",
     * and a downloaded copy that the lookup then ignores would make the picker a lie. Only lyrics
     * the user wrote themselves (see [saveLyrics]) still win, because those are not a guess either.
     * Returns [candidate]'s lyrics, or a source-less result when they turn out not to be parseable.
     */
    suspend fun selectLyricsCandidate(song: Song, candidate: LyricsCandidate): Lyrics

    /**
     * Remembers [rawContent] as the lyrics the user wrote for [song].
     *
     * Stored against the song rather than against a `title|artist|duration` fingerprint, because
     * the edit has to survive the very thing the user is most likely to do next — correcting the
     * title or artist — and a fingerprint would move out from under it. What comes back is the
     * result of parsing [rawContent]: timed lyrics for the player, and always the raw text for the
     * detail page, which renders whatever was written even when it carries no timings.
     */
    suspend fun saveLyrics(song: Song, rawContent: String): Lyrics

    /**
     * Forgets the user's own lyrics for [song], handing the track back to the automatic sources.
     *
     * Deleting rather than blanking the row is what makes this reversible: the next lookup is a
     * real one, so a sidecar file, an embedded tag or a provider can answer again.
     */
    suspend fun clearLyrics(song: Song)
}
