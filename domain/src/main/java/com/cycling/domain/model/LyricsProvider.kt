package com.cycling.domain.model

/**
 * Where to look for lyrics that are not already on disk.
 *
 * [NETEASE] has much better Chinese coverage and word-by-word (`yrc`) timing but is an
 * unofficial endpoint, so it can rate-limit or change without notice; [LRCLIB] is a documented
 * free service. The two are tried in the user's configured order, so a NetEase failure degrades
 * to LRCLIB rather than to no lyrics at all.
 */
enum class LyricsProvider {
    NETEASE,
    LRCLIB
}
