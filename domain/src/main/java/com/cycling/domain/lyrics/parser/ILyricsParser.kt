package com.cycling.domain.lyrics.parser

import com.cycling.domain.lyrics.model.SyncedLyrics

interface ILyricsParser {
    fun parse(lines: List<String>): SyncedLyrics {
        return parse(lines.joinToString("\n"))
    }

    fun parse(content: String): SyncedLyrics {
        return parse(content.split('\n'))
    }
}
