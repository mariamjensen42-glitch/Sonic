package com.cycling.data.api.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AMLL 文档的两条验收：**有没有可播的行**、**是不是比本地音频还长**。
 *
 * 前者决定「乐器和占位文档不能冒充歌词」，后者只做单向否决 —— AMLL 的 `dur` 是最后一句歌词的
 * 结束时间而不是音轨长度，所以「比 dur 短」不代表不是同一首歌，但「比整首歌还长」一定是另一次
 * 录音。用实测抓到的 TTML（`晴天`）钉住头部结构。
 */
class AmllDocumentTest {

    private val ttml = """
        <tt xmlns="http://www.w3.org/ns/ttml"><head><metadata><amll:meta key="musicName" value="晴天"/></metadata></head>
        <body dur="04:25.541"><div begin="00:29.231" end="04:25.541">
        <p begin="00:29.231" end="00:32.723"><span begin="00:29.231" end="00:29.692">故</span></p>
        </div></body></tt>
    """.trimIndent()

    @Test
    fun `a document with timed lines is playable`() {
        assertTrue(amllHasTimedLines(ttml))
    }

    @Test
    fun `an instrumental or placeholder document is not playable`() {
        // 只有 <head> 元数据：显示出来是一片空白，比「没有歌词」更糟 —— 面板会以为这家答过了。
        val noLines = """
            <tt xmlns="http://www.w3.org/ns/ttml"><head><metadata>
            <amll:meta key="musicName" value="晴天"/></metadata></head><body dur="00:00.000"/></tt>
        """.trimIndent()

        assertFalse(amllHasTimedLines(noLines))
        assertFalse(amllHasTimedLines(""))
    }

    @Test
    fun `a line without a begin time is not playable`() {
        assertFalse(amllHasTimedLines("""<tt><body><p itunes:key="L1"><span>故</span></p></body></tt>"""))
    }

    @Test
    fun `lyrics that end past the file's length are another recording`() {
        // 本地 3:20 的文件配一份 4:25 的歌词：现场版 / 加长版，不能拿来用。
        assertTrue(amllExceedsTrack(ttml, 200_000))
    }

    @Test
    fun `lyrics that fit inside the file are accepted`() {
        // 4:29 的曲子配 4:25 的歌词：正常（尾奏没有词）。
        assertFalse(amllExceedsTrack(ttml, 269_000))
    }

    @Test
    fun `a short outro inside the margin is still accepted`() {
        // 4:25.5 的歌词配 4:10 的文件，差 15 秒落在余量里 —— 裁剪 / 编码差异，不当成别的录音。
        assertFalse(amllExceedsTrack(ttml, 250_000))
    }

    @Test
    fun `an unknown length never rejects a document`() {
        assertFalse(amllExceedsTrack(ttml, 0))
    }

    @Test
    fun `a document without a declared duration is not judged`() {
        assertFalse(amllExceedsTrack("<tt><body><p begin=\"00:01.000\">x</p></body></tt>", 30_000))
    }

    @Test
    fun `parses the clock time TTML uses`() {
        assertEquals(265_541L, parseTtmlClock("04:25.541"))
        assertEquals(5_000L, parseTtmlClock("00:05.000"))
        assertEquals(3_725_500L, parseTtmlClock("01:02:05.500"))
    }

    @Test
    fun `rejects a nonsense clock time`() {
        assertNull(parseTtmlClock(""))
        assertNull(parseTtmlClock("04"))
        assertNull(parseTtmlClock("aa:bb"))
    }
}
