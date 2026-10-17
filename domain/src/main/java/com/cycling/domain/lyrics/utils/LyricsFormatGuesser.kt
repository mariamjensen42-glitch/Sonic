package com.cycling.domain.lyrics.utils

class LyricsFormatGuesser {
    data class LyricsFormat(
        val name: String,
        val detector: (String) -> Boolean
    )

    private val registeredFormats = mutableListOf<LyricsFormat>()

    init {
        registerFormat(
            LyricsFormat(
                "TTML"
            ) {
                it.contains("<tt.*xmlns.*=.*http://www.w3.org/ns/ttml.*>".toRegex(RegexOption.MULTILINE))
            })

        registerFormat(
            LyricsFormat(
                "LRC"
            ) { it.contains("\\[\\d{2}:\\d{2}\\.\\d{2,3}].+".toRegex()) })

        registerFormat(
            LyricsFormat(
                "WORD_BY_WORD_LRC"
            ) {
                it.contains("\\[\\d{2}:\\d{2}\\.\\d{2,3}].*\\[\\d{2}:\\d{2}\\.\\d{2,3}]".toRegex())
            })

        registerFormat(
            LyricsFormat(
                "ENHANCED_LRC"
            ) {
                it.contains("<\\d{1,2}:\\d{1,2}\\.\\d{1,3}>".toRegex())
            })

        registerFormat(
            LyricsFormat(
                "LYRICIFY_SYLLABLE"
            ) { it.contains("[a-zA-Z]+\\s*\\(\\d+,\\d+\\)".toRegex()) })

        // 必须带逐字标签才算 YRC：Lyricify Lines 的行首 [start,end] 长得一样，却没有逐字标签。
        registerFormat(
            LyricsFormat(
                "YRC"
            ) { it.contains("\\[\\d+,\\d+]\\s*\\(\\d+,\\d+,\\d+\\)".toRegex()) })
    }

    fun registerFormat(format: LyricsFormat) {
        registeredFormats.add(0, format)
    }

    fun guessFormat(lines: List<String>): LyricsFormat? {
        return guessFormat(lines.joinToString("\n"))
    }

    fun guessFormat(content: String): LyricsFormat? {
        return registeredFormats.firstOrNull { it.detector(content) }
    }
}
