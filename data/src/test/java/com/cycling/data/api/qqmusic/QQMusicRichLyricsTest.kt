package com.cycling.data.api.qqmusic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * QRC 逐字歌词的纯解析部分。
 *
 * 两个函数都不碰网络：[qqSecondaryLines] 把译文/罗马音转成「行首毫秒 → 文本」，
 * [qqMergeTranslation] 再把它们按行插回逐字正文。它们的输出形状由 `YrcParser` 的配对规则
 * 决定 —— 译文行必须是 `[start,0]文本`（同 start、无逐字标签），否则每句译文会变成独立歌词。
 */
class QQMusicRichLyricsTest {

    // ── 译文解析 ──────────────────────────────────────────────────────

    @Test
    fun `secondary lines reads a plain lrc translation`() {
        val translation = """
            [00:01.00]line one
            [00:12.50]line two
        """.trimIndent()

        assertEquals(
            mapOf(1_000L to "line one", 12_500L to "line two"),
            qqSecondaryLines(translation)
        )
    }

    @Test
    fun `secondary lines strips qrc syllable tags`() {
        val translation = "[1000,2000](1000,500)译(1500,500)文\n[3000,1000](3000,1000)第二句"

        assertEquals(
            mapOf(1_000L to "译文", 3_000L to "第二句"),
            qqSecondaryLines(translation)
        )
    }

    @Test
    fun `secondary lines keeps the first line when timestamps repeat`() {
        val translation = "[00:05.00]first\n[00:05.00]second"

        assertEquals(mapOf(5_000L to "first"), qqSecondaryLines(translation))
    }

    @Test
    fun `secondary lines tolerates blank and garbage input`() {
        assertTrue(qqSecondaryLines("").isEmpty())
        assertTrue(qqSecondaryLines("   ").isEmpty())
        assertTrue(qqSecondaryLines("not a lyric at all").isEmpty())
    }

    @Test
    fun `secondary lines normalises one and three digit fractions`() {
        val translation = "[00:01.5]a\n[00:02.123]b"

        assertEquals(
            mapOf(1_500L to "a", 2_123L to "b"),
            qqSecondaryLines(translation)
        )
    }

    // ── 译文合并 ──────────────────────────────────────────────────────

    @Test
    fun `merge inserts the translation directly under its source line`() {
        val yrc = """
            [1000,2000](1000,1000)原(2000,1000)文
            [4000,1000](4000,1000)下(4500,500)句
        """.trimIndent()

        val merged = qqMergeTranslation(yrc, mapOf(1_000L to "译文"))

        assertEquals(
            """
                [1000,2000](1000,1000)原(2000,1000)文
                [1000,0]译文
                [4000,1000](4000,1000)下(4500,500)句
            """.trimIndent(),
            merged
        )
    }

    @Test
    fun `merge leaves lines without a translation untouched`() {
        val yrc = "[1000,2000](1000,2000)原(2000,1000)文"

        assertEquals(yrc, qqMergeTranslation(yrc, emptyMap()))
        assertEquals(yrc, qqMergeTranslation(yrc, mapOf(9_000L to "别处")))
    }

    @Test
    fun `merged document is paired by the yrc parser as translation`() {
        val yrc = "[1000,2000](1000,1000)原(2000,1000)文"
        val merged = qqMergeTranslation(yrc, mapOf(1_000L to "译文"))

        // 与 YrcParser.combineRawWithTranslation 的判定一致：同 start、无逐字标签即为译文。
        val lines = merged.lines()
        assertEquals(2, lines.size)
        assertTrue(lines[1].startsWith("[1000,0]"))
        assertTrue(lines[1].substringAfter(']').isNotBlank())
    }
}
