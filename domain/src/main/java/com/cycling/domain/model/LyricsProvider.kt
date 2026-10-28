package com.cycling.domain.model

/**
 * Where to look for lyrics that are not already on disk.
 *
 * [AMLL] is the curated TTML DB behind the Apple-Music-like lyric players: every document is
 * hand-aligned word by word by contributors, which makes it the best answer when it has one, but
 * it only covers a few thousand tracks — a miss outside that catalogue is the common case, not a
 * failure.
 *
 * [NETEASE], [QQ_MUSIC] and [KUGOU] are the Chinese catalogues, best coverage first: NetEase
 * publishes word-by-word `yrc` plus a timed translation, while QQ and Kugou only serve line-level
 * LRC (QQ adds a translation document) from unofficial web endpoints that can rate-limit or change
 * without notice. [LRCLIB] is a documented free service with a strong Western catalogue.
 *
 * The five are tried in the user's configured order, so one catalogue failing or not having the
 * track degrades to the next rather than to no lyrics at all.
 *
 * NetEase and AMLL both publish word-by-word documents — `yrc` and TTML, which `:domain`'s
 * `YrcParser` and `TTMLParser` understand. AMLL also publishes one recording several times over,
 * as different contributors' submissions, which is part of what the lyrics screen's version picker
 * is for.
 *
 * [LX] is the user's own LX source scripts (`LxLyricsProvider`). It is not a catalogue: it answers
 * only for online tracks, because the script protocol takes a platform song id rather than a
 * search query. It sits last by default — the user added it, but the built-in sources are far more
 * predictable.
 */
enum class LyricsProvider {
    AMLL,
    NETEASE,
    QQ_MUSIC,
    KUGOU,
    LRCLIB,
    LX
}
