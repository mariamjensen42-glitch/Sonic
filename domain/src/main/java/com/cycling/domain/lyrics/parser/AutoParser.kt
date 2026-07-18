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

    fun registerParser(formatName: String, parser: ILyricsParser) {
        parsers[formatName] = parser
    }

    fun register(format: LyricsFormatGuesser.LyricsFormat, parser: ILyricsParser) {
        guesser.registerFormat(format)
        registerParser(format.name, parser)
    }

    override fun parse(lines: List<String>): SyncedLyrics {
        return parse(lines.joinToString("\n"))
    }

    override fun parse(content: String): SyncedLyrics {
        val format = guesser.guessFormat(content)
        val parser = format?.name?.let { parsers[it] }

        return parser?.parse(content.split("\n"))
            ?: SyncedLyrics(lines = emptyList())
    }

    class Builder {
        private val guesser = LyricsFormatGuesser()
        private val customParsers = mutableMapOf<String, ILyricsParser>()

        fun withFormat(format: LyricsFormatGuesser.LyricsFormat, parser: ILyricsParser): Builder {
            guesser.registerFormat(format)
            customParsers[format.name] = parser
            return this
        }

        fun build(): AutoParser {
            val autoParser = AutoParser(guesser)
            customParsers.forEach { (name, parser) ->
                autoParser.registerParser(name, parser)
            }
            return autoParser
        }
    }
}
