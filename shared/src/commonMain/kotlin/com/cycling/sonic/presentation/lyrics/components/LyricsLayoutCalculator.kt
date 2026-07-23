package com.cycling.sonic.presentation.lyrics.components

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import com.cycling.sonic.domain.lyrics.model.karaoke.KaraokeSyllable
import com.cycling.sonic.presentation.lyrics.utils.isArabic
import com.cycling.sonic.presentation.lyrics.utils.isDevanagari
import com.cycling.sonic.presentation.lyrics.utils.isPunctuation
import com.cycling.sonic.presentation.lyrics.utils.isPureCjk
import kotlin.math.pow

@Stable
data class SyllableLayout(
    val syllable: KaraokeSyllable,
    val textLayoutResult: TextLayoutResult,
    val wordId: Int,
    val useAwesomeAnimation: Boolean,
    val width: Float = textLayoutResult.size.width.toFloat(),
    val position: Offset = Offset.Zero,
    val wordPivot: Offset = Offset.Zero,
    val wordAnimInfo: WordAnimationInfo? = null,
    val charOffsetInWord: Int = 0,
    val charLayouts: List<TextLayoutResult>? = null,
    val charOriginalBounds: List<Rect>? = null,
    val firstBaseline: Float = 0f,
)

@Stable
data class WordAnimationInfo(
    val wordStartTime: Long,
    val wordEndTime: Long,
    val wordContent: String,
    val wordDuration: Long = wordEndTime - wordStartTime
)

@Stable
data class WrappedLine(
    val syllables: List<SyllableLayout>, val totalWidth: Float
)

fun String.shouldUseSimpleAnimation(): Boolean {
    val cleanedStr = this.filter { !it.isWhitespace() && !it.toString().isPunctuation() }
    if (cleanedStr.isEmpty()) return false
    return cleanedStr.isPureCjk() || cleanedStr.any { it.isArabic() || it.isDevanagari() }
}

fun groupIntoWords(syllables: List<KaraokeSyllable>): List<List<KaraokeSyllable>> {
    if (syllables.isEmpty()) return emptyList()
    val words = mutableListOf<List<KaraokeSyllable>>()
    var currentWord = mutableListOf<KaraokeSyllable>()
    syllables.forEach { syllable ->
        currentWord.add(syllable)
        if (syllable.content.trimEnd().length < syllable.content.length) {
            words.add(currentWord.toList())
            currentWord = mutableListOf()
        }
    }
    if (currentWord.isNotEmpty()) {
        words.add(currentWord.toList())
    }
    return words
}

fun measureSyllablesAndDetermineAnimation(
    syllables: List<KaraokeSyllable>,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    isAccompanimentLine: Boolean,
    spaceWidth: Float
): List<SyllableLayout> {
    val words = groupIntoWords(syllables)
    val fastCharAnimationThresholdMs = 200f

    return words.flatMapIndexed { wordIndex, word ->
        val wordContent = word.joinToString("") { it.content }
        val wordDuration = if (word.isNotEmpty()) word.last().end - word.first().start else 0
        val perCharDuration = if (wordContent.isNotEmpty() && wordDuration > 0) {
            wordDuration.toFloat() / wordContent.length
        } else 0f

        val useAwesomeAnimation =
            perCharDuration > fastCharAnimationThresholdMs && wordDuration >= 1000
                    && !wordContent.shouldUseSimpleAnimation()
                    && !isAccompanimentLine

        word.map { syllable ->
            val layoutResult = textMeasurer.measure(syllable.content, style)

            var layoutWidth = layoutResult.size.width.toFloat()
            if (syllable.content.endsWith(" ")) {
                val trimmedWidth = textMeasurer.measure(syllable.content.trimEnd(), style).size.width.toFloat()
                if (layoutWidth <= trimmedWidth) {
                    val spaceCount = syllable.content.length - syllable.content.trimEnd().length
                    layoutWidth = trimmedWidth + (spaceWidth * spaceCount)
                }
            }

            val (charLayouts, charBounds) = if (useAwesomeAnimation) {
                val layouts = syllable.content.map { char -> textMeasurer.measure(char.toString(), style) }
                val bounds = syllable.content.indices.map { index -> layoutResult.getBoundingBox(index) }
                layouts to bounds
            } else null to null

            SyllableLayout(
                syllable = syllable,
                textLayoutResult = layoutResult,
                wordId = wordIndex,
                useAwesomeAnimation = useAwesomeAnimation,
                width = layoutWidth,
                charLayouts = charLayouts,
                charOriginalBounds = charBounds,
                firstBaseline = layoutResult.firstBaseline
            )
        }
    }
}

