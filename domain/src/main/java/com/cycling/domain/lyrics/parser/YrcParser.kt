package com.cycling.domain.lyrics.parser

import com.cycling.domain.lyrics.model.SyncedLyrics
import com.cycling.domain.lyrics.model.karaoke.KaraokeAlignment
import com.cycling.domain.lyrics.model.karaoke.KaraokeLine
import com.cycling.domain.lyrics.model.karaoke.KaraokeSyllable

/**
 * NetEase's word-by-word format.
 *
 * A line reads `[start,duration](start,duration,0)word(start,duration,0)word…`, every timestamp in
 * absolute milliseconds and each `(…)` tag describing the word that *follows* it. Unlike
 * [WordByWordLrcParser] the durations are explicit, so syllables need no help from the next line to
 * find their end.
 *
 * A line carrying no `(…)` tag at all is a translation of the line it follows, sharing that line's
 * tag: the provider emits NetEase's `ytlrc` directly beneath the line it translates. Pairing on the
 * tag alone would be wrong here — yrc lines legitimately overlap — so the follower additionally has
 * to be syllable-free.
 */
object YrcParser : ILyricsParser {
    private val lineRegex = Regex("^\\[(\\d+),(\\d+)](.*)$")
    private val syllableTagRegex = Regex("\\((\\d+),(\\d+)(?:,\\d+)?\\)")

    override fun parse(lines: List<String>): SyncedLyrics {
        val data = lines
            .mapNotNull { parseLine(it.trimEnd()) }
            .combineRawWithTranslation()
            .sortedBy { it.start }
        return SyncedLyrics(lines = data)
    }

    /** One source line before translation pairing decides whether it is a lyric or a translation. */
    private data class ParsedLine(
        val start: Int,
        val end: Int,
        val syllables: List<KaraokeSyllable>,
        val text: String,
    )

    private fun parseLine(line: String): ParsedLine? {
        val match = lineRegex.find(line) ?: return null
        val start = match.groupValues[1].toIntOrNull() ?: return null
        val duration = match.groupValues[2].toIntOrNull() ?: 0
        val body = match.groupValues[3]

        val tags = syllableTagRegex.findAll(body).toList()
        val syllables = tags.mapIndexedNotNull { index, tag ->
            val syllableStart = tag.groupValues[1].toIntOrNull() ?: return@mapIndexedNotNull null
            val syllableDuration = tag.groupValues[2].toIntOrNull() ?: 0
            // Everything up to the next tag belongs to this one, spaces included — they are what
            // separates the words of a latin-script line.
            val contentEnd = tags.getOrNull(index + 1)?.range?.first ?: body.length
            val content = body.substring(tag.range.last + 1, contentEnd)
            if (content.isBlank()) null
            else KaraokeSyllable(content, syllableStart, syllableStart + syllableDuration)
        }

        return ParsedLine(
            start = start,
            // The line tag is authoritative, but a line written shorter than the syllables it
            // contains would clip the last word, so the further of the two wins.
            end = maxOf(start + duration, syllables.lastOrNull()?.end ?: start),
            syllables = syllables,
            // Only a syllable-free line is a translation; on a lyric line the raw body is tag soup.
            text = if (tags.isEmpty()) body.trim() else "",
        )
    }

    private fun List<ParsedLine>.combineRawWithTranslation(): List<KaraokeLine> {
        val list = mutableListOf<KaraokeLine>()
        var i = 0
        while (i < this.size) {
            val line = this[i]
            val nextLine = this.getOrNull(i + 1)
            if (nextLine != null && nextLine.syllables.isEmpty() && line.start == nextLine.start) {
                list.add(line.toKaraokeLine(translation = nextLine.text))
                i += 2
            } else {
                list.add(line.toKaraokeLine(translation = null))
                i++
            }
        }
        return list
    }

    private fun ParsedLine.toKaraokeLine(translation: String?): KaraokeLine = KaraokeLine(
        syllables = syllables.ifEmpty { listOf(KaraokeSyllable(text, start, end)) },
        translation = translation?.takeIf { it.isNotBlank() },
        isAccompaniment = false,
        alignment = KaraokeAlignment.Unspecified,
        start = start,
        end = end
    )
}
