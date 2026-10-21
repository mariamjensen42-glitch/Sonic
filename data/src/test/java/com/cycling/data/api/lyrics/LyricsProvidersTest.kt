package com.cycling.data.api.lyrics

import com.cycling.domain.model.LyricsQuery
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 酷狗 / QQ 音乐歌词接口的纯解析部分。
 *
 * 两个端点都是未官方的，字段名与编码方式（base64、JSONP 包装、非零 `code` 表示“没有”）都是
 * 实测抓包的结果，用这里的 payload 钉住；解析失败必须返回 `null` 而不是抛异常，调用方才能
 * 安静地换下一个来源。
 */
class LyricsProvidersTest {

    private val query = LyricsQuery(
        title = "晴天",
        artist = "周杰伦",
        album = "叶惠美",
        durationMs = 269_000
    )

    // ── Kugou ────────────────────────────────────────────────────────

    private val kugouSearch = """
        {"status":200,"info":"OK","errcode":200,"candidates":[
          {"id":"722828582","accesskey":"CF0F","song":"晴天","singer":"晴天","duration":210000},
          {"id":"829295110","accesskey":"BF23","song":"晴天","singer":"周杰伦","duration":269000},
          {"id":"679221641","accesskey":"7C07","song":"晴天","singer":"周杰伦","duration":269792},
          {"id":"","accesskey":"","song":"晴天","singer":"周杰伦","duration":269000}
        ]}
    """.trimIndent()

    @Test
    fun `kugou ranks candidates by length and requires an id and access key`() {
        val candidates = parseKugouLyricCandidates(kugouSearch, query)

        assertEquals(listOf("829295110", "679221641"), candidates.map { it.id })
    }

    @Test
    fun `kugou drops every candidate outside the length tolerance`() {
        val farOff = query.copy(durationMs = 100_000)

        assertTrue(parseKugouLyricCandidates(kugouSearch, farOff).isEmpty())
    }

    @Test
    fun `kugou decodes the base64 LRC it serves`() {
        val lrc = "[00:01.00]故事的小黄花\n[00:02.00]从出生那年就飘着"
        val encoded = Base64.getEncoder().encodeToString(lrc.toByteArray(Charsets.UTF_8))

        assertEquals(lrc, decodeKugouLrc(encoded))
    }

    @Test
    fun `kugou treats anything that is not an LRC as nothing found`() {
        assertNull(decodeKugouLrc(null))
        assertNull(decodeKugouLrc(""))
        assertNull(decodeKugouLrc(Base64.getEncoder().encodeToString("not lyrics".toByteArray())))
        assertNull(decodeKugouLrc("!!! definitely not base64 !!!"))
    }

    // ── QQ Music ─────────────────────────────────────────────────────

    private val qqSearch = """
        {"code":0,"data":{"song":{"list":[
          {"songname":"晴天","songmid":"0039MnYb0qxYhV","interval":269,"singer":[{"name":"周杰伦"}]},
          {"songname":"晴天","songmid":"004Z8Ihr0JIu5s","interval":210,"singer":[{"name":"某翻唱"}]},
          {"songname":"晴天","songmid":"","interval":269,"singer":[{"name":"周杰伦"}]}
        ]}}}
    """.trimIndent()

    @Test
    fun `qq candidates need a songmid and a length inside the tolerance`() {
        val candidates = parseQqLyricCandidates(qqSearch, query)

        assertEquals(listOf("0039MnYb0qxYhV"), candidates.map { it.songmid })
    }

    @Test
    fun `qq search wrapped in a callback is still readable`() {
        assertEquals(1, parseQqLyricCandidates("callback($qqSearch);", query).size)
    }

    @Test
    fun `qq reads the plain LRC`() {
        val body = """{"retcode":0,"code":0,"lyric":"[00:01.00]line one\n[00:02.00]line two"}"""

        assertEquals("[00:01.00]line one\n[00:02.00]line two", parseQqLyrics(body))
    }

    @Test
    fun `qq reads the base64 variant when the endpoint ignores nobase64`() {
        val encoded = Base64.getEncoder().encodeToString("[00:01.00]line one".toByteArray(Charsets.UTF_8))

        assertEquals("[00:01.00]line one", parseQqLyrics("""{"retcode":0,"code":0,"lyric":"$encoded"}"""))
    }

    @Test
    fun `qq reports a missing lyric through a non-zero code`() {
        assertNull(parseQqLyrics("""{"retcode":-1901,"code":-1901}"""))
    }

    @Test
    fun `qq places the translation under the line it translates`() {
        val lyric = "[00:01.00]line one\n[00:02.00]line two"
        val translation = "[00:01.00]第一行\n[00:02.00]第二行"

        assertEquals(
            "[00:01.00]line one\n[00:01.00]第一行\n[00:02.00]line two\n[00:02.00]第二行",
            mergeQqTranslation(lyric, translation)
        )
    }

    @Test
    fun `qq without a translation keeps the plain lyric`() {
        val lyric = "[00:01.00]line one"

        assertEquals(lyric, mergeQqTranslation(lyric, null))
        assertEquals(lyric, mergeQqTranslation(lyric, ""))
    }
}
