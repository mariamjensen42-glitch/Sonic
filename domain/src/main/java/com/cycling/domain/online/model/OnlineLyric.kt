package com.cycling.domain.online.model

/**
 * 音源脚本返回的一份歌词（LX 协议 `action = 'lyric'` 的结果）。
 *
 * 协议里四个字段是并列的：`lyric` 普通歌词、`lxlyric` 逐字歌词、`tlyric` 翻译、
 * `rlyric` 音译。宿主这边只分成三段 —— 逐字优先当主文档（格式由歌词管线自己判定），
 * 翻译与音译各留一份原文，由调用方决定要不要合并。
 *
 * 内容格式不做约定：脚本可能给 LRC、YRC、QRC 或 TTML，甚至就是一段 XML，
 * 翻译/音译同理。判定与解析在歌词管线里做（`LyricsProvider`）。
 */
data class OnlineLyric(
    val lyric: String,
    val translation: String? = null,
    val romanization: String? = null
) {
    val isEmpty: Boolean get() = lyric.isBlank() && translation.isNullOrBlank()
}
