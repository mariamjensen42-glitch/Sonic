package com.cycling.domain.lyrics.model

import com.cycling.domain.lyrics.model.synced.SyncedLine
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 高亮行集合的边界行为。
 *
 * 重点是「行末 = 下一行开头」这个 LRC 惯例：区间必须是左闭右开，
 * 否则点某一句跳过去时，它上一句会跟着一起亮。
 */
class SyncedLyricsHighlightTest {

    private fun lyrics(vararg ranges: Pair<Int, Int>) = SyncedLyrics(
        lines = ranges.mapIndexed { index, (start, end) ->
            SyncedLine(content = "line$index", translation = null, start = start, end = end)
        }
    )

    @Test
    fun `跳转到某句开头时只有这一句高亮`() {
        val lyrics = lyrics(0 to 1000, 1000 to 2000, 2000 to 3000)

        assertEquals(listOf(1), lyrics.getCurrentAllHighlightLineIndicesByTime(1000))
        assertEquals(listOf(2), lyrics.getCurrentAllHighlightLineIndicesByTime(2000))
    }

    @Test
    fun `行内任意时刻只有当前句`() {
        val lyrics = lyrics(0 to 1000, 1000 to 2000)

        assertEquals(listOf(0), lyrics.getCurrentAllHighlightLineIndicesByTime(999))
        assertEquals(listOf(1), lyrics.getCurrentAllHighlightLineIndicesByTime(1001))
    }

    @Test
    fun `对唱重叠时两句同时高亮`() {
        val lyrics = lyrics(0 to 3000, 1000 to 2000)

        assertEquals(listOf(0, 1), lyrics.getCurrentAllHighlightLineIndicesByTime(1500))
    }

    @Test
    fun `行间空隙保持最后一句高亮`() {
        val lyrics = lyrics(0 to 1000, 1500 to 2000)

        assertEquals(listOf(0), lyrics.getCurrentAllHighlightLineIndicesByTime(1200))
    }

    @Test
    fun `最后一句唱完仍然高亮`() {
        val lyrics = lyrics(0 to 1000, 1000 to 2000)

        assertEquals(listOf(1), lyrics.getCurrentAllHighlightLineIndicesByTime(9000))
    }

    @Test
    fun `第一句开始之前不高亮任何行`() {
        val lyrics = lyrics(500 to 1000)

        assertEquals(emptyList<Int>(), lyrics.getCurrentAllHighlightLineIndicesByTime(100))
    }
}
