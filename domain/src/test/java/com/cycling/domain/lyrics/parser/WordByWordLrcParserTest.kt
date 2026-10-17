package com.cycling.domain.lyrics.parser

import com.cycling.domain.lyrics.model.karaoke.KaraokeLine
import com.cycling.domain.lyrics.utils.LyricsFormatGuesser
import org.junit.Assert.assertEquals
import org.junit.Test

class WordByWordLrcParserTest {

    // 与真实文件结构一致：行时间戳 + 行首字 + 逐字时间戳，译文行共用同一个行时间戳
    private val content = """
[ti:Good-bye My Loneliness]
[offset:0]
[tool:LDDC v0.9.2 https://github.com/chenmozhijin/LDDC]

[00:37.290]心[00:37.570]の[00:38.050]奥[00:38.910]を[00:39.720]あ[00:40.170]な[00:40.380]た[00:40.890]に[00:43.560]
[00:37.290]你好像总能一眼看穿我的心事[00:43.560]
[00:45.250]瞳[00:45.780]を[00:46.180]そ[00:46.610]ら[00:47.360]し[00:49.570]て[00:52.440]
[00:45.250]就算移开目光也无济于事[00:52.440]
""".trimIndent()

    private fun parse(): List<KaraokeLine> {
        val parsed = AutoParser(LyricsFormatGuesser()).parse(content)
        return parsed.lines.filterIsInstance<KaraokeLine>()
    }

    @Test
    fun `keeps the leading character before the first word tag`() {
        val first = parse().first()

        assertEquals("心の奥をあなたに", first.syllables.joinToString("") { it.content })
    }

    @Test
    fun `line starts at the line tag time`() {
        val first = parse().first()

        assertEquals(37290, first.start)
        assertEquals(43560, first.end)
    }

    @Test
    fun `pairs the same timestamp line as translation`() {
        val lines = parse()

        assertEquals(2, lines.size)
        assertEquals("你好像总能一眼看穿我的心事", lines[0].translation)
        assertEquals("就算移开目光也无济于事", lines[1].translation)
    }

    @Test
    fun `translation lines are not left standing on their own`() {
        val lines = parse()

        assertEquals(2, lines.size)
        assertEquals(0, lines.count { it.translation == null })
    }
}
