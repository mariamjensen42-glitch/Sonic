package com.cycling.domain.lyrics.parser

import com.cycling.domain.lyrics.model.SyncedLyrics
import com.cycling.domain.lyrics.model.synced.SyncedLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    private fun SyncedLyrics.getLine(index: Int): SyncedLine {
        return lines[index] as SyncedLine
    }

    @Test
    fun `parse standard LRC lyrics correctly`() {
        val lines = listOf(
            "[00:00.00]First line",
            "[00:05.00]Second line",
            "[00:10.00]Third line"
        )

        val result = LrcParser.parse(lines)

        assertEquals(3, result.lines.size)
        assertEquals("First line", result.getLine(0).content)
        assertEquals(0, result.lines[0].start)
        assertEquals("Second line", result.getLine(1).content)
        assertEquals(5000, result.lines[1].start)
        assertEquals("Third line", result.getLine(2).content)
        assertEquals(10000, result.lines[2].start)
    }

    @Test
    fun `parse LRC lyrics with translation correctly`() {
        val lines = listOf(
            "[00:00.00]Hello",
            "[00:00.00]你好",
            "[00:05.00]World",
            "[00:05.00]世界"
        )

        val result = LrcParser.parse(lines)

        assertEquals(2, result.lines.size)
        assertEquals("Hello", result.getLine(0).content)
        assertEquals("你好", result.getLine(0).translation)
        assertEquals("World", result.getLine(1).content)
        assertEquals("世界", result.getLine(1).translation)
    }

    @Test
    fun `parse empty lines returns empty lyrics`() {
        val lines = emptyList<String>()

        val result = LrcParser.parse(lines)

        assertTrue(result.lines.isEmpty())
    }

    @Test
    fun `parse lines with only metadata returns empty lyrics`() {
        val lines = listOf(
            "[ti:Song Title]",
            "[ar:Artist Name]",
            "[al:Album Name]"
        )

        val result = LrcParser.parse(lines)

        assertTrue(result.lines.isEmpty())
    }

    @Test
    fun `parse lines with invalid format are handled gracefully`() {
        val lines = listOf(
            "[00:00.00]Valid line",
            "Invalid line without timestamp",
            "[invalid]Invalid timestamp",
            "[00:05.00]Another valid line"
        )

        val result = LrcParser.parse(lines)

        assertEquals(2, result.lines.size)
        assertEquals("Valid line", result.getLine(0).content)
        assertEquals("Another valid line", result.getLine(1).content)
    }

    @Test
    fun `parse lines with different time formats`() {
        val lines = listOf(
            "[00:00.000]Millisecond format",
            "[00:05.50]Short millisecond",
            "[01:30.00]Minute format"
        )

        val result = LrcParser.parse(lines)

        assertEquals(3, result.lines.size)
        assertEquals(0, result.lines[0].start)
        assertEquals(5500, result.lines[1].start)
        assertEquals(90000, result.lines[2].start)
    }

    @Test
    fun `parse lines sets correct end time`() {
        val lines = listOf(
            "[00:00.00]First line",
            "[00:05.00]Second line"
        )

        val result = LrcParser.parse(lines)

        assertEquals(5000, result.lines[0].end)
        assertEquals(Int.MAX_VALUE, result.lines[1].end)
    }

    @Test
    fun `parse lines are sorted by start time`() {
        val lines = listOf(
            "[00:10.00]Third line",
            "[00:00.00]First line",
            "[00:05.00]Second line"
        )

        val result = LrcParser.parse(lines)

        assertEquals("First line", result.getLine(0).content)
        assertEquals("Second line", result.getLine(1).content)
        assertEquals("Third line", result.getLine(2).content)
    }

    @Test
    fun `parse lines with blank content are filtered`() {
        val lines = listOf(
            "[00:00.00]Valid line",
            "[00:05.00]   ",
            "[00:10.00]Another valid line"
        )

        val result = LrcParser.parse(lines)

        assertEquals(2, result.lines.size)
        assertEquals("Valid line", result.getLine(0).content)
        assertEquals("Another valid line", result.getLine(1).content)
    }
}
