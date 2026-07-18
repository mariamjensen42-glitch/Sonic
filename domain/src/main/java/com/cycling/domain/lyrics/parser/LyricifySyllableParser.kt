package com.cycling.domain.lyrics.parser

import com.cycling.domain.lyrics.model.SyncedLyrics
import com.cycling.domain.lyrics.model.karaoke.KaraokeAlignment
import com.cycling.domain.lyrics.model.karaoke.KaraokeLine
import com.cycling.domain.lyrics.model.karaoke.KaraokeSyllable
import com.cycling.domain.lyrics.utils.LrcMetadataHelper
import com.cycling.domain.lyrics.utils.isDigitsOnly

object LyricifySyllableParser: ILyricsParser {
    private val parser = Regex("(.*?)\\((\\d+),(\\d+)\\)")

    override fun parse(lines: List<String>): SyncedLyrics {
        val lyricsLines = LrcMetadataHelper.removeAttributes(lines)
        val data = lyricsLines.mapNotNull { line->
            if (line.isBlank()) null else parseLine(line)
        }
        return SyncedLyrics(lines = data)
    }

    private fun parseLine(line: String): KaraokeLine {
        val real: String
        var isAccompaniment = false
        val alignment: KaraokeAlignment
        val attributes = mutableListOf<Int>()
        if (line.contains("]") and line.contains("[") and (line.indexOf("]")-line.indexOf("[")==2)) {
            real = line.substring(startIndex = (line.indexOf(']')+1))
            val regex = Regex("\\[(\\d+)]")
            val attribute = regex.find(line)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            attributes.add(attribute)
            if(attribute !in 0..5) {
                isAccompaniment = true
            }
            alignment = if ( attribute==2 || attribute==5 || attribute==8) {
                KaraokeAlignment.End
            } else {
                KaraokeAlignment.Start
            }

        }
        else {
            real = line
            isAccompaniment = false
            alignment = KaraokeAlignment.Start
        }

        val data = parser.findAll(real)
        val syllables = data.map { matched->
            val result = matched.groupValues
            if (result.size == 4 && result[2].isDigitsOnly() && result[3].isDigitsOnly()) {
                KaraokeSyllable(
                    content = result[1],
                    start = result[2].toInt(),
                    end = result[2].toInt() + result[3].toInt(),
                )
            } else  {
                KaraokeSyllable(
                content ="Error",
                start = 0,
                end = 0,
            ) }
        }.toList()

        val startTime = if (syllables.isNotEmpty()) syllables[0].start else 0
        val endTime = if (syllables.isNotEmpty()) syllables.last().end else 0

        return KaraokeLine(syllables, null, isAccompaniment, alignment, startTime, endTime)
    }
}
