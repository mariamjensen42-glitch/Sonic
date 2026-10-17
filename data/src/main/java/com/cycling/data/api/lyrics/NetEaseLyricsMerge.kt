package com.cycling.data.api.lyrics

import com.cycling.data.api.lyrics.dto.NetEaseLyricResponse

/**
 * Folds NetEase's separate lyric documents into the single blob the rest of the pipeline speaks.
 *
 * The endpoint answers with up to four independent payloads — line-level `lrc`, word-by-word `yrc`
 * and the translation matching each of them — while every parser downstream reads one string and
 * takes the line following another as its translation when the two share a start time. Merging is
 * therefore just interleaving: each translation is emitted directly beneath the line it translates,
 * carrying that line's own tag.
 *
 * Word-by-word wins whenever the track has it. `ytlrc` is translated against `yrc`, so pairing the
 * word-by-word lines with the line-level `tlyric` would leave the two a fraction of a second out of
 * step — the timestamps of the two translations differ.
 */
internal fun mergeNetEaseLyrics(response: NetEaseLyricResponse): String? =
    if (response.hasWordByWord()) {
        mergeLines(response.yrc?.lyric.orEmpty(), response.ytlrc?.lyric.orEmpty(), ::yrcLine)
    } else {
        mergeLines(response.lrc?.lyric.orEmpty(), response.tlyric?.lyric.orEmpty(), ::lrcLine)
    }

/**
 * Whether the endpoint sent a word-by-word document — the same test [mergeNetEaseLyrics] branches
 * on, and the reason it is exposed rather than duplicated.
 *
 * A line-level answer is ambiguous: it is what a track with no word-by-word lyrics looks like, and
 * it is also what NetEase returns for an entry of the track that is not the one carrying them.
 * [com.cycling.data.api.lyrics.NetEaseLyricsProvider] resolves the ambiguity by asking about the
 * track's other entries, which it can only do if it can tell the two answers apart.
 */
internal fun NetEaseLyricResponse.hasWordByWord(): Boolean = !yrc?.lyric.isNullOrBlank()

/** `[20250,4530]` — NetEase's word-by-word line tag, start and duration in milliseconds. */
private val YRC_TAG = Regex("^\\[(\\d+),(\\d+)]")

/** `[mm:ss.mmm]` — the same pair split into minutes, seconds and thousandths. */
private val LRC_TAG = Regex("^\\[(\\d{1,2}):(\\d{1,2})\\.(\\d{1,3})]")

/** A source line reduced to what merging needs of it: when it starts, and its tag, verbatim. */
private data class TimedLine(val startMs: Int, val tag: String, val content: String)

private fun yrcLine(line: String): TimedLine? {
    val match = YRC_TAG.find(line) ?: return null
    val startMs = match.groupValues[1].toIntOrNull() ?: return null
    return TimedLine(startMs, match.value, line.substring(match.value.length).trim())
}

private fun lrcLine(line: String): TimedLine? {
    val match = LRC_TAG.find(line) ?: return null
    val minutes = match.groupValues[1].toIntOrNull() ?: return null
    val seconds = match.groupValues[2].toIntOrNull() ?: return null
    // Two decimal places are as common as three; `.73` is 730 ms, not 73.
    val millis = match.groupValues[3].padEnd(3, '0').toIntOrNull() ?: return null
    return TimedLine(
        startMs = minutes * 60_000 + seconds * 1_000 + millis,
        tag = match.value,
        content = line.substring(match.value.length).trim()
    )
}

/**
 * [translation] keyed by start time, dropping the blank entries the endpoint pads its timeline with
 * — a track with no translation still comes back as one timestamp per lyric line, all of them empty.
 *
 * Both translation documents — the line-level `tlyric` and the word-by-word `ytlrc` — are
 * `[mm:ss.mmm]` lines. Only the lyrics they translate can arrive in another syntax.
 */
private fun translationsOf(translation: String): Map<Int, String> =
    translation.lines()
        .mapNotNull(::lrcLine)
        .filter { it.content.isNotBlank() }
        .associate { it.startMs to it.content }

/** @param parseOriginal reads the tag of the document [original] came in. */
private fun mergeLines(original: String, translation: String, parseOriginal: (String) -> TimedLine?): String? {
    if (original.isBlank()) return null

    val translations = translationsOf(translation)
    return original.lines()
        .flatMap { line ->
            val timed = parseOriginal(line)
            val text = translations[timed?.startMs]
            // A line the translation does not mention is left exactly as the endpoint wrote it, so
            // the parsers keep seeing whatever format it came in.
            if (timed == null || text == null) listOf(line) else listOf(line, timed.tag + text)
        }
        .joinToString("\n")
}
