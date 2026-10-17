package com.cycling.domain.lyrics.parser

import com.cycling.domain.lyrics.model.SyncedLyrics
import com.cycling.domain.lyrics.utils.LyricsFormatGuesser

class AutoParser(private val guesser: LyricsFormatGuesser) : ILyricsParser {
    private val parsers = mutableMapOf<String, ILyricsParser>()

    init {
        registerParser("LRC", LrcParser)
        registerParser("ENHANCED_LRC", EnhancedLrcParser)
        registerParser("TTML", TTMLParser)
        registerParser("LYRICIFY_SYLLABLE", LyricifySyllableParser)
        registerParser("WORD_BY_WORD_LRC", WordByWordLrcParser)
    }

    private fun registerParser(formatName: String, parser: ILyricsParser) {
        parsers[formatName] = parser
    }

    override fun parse(content: String): SyncedLyrics {
        val format = guesser.guessFormat(content)
        val parser = format?.name?.let { parsers[it] }

        return parser?.parse(content.split("\n"))
            ?: SyncedLyrics(lines = emptyList())
    }
}
