package com.cycling.domain.model

import com.cycling.domain.lyrics.model.SyncedLyrics

data class Lyrics(
    val syncedLyrics: SyncedLyrics?,
    val rawContent: String?,
    val source: LyricsSource,
    /**
     * 这份歌词由哪个在线来源提供；来自本机（手写、内嵌标签、旁车文件）时为 `null`。
     *
     * 只用于展示 —— 让歌曲详情页能说清「当前这份是谁给的」，而不是让用户对着「在线下载」
     * 四个字猜是网易云还是 LRCLIB。
     */
    val provider: LyricsProvider? = null
)

enum class LyricsSource {
    /**
     * Written by the user for this file, and therefore authoritative.
     *
     * Ranks above every other source — a sidecar file and an embedded tag included — because an
     * edit is an explicit statement about *this* recording, while everything below it is a guess
     * made on the user's behalf.
     */
    USER,
    EMBEDDED,
    FILE,
    /** Downloaded from an [LyricsProvider] and persisted in `lyrics_cache`. */
    ONLINE,
    NONE
}
