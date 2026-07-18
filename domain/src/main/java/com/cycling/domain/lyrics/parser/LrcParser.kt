package com.cycling.domain.lyrics.parser

import com.cycling.domain.lyrics.model.SyncedLyrics
import com.cycling.domain.lyrics.model.synced.UncheckedSyncedLine
import com.cycling.domain.lyrics.utils.LrcMetadataHelper
import com.cycling.domain.lyrics.utils.parseAsTime

object LrcParser : ILyricsParser {
    private val parser = Regex("\\[(\\d{1,2}:\\d{1,2}\\.\\d{2,3})](.*)")

    override fun parse(lines: List<String>): SyncedLyrics {
        val lyricsLines = LrcMetadataHelper.removeAttributes(lines)
        val data = lyricsLines
            .flatMap { line -> parseLine(line) }
            .combineRawWithTranslation()
            .sortedBy { it.start }
            .rearrangeTime()
            .map { it.toSyncedLine() }
            .filter { it.content.isNotBlank() }
        return SyncedLyrics(lines = data)
    }

    private fun parseLine(content: String): List<UncheckedSyncedLine> {
        return parser.findAll(content).map { matchResult ->
            val (time, lyric) = matchResult.destructured
            UncheckedSyncedLine(
                start = time.parseAsTime(),
                end = 0,
                content = lyric.trim(),
                translation = null
            )
        }.toList()
    }

    private fun List<UncheckedSyncedLine>.combineRawWithTranslation(): List<UncheckedSyncedLine> {
        val list = mutableListOf<UncheckedSyncedLine>()
        var i = 0
        while (i < this.size) {
            val line = this[i]
            val nextLine = this.getOrNull(i + 1)
            if (nextLine != null && line.start == nextLine.start) {
                list.add(line.copy(translation = nextLine.content))
                i += 2
            } else {
                list.add(line)
                i++
            }
        }
        return list
    }

    private fun List<UncheckedSyncedLine>.rearrangeTime(): List<UncheckedSyncedLine> =
        this.mapIndexed { index, line ->
            val end = this.getOrNull(index + 1)?.start ?: Int.MAX_VALUE
            line.copy(end = end)
        }

}
