package com.cycling.domain.lyrics.parser

import com.cycling.domain.lyrics.model.karaoke.KaraokeAlignment
import com.cycling.domain.lyrics.model.karaoke.KaraokeLine
import com.cycling.domain.lyrics.model.karaoke.KaraokeSyllable
import com.cycling.domain.lyrics.utils.LyricsFormatGuesser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 真实形态 TTML（ttm:agent 对齐、iTunes 翻译、x-bg 伴唱）锁定 [TTMLParser] 的行为。
 *
 * 这份样例同时是 XML 解析层的护栏：换掉手写解析器实现前，先拿它对着旧实现跑绿。
 * 标签刻意不缩进——`AutoParser` 按行 split/trimIndent 后 join("")，缩进空白在
 * 切分时即被抹掉，写紧凑些让 DOM 树与预期一一对应。
 */
class TTMLParserTest {

    private val ttml = """
<?xml version="1.0" encoding="UTF-8"?>
<tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" xmlns:itunes="http://music.apple.com/lyric-ttml" itunes:timing="word">
<head>
<metadata>
<ttm:agent xml:id="v1" type="person"/>
<ttm:agent xml:id="v2" type="other"/>
<iTunesMetadata>
<translations>
<translation for="L1">
<text for="L1">你好世界（Hello world）</text>
</translation>
</translations>
</iTunesMetadata>
</metadata>
</head>
<body>
<div begin="0" end="60.000">
<p begin="00:01.000" end="00:03.000" ttm:agent="v1" itunes:key="L1">
<span begin="00:01.000" end="00:01.500">Hello</span>
<span begin="00:01.500" end="00:02.000">world</span>
<span ttm:role="x-translation" begin="00:01.000" end="00:03.000">你好世界</span>
</p>
<p begin="00:02.000" end="00:03.000" ttm:agent="v2">
<span begin="00:02.000" end="00:02.500">foo</span>
<span ttm:role="x-bg" begin="00:02.600" end="00:02.900">
<span begin="00:02.600" end="00:02.750">bar</span>
</span>
</p>
</div>
</body>
</tt>
""".trimIndent()

    private fun parse() = AutoParser(LyricsFormatGuesser()).parse(ttml)

    @Test
    fun `word-by-word lines with agent alignment and translations`() {
        val result = parse()

        assertTrue(result.isWordByWord)
        val lines = result.lines.filterIsInstance<KaraokeLine>()
        assertEquals(3, lines.size)

        val main = lines[0]
        assertEquals(false, main.isAccompaniment)
        assertEquals(KaraokeAlignment.Start, main.alignment)
        assertEquals(1000, main.start)
        assertEquals(3000, main.end)
        assertEquals("你好世界", main.translation)
        assertEquals(
            listOf(
                KaraokeSyllable("Hello", 1000, 1500),
                KaraokeSyllable("world", 1500, 2000)
            ),
            main.syllables
        )

        val second = lines[1]
        assertEquals(false, second.isAccompaniment)
        assertEquals(KaraokeAlignment.End, second.alignment)
        assertEquals(2000, second.start)
        assertEquals(3000, second.end)
        assertEquals(null, second.translation)
        assertEquals(listOf(KaraokeSyllable("foo", 2000, 2500)), second.syllables)

        val accompaniment = lines[2]
        assertEquals(true, accompaniment.isAccompaniment)
        assertEquals(KaraokeAlignment.End, accompaniment.alignment)
        assertEquals(2600, accompaniment.start)
        assertEquals(2900, accompaniment.end)
        assertEquals(null, accompaniment.translation)
        assertEquals(listOf(KaraokeSyllable("bar", 2600, 2750)), accompaniment.syllables)
    }

    @Test
    fun `unrecognised content yields nothing`() {
        val result = AutoParser(LyricsFormatGuesser()).parse("just plain lyrics, no timestamps")
        assertTrue(result.lines.isEmpty())
    }

    /**
     * AMLL 词库实际返回的形态（captured from `api.amll.dev`，节选两行）。
     *
     * 与上面的手写样例有两处不同，都是解析器容易踩的点：标签之间完全不缩进、不换行（`TTMLParser`
     * 先按行 trimIndent 再 join），并且英文音节之间的空格是 span 之外的裸文本节点 —— 音节内容必须
     * 带上那个尾随空格，否则「ReSoSo」会粘成一个词。头部也换成了 `amll:meta`，没有 iTunes 翻译表。
     */
    private val amllTtml = """
<?xml version="1.0" encoding="utf-8"?>
<tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" xmlns:itunes="http://music.apple.com/lyric-ttml-internal" xmlns:amll="http://www.example.com/ns/amll"><head><metadata xmlns=""><ttm:agent type="person" xml:id="v1"/><amll:meta key="musicName" value="晴天"/></metadata></head><body dur="00:05.000"><div xmlns="" begin="00:01.000" end="00:05.000"><p begin="00:01.000" end="00:03.000" ttm:agent="v1" itunes:key="L1"><span begin="00:01.000" end="00:01.500">故</span><span begin="00:01.500" end="00:02.000">事</span></p><p begin="00:03.000" end="00:05.000" ttm:agent="v1" itunes:key="L2"><span begin="00:03.000" end="00:03.500">Re</span> <span begin="00:03.500" end="00:04.000">So</span></p></div></body></tt>
""".trimIndent()

    @Test
    fun `parses the compact TTML that AMLL publishes`() {
        val result = AutoParser(LyricsFormatGuesser()).parse(amllTtml)

        assertTrue(result.isWordByWord)
        val lines = result.lines.filterIsInstance<KaraokeLine>()
        assertEquals(2, lines.size)

        assertEquals(1000, lines[0].start)
        assertEquals(3000, lines[0].end)
        assertEquals(listOf("故", "事"), lines[0].syllables.map { it.content })
        assertEquals(null, lines[0].translation)

        // 英文音节之间的空格来自 span 后面的裸文本节点，末尾那格不能再被 trim 掉。
        assertEquals(listOf("Re ", "So"), lines[1].syllables.map { it.content })
    }
}
