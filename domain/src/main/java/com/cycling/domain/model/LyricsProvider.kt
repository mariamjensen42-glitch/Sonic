package com.cycling.domain.model

/**
 * Where to look for lyrics that are not already on disk.
 *
 * [NETEASE] has much better Chinese coverage but is an unofficial endpoint, so it can rate-limit
 * or change without notice; [LRCLIB] is a documented free service with a strong Western catalogue.
 * The two are tried in the user's configured order, so a NetEase failure degrades to LRCLIB rather
 * than to no lyrics at all.
 *
 * NetEase also offers word-by-word `yrc`, which `:domain`'s `YrcParser` understands — and because
 * it publishes one recording several times over with different documents attached, it is the
 * source whose versions the lyrics screen's picker is mostly there for.
 */
enum class LyricsProvider {
    NETEASE,
    LRCLIB
}
