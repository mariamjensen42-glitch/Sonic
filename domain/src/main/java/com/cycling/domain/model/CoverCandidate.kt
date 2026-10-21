package com.cycling.domain.model

/** Where an online catalogue search looked. */
enum class CoverSource {
    NETEASE,
    QQ_MUSIC,
    KUGOU,
    ITUNES
}

/**
 * One catalogue match for a track: a cover image plus whatever metadata the source could supply.
 *
 * The same match feeds two features, which is why the fields go beyond artwork: the cover picker
 * only needs [imageUrl], while the "fill in from online" action needs the textual fields. They
 * come from the same search response, so asking for them separately would cost the same request
 * twice.
 *
 * The URLs point at the provider's CDN; the image itself is not downloaded until the user picks a
 * candidate, and the app then keeps its own copy, so the chosen cover does not depend on the
 * provider staying up or on the URL not expiring.
 */
data class CoverCandidate(
    /** Identifies this candidate within one search; what the UI sends back to pick it. */
    val id: String,
    val source: CoverSource,
    /** Full-size image URL. */
    val imageUrl: String,
    val album: String? = null,
    val artist: String? = null,
    val title: String? = null,
    /** Album artist (`TPE2`); sources that do not expose it leave this null. */
    val albumArtist: String? = null,
    /** Release year as text, e.g. `1999`; derived from the source's publish timestamp. */
    val year: String? = null,
    val genre: String? = null,
    /** `3/12` where the source reports a track total, plain `3` otherwise. */
    val trackNumber: String? = null,
    /** `1/2`, or plain `1` — `null` when the source does not report it. */
    val discNumber: String? = null,
    /** Track length, used to rank matches by how close they are to the local file. */
    val durationMs: Long? = null,
    /**
     * The first entry of the search result, i.e. the provider's own best guess for the track.
     *
     * It is still a guess — the picker exists precisely because it can be wrong — but it saves a
     * tap in the common case of a correctly tagged file.
     */
    val isBestMatch: Boolean = false
)
