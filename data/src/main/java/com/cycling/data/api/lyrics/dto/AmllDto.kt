package com.cycling.data.api.lyrics.dto

import kotlinx.serialization.Serializable

/**
 * Shapes for the AMLL TTML API (`https://api.amll.dev`, the `/v1/lyrics/search` and
 * `/v1/lyrics/get` endpoints).
 *
 * Search and get answer with the same `SongItem` object — the get endpoint merely fills in
 * `lyrics` — so one DTO covers both calls, exactly as [LrclibTrackDto] does for LRCLIB. Every field
 * is defaulted and every container empty-list-backed because the payload is external: a key that
 * disappears in an API version bump must read as "that field was not sent" instead of aborting the
 * lookup. The metadata keys the provider does not use (`createdAt`, `ncmMusicIds`, `matchContext`,
 * …) are dropped by `ignoreUnknownKeys`.
 */
@Serializable
internal data class AmllSearchResponseDto(
    val status: Int = 0,
    val data: AmllSearchDataDto? = null
)

@Serializable
internal data class AmllSearchDataDto(
    val items: List<AmllSongItemDto> = emptyList()
)

@Serializable
internal data class AmllGetResponseDto(
    val status: Int = 0,
    val data: AmllSongItemDto? = null
)

/** One lyric document in the library: what it claims to be, and (via get only) its TTML body. */
@Serializable
internal data class AmllSongItemDto(
    /** 53-bit id derived from the file name; the permanent key for [lyrics]. */
    val id: Long = 0,
    val filename: String = "",
    val musicNames: List<String> = emptyList(),
    val artistNames: List<String> = emptyList(),
    val albumNames: List<String> = emptyList(),
    /**
     * 各平台里**同一条录音**的 id。词库里每份文档都登记了提交者填写的平台 id，网易云与 QQ
     * 的 id 能直接和本地「在线曲目」的 `onlineSongId` 对上 —— 那是比曲名、专辑都硬的证据：
     * 平台 id 相同就是同一份母带，不是"同名歌"。
     */
    val ncmMusicIds: List<String> = emptyList(),
    val qqMusicIds: List<String> = emptyList(),
    /** GitHub usernames of the TTML's contributors — what tells two submissions apart. */
    val authorUsernames: List<String> = emptyList(),
    /** The TTML document itself. Only the get endpoint sends it; search leaves it null. */
    val lyrics: String? = null
)
