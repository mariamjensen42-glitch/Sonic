package com.cycling.domain.model

/**
 * One version of a track's lyrics, offered so the user can choose between the available ones.
 *
 * Online sources publish the same recording many times over, and the versions do not all carry the
 * same documents — one upload has word-by-word timings, another only line-level ones, a third is a
 * different mastering with a different offset. The version the provider order picks automatically
 * is therefore a default rather than an answer, and this is what the lyrics screen's picker lists.
 *
 * Local sources are deliberately absent: a sidecar `.lrc` or an embedded tag is the user's own and
 * always wins, so offering to replace it with a download would be a lie.
 */
data class LyricsCandidate(
    /** Identifies this version within one lookup; what the UI sends back to select it. */
    val id: String,
    val provider: LyricsProvider,
    /** Whether the player can follow this version word by word rather than line by line. */
    val wordByWord: Boolean,
    /** What this version is — an album name, usually; `null` when the source cannot tell. */
    val label: String? = null,
    /** The first line of the lyrics, to fall back on when there is no [label]. */
    val preview: String? = null,
    /** Whether this is the version already in use. */
    val isCurrent: Boolean = false,
    /** Exactly what the source sent, so selecting this version can be cached verbatim. */
    val rawContent: String
)
