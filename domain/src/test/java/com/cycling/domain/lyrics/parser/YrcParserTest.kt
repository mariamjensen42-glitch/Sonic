package com.cycling.domain.lyrics.parser

import com.cycling.domain.lyrics.model.karaoke.KaraokeLine
import com.cycling.domain.lyrics.utils.LyricsFormatGuesser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YrcParserTest {

    // 与网易云返回的结构一致：行标签 [起始,时长] 在前，逐字标签 (起始,时长,?) 描述它后面的字，
    // 译文行共用同一行标签、且不带逐字标签。
    private val content = """
[20250,4530](20250,630,0)Once (20880,240,0)I (21120,180,0)was (21300,660,0)seven (21960,480,0)years (22440,420,0)old (22860,360,0)my (23220,600,0)momma (23820,540,0)told (24360,420,0)me
[20250,4530]那年我七岁 妈妈就对我说
[24780,3480](24780,480,0)Go (25260,420,0)make (25680,450,0)yourself (26130,240,0)some (26370,360,0)friends (26730,150,0)or (26880,270,0)you'll (27150,150,0)be (27300,960,0)lonely
[24780,3480]去交些朋友 不然你会孤独寂寞
""".trimIndent()

    private val wordByWordOnly = """
[15010,4470](15010,330,0)这(15340,210,0)个(15550,310,0)失(15860,450,0)眠(16310,810,0)夜 (17120,360,0)想(17480,100,0)不(17580,530,0)起(18110,250,0)你(18360,280,0)是(18640,840,0)谁
""".trimIndent()

    private fun parse(source: String = content): List<KaraokeLine> {
        val parsed = AutoParser(LyricsFormatGuesser()).parse(source)
        return parsed.lines.filterIsInstance<KaraokeLine>()
    }

    @Test
    fun `keeps every word as its own syllable`() {
        val first = parse().first()

        assertEquals("Once I was seven years old my momma told me", first.syllables.joinToString("") { it.content })
    }

    @Test
    fun `syllables carry the start and end their own tag gives them`() {
        val syllable = parse().first().syllables.first()

        assertEquals(20250, syllable.start)
        assertEquals(20880, syllable.end)
    }

    @Test
    fun `a line spans its line tag rather than its syllables`() {
        val first = parse().first()

        assertEquals(20250, first.start)
        assertEquals(24780, first.end)
    }

    @Test
    fun `pairs the line below as the translation`() {
        val lines = parse()

        assertEquals(2, lines.size)
        assertEquals("那年我七岁 妈妈就对我说", lines[0].translation)
        assertEquals("去交些朋友 不然你会孤独寂寞", lines[1].translation)
    }

    @Test
    fun `translation lines are not left standing on their own`() {
        val lines = parse()

        assertEquals(2, lines.size)
        assertEquals(0, lines.count { it.translation == null })
    }

    @Test
    fun `a line with no translation below it stays untranslated`() {
        val line = parse(wordByWordOnly).single()

        assertNull(line.translation)
        assertEquals("这个失眠夜 想不起你是谁", line.syllables.joinToString("") { it.content })
        assertEquals(15010, line.start)
        assertEquals(19480, line.end)
    }

    // 逐字行允许重叠，同一时间戳上可以有多行；只有不带逐字标签的那一行才是译文。
    @Test
    fun `two lines sharing a start are not mistaken for a translation pair`() {
        val overlapping = """
[1000,2000](1000,500,0)la
[1000,2000](1000,500,0)la
""".trimIndent()

        val lines = parse(overlapping)

        assertEquals(2, lines.size)
        assertEquals(0, lines.count { it.translation != null })
    }
}