fun calculateGreedyWrappedLines(
    syllableLayouts: List<SyllableLayout>,
    availableWidthPx: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle
): List<WrappedLine> {
    val lines = mutableListOf<WrappedLine>()
    val currentLine = mutableListOf<SyllableLayout>()
    var currentLineWidth = 0f

    val wordGroups = mutableListOf<List<SyllableLayout>>()
    if (syllableLayouts.isNotEmpty()) {
        var currentWordGroup = mutableListOf<SyllableLayout>()
        var currentWordId = syllableLayouts.first().wordId
        syllableLayouts.forEach { layout ->
            if (layout.wordId != currentWordId) {
                wordGroups.add(currentWordGroup)
                currentWordGroup = mutableListOf()
                currentWordId = layout.wordId
            }
            currentWordGroup.add(layout)
        }
        wordGroups.add(currentWordGroup)
    }

    wordGroups.forEach { wordSyllables ->
        val wordWidth = wordSyllables.sumOf { it.width.toDouble() }.toFloat()
        if (currentLineWidth + wordWidth <= availableWidthPx) {
            currentLine.addAll(wordSyllables)
            currentLineWidth += wordWidth
        } else {
            if (currentLine.isNotEmpty()) {
                val trimmed = trimDisplayLineTrailingSpaces(currentLine, textMeasurer, style)
                if (trimmed.syllables.isNotEmpty()) lines.add(trimmed)
                currentLine.clear()
                currentLineWidth = 0f
            }
            if (wordWidth <= availableWidthPx) {
                currentLine.addAll(wordSyllables)
                currentLineWidth += wordWidth
            } else {
                wordSyllables.forEach { syllable ->
                    if (currentLineWidth + syllable.width > availableWidthPx && currentLine.isNotEmpty()) {
                        val trimmed = trimDisplayLineTrailingSpaces(currentLine, textMeasurer, style)
                        if (trimmed.syllables.isNotEmpty()) lines.add(trimmed)
                        currentLine.clear()
                        currentLineWidth = 0f
                    }
                    currentLine.add(syllable)
                    currentLineWidth += syllable.width
                }
            }
        }
    }
    if (currentLine.isNotEmpty()) {
        val trimmed = trimDisplayLineTrailingSpaces(currentLine, textMeasurer, style)
        if (trimmed.syllables.isNotEmpty()) lines.add(trimmed)
    }
    return lines
}

fun calculateBalancedLines(
    syllableLayouts: List<SyllableLayout>,
    availableWidthPx: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle
): List<WrappedLine> {
    if (syllableLayouts.isEmpty()) return emptyList()

    val n = syllableLayouts.size
    val costs = DoubleArray(n + 1) { Double.POSITIVE_INFINITY }
    val breaks = IntArray(n + 1)
    costs[0] = 0.0

    for (i in 1..n) {
        var currentLineWidth = 0f
        for (j in i downTo 1) {
            if (j > 1 && syllableLayouts[j - 2].wordId == syllableLayouts[j - 1].wordId) {
                currentLineWidth += syllableLayouts[j - 1].width
                if (currentLineWidth > availableWidthPx) break
                continue
            }
            currentLineWidth += syllableLayouts[j - 1].width
            if (currentLineWidth > availableWidthPx) break
            val badness = (availableWidthPx - currentLineWidth).pow(2).toDouble()
            if (costs[j - 1] != Double.POSITIVE_INFINITY && costs[j - 1] + badness < costs[i]) {
                costs[i] = costs[j - 1] + badness
                breaks[i] = j - 1
            }
        }
    }

    if (costs[n] == Double.POSITIVE_INFINITY) {
        return calculateGreedyWrappedLines(syllableLayouts, availableWidthPx, textMeasurer, style)
    }

    val lines = mutableListOf<WrappedLine>()
    var currentIndex = n
    while (currentIndex > 0) {
        val startIndex = breaks[currentIndex]
        val lineSyllables = syllableLayouts.subList(startIndex, currentIndex)
        lines.add(0, trimDisplayLineTrailingSpaces(lineSyllables, textMeasurer, style))
        currentIndex = startIndex
    }
    return lines
}

