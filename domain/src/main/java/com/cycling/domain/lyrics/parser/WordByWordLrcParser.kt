package com.cycling.domain.lyrics.parser

import com.cycling.domain.lyrics.model.SyncedLyrics
import com.cycling.domain.lyrics.model.karaoke.KaraokeAlignment
import com.cycling.domain.lyrics.model.karaoke.KaraokeLine
import com.cycling.domain.lyrics.model.karaoke.KaraokeSyllable
import com.cycling.domain.lyrics.utils.LrcMetadataHelper
import com.cycling.domain.lyrics.utils.parseAsTime

object WordByWordLrcParser : ILyricsParser {
    private val timeTagRegex = Regex("\\[(\\d{2}:\\d{2}\\.\\d{2,3})]")
    private val lineStartRegex = Regex("^\\[(\\d{2}:\\d{2}\\.\\d{2,3})](.*)$")

    override fun parse(lines: List<String>): SyncedLyrics {
        val lyricsLines = LrcMetadataHelper.removeAttributes(lines)
        val data = lyricsLines
            .mapNotNull { line -> parseLine(line) }
            .combineRawWithTranslation()
            .sortedBy { it.start }
        return SyncedLyrics(lines = data)
    }

    private fun List<KaraokeLine>.combineRawWithTranslation(): List<KaraokeLine> {
        val list = mutableListOf<KaraokeLine>()
        var i = 0
        while (i < this.size) {
            val line = this[i]
            val nextLine = this.getOrNull(i + 1)
            
            if (nextLine != null && line.start == nextLine.start) {
                val lineText = line.syllables.joinToString("") { it.content }.trim()
                val nextLineText = nextLine.syllables.joinToString("") { it.content }.trim()
                
                val (originalLine, translationText) = if (isOriginal(lineText, nextLineText)) {
                    line to nextLineText
                } else {
                    nextLine to lineText
                }
                
                list.add(originalLine.copy(translation = translationText.ifEmpty { null }))
                i += 2
            } else {
                list.add(line)
                i++
            }
        }
        return list
    }

    private fun isOriginal(first: String, second: String): Boolean {
        if (first.isEmpty()) return false
        if (second.isEmpty()) return true

        val firstIsCJK = first.any { it.isCJK() }
        val secondIsCJK = second.any { it.isCJK() }

        if (firstIsCJK && !secondIsCJK) return false
        if (!firstIsCJK && secondIsCJK) return true

        val firstIsLatin = first.any { it.isLetter() && it.code < 128 }
        val secondIsNonLatin = second.any { it.isLetter() && it.code > 127 }

        if (firstIsLatin && secondIsNonLatin) return true

        return true
    }

    private fun Char.isCJK(): Boolean {
        val code = this.code
        return code in 0x4E00..0x9FFF ||
                code in 0x3400..0x4DBF ||
                code in 0x3000..0x303F ||
                code in 0xFF00..0xFFEF ||
                code in 0x31C0..0x31EF ||
                code in 0x2E80..0x2EFF
    }

    private fun parseLine(line: String): KaraokeLine? {
        val lineStartMatch = lineStartRegex.find(line) ?: return null
        val lineStartTime = lineStartMatch.groupValues[1].parseAsTime()
        val content = lineStartMatch.groupValues[2]

        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) {
            return null
        }

        val syllables = parseSyllables(trimmedContent)

        if (syllables.isEmpty()) {
            return KaraokeLine(
                syllables = listOf(KaraokeSyllable(trimmedContent, lineStartTime, lineStartTime)),
                translation = null,
                isAccompaniment = false,
                alignment = KaraokeAlignment.Unspecified,
                start = lineStartTime,
                end = lineStartTime
            )
        }

        return KaraokeLine(
            syllables = syllables,
            translation = null,
            isAccompaniment = false,
            alignment = KaraokeAlignment.Unspecified,
            start = syllables.first().start,
            end = syllables.last().end
        )
    }

    private fun parseSyllables(content: String): List<KaraokeSyllable> {
        val syllables = mutableListOf<KaraokeSyllable>()
        val timeTags = timeTagRegex.findAll(content).toList()

        if (timeTags.isEmpty()) return emptyList()

        for (i in timeTags.indices) {
            val currentMatch = timeTags[i]
            val currentTime = currentMatch.groupValues[1].parseAsTime()

            val textStart = currentMatch.range.last + 1
            val textEnd = if (i + 1 < timeTags.size) {
                timeTags[i + 1].range.first
            } else {
                content.length
            }

            val text = if (textStart <= textEnd) {
                content.substring(textStart, textEnd)
            } else {
                ""
            }

            val nextTime = if (i + 1 < timeTags.size) {
                timeTags[i + 1].groupValues[1].parseAsTime()
            } else {
                currentTime + 500
            }

            if (text.isNotBlank()) {
                syllables.add(
                    KaraokeSyllable(
                        content = text,
                        start = currentTime,
                        end = nextTime
                    )
                )
            } else if (syllables.isNotEmpty() && i == timeTags.lastIndex) {
                syllables[syllables.lastIndex] = syllables.last().copy(end = currentTime)
            }
        }

        return syllables
    }
}