fun calculateStaticLineLayout(
    wrappedLines: List<WrappedLine>,
    isLineRightAligned: Boolean,
    canvasWidth: Float,
    lineHeight: Float,
    isRtl: Boolean
): List<List<SyllableLayout>> {
    val layoutsByWord = mutableMapOf<Int, MutableList<SyllableLayout>>()

    val positionedLines = wrappedLines.mapIndexed { lineIndex, wrappedLine ->
        val maxBaseline = wrappedLine.syllables.maxOfOrNull { it.firstBaseline } ?: 0f
        val rowTopY = lineIndex * lineHeight
        val startX = if (isLineRightAligned) canvasWidth - wrappedLine.totalWidth else 0f
        var currentX = if (isRtl) startX + wrappedLine.totalWidth else startX

        wrappedLine.syllables.map { initialLayout ->
            val positionX = if (isRtl) currentX - initialLayout.width else currentX
            val positionedLayout = initialLayout.copy(
                position = Offset(positionX, rowTopY + maxBaseline - initialLayout.firstBaseline)
            )
            layoutsByWord.getOrPut(positionedLayout.wordId) { mutableListOf() }.add(positionedLayout)
            if (isRtl) currentX -= positionedLayout.width else currentX += positionedLayout.width
            positionedLayout
        }
    }

    val animInfoByWord = mutableMapOf<Int, WordAnimationInfo>()
    val charOffsetsBySyllable = mutableMapOf<SyllableLayout, Int>()
    layoutsByWord.forEach { (wordId, layouts) ->
        if (layouts.first().useAwesomeAnimation) {
            animInfoByWord[wordId] = WordAnimationInfo(
                wordStartTime = layouts.minOf { it.syllable.start }.toLong(),
                wordEndTime = layouts.maxOf { it.syllable.end }.toLong(),
                wordContent = layouts.joinToString("") { it.syllable.content })
            var runningCharOffset = 0
            layouts.forEach { layout ->
                charOffsetsBySyllable[layout] = runningCharOffset
                runningCharOffset += layout.syllable.content.length
            }
        }
    }

    return positionedLines.map { line ->
        line.map { positionedLayout ->
            val wordLayouts = layoutsByWord.getValue(positionedLayout.wordId)
            positionedLayout.copy(
                wordPivot = Offset(
                    x = (wordLayouts.minOf { it.position.x } + wordLayouts.maxOf { it.position.x + it.width }) / 2f,
                    y = wordLayouts.maxOf { it.position.y + it.textLayoutResult.size.height }
                ),
                wordAnimInfo = animInfoByWord[positionedLayout.wordId],
                charOffsetInWord = charOffsetsBySyllable[positionedLayout] ?: 0
            )
        }
    }
}

fun trimDisplayLineTrailingSpaces(
    displayLineSyllables: List<SyllableLayout>, textMeasurer: TextMeasurer, style: TextStyle
): WrappedLine {
    if (displayLineSyllables.isEmpty()) return WrappedLine(emptyList(), 0f)

    val processed = displayLineSyllables.toMutableList()
    var lastIndex = processed.lastIndex
    while (lastIndex >= 0 && processed[lastIndex].syllable.content.isBlank()) {
        processed.removeAt(lastIndex)
        lastIndex--
    }
    if (processed.isEmpty()) return WrappedLine(emptyList(), 0f)

    val lastLayout = processed.last()
    val originalContent = lastLayout.syllable.content
    val trimmedContent = originalContent.trimEnd()
    if (trimmedContent.length < originalContent.length) {
        if (trimmedContent.isNotEmpty()) {
            val trimmedResult = textMeasurer.measure(trimmedContent, style)
            processed[processed.lastIndex] = lastLayout.copy(
                syllable = lastLayout.syllable.copy(content = trimmedContent),
                textLayoutResult = trimmedResult,
                width = trimmedResult.size.width.toFloat()
            )
        } else {
            processed.removeAt(processed.lastIndex)
        }
    }

    val totalWidth = processed.sumOf { it.width.toDouble() }.toFloat()
    return WrappedLine(processed, totalWidth)
}
