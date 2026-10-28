package com.cycling.domain.scrape

import com.cycling.domain.model.CoverCandidate
import com.cycling.domain.model.CoverSource
import com.cycling.domain.model.ScrapeField
import com.cycling.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 刮削的匹配规则。
 *
 * 这里锁的是「批量自动刮削凭什么敢不停下来问用户」：标题对得上、或标题近似且艺术家对得上
 * 才算可信。规则错了不会崩，但会把整库的标题改成同名翻唱 —— 比没有元数据更难发现。
 */
class ScrapeMatchTest {

    @Test
    fun `normalize drops brackets, feat and punctuation`() {
        assertEquals("稻香", ScrapeMatch.normalize("稻香 (Live)"))
        assertEquals("晴天", ScrapeMatch.normalize("晴天【官方版】"))
        assertEquals("lovestory", ScrapeMatch.normalize("Love Story feat. Someone"))
        assertEquals("abc", ScrapeMatch.normalize("A.B-C!"))
    }

    @Test
    fun `placeholder values count as missing`() {
        assertEquals(
            setOf(ScrapeField.ARTIST, ScrapeField.ALBUM),
            ScrapeMatch.missingFields(song(title = "稻香", artist = "<unknown>", album = "未知专辑"))
        )
        assertEquals(
            setOf(ScrapeField.ARTIST),
            ScrapeMatch.missingFields(song(title = "稻香", artist = "未知艺术家", album = "魔杰座"))
        )
        assertEquals(emptySet<ScrapeField>(), ScrapeMatch.missingFields(song()))
    }

    @Test
    fun `title copied from file name counts as missing`() {
        // 扫描器在文件没有标题标签时用文件名当标题，界面上看不出来
        val fromFileName = song(
            title = "周杰伦 - 稻香",
            path = "/storage/emulated/0/Music/周杰伦 - 稻香.mp3"
        )
        assertTrue(ScrapeMatch.missingFields(fromFileName).contains(ScrapeField.TITLE))

        // 单曲名与文件名相同（如 `Intro.mp3`）不算问题
        val plain = song(title = "Intro", path = "/storage/emulated/0/Music/Intro.mp3")
        assertFalse(ScrapeMatch.missingFields(plain).contains(ScrapeField.TITLE))
    }

    @Test
    fun `same title is confident even without a known artist`() {
        val local = song(title = "稻香", artist = "未知艺术家")
        val remote = candidate(title = "稻香", artist = "周杰伦")
        assertTrue(ScrapeMatch.confidence(local, remote) >= ScrapeMatch.AUTO_APPLY_THRESHOLD)
    }

    @Test
    fun `different title is not confident`() {
        val local = song(title = "稻香")
        val remote = candidate(title = "告白气球", artist = "周杰伦")
        assertTrue(ScrapeMatch.confidence(local, remote) < ScrapeMatch.AUTO_APPLY_THRESHOLD)
    }

    @Test
    fun `title plus artist plus duration is the strongest match`() {
        val local = song(title = "晴天", artist = "周杰伦", duration = 269_000)
        val remote = candidate(title = "晴天", artist = "周杰伦", durationMs = 269_500)
        assertEquals(1f, ScrapeMatch.confidence(local, remote), 0.001f)
    }

    @Test
    fun `duration far off pulls the score down`() {
        val local = song(title = "晴天", artist = "周杰伦", duration = 269_000)
        val sameSongDifferentCut = candidate(title = "晴天", artist = "周杰伦", durationMs = 360_000)
        assertTrue(ScrapeMatch.confidence(local, sameSongDifferentCut) < ScrapeMatch.AUTO_APPLY_THRESHOLD)
    }

    @Test
    fun `rank prefers the closer title and keeps source order on ties`() {
        val local = song(title = "稻香", artist = "周杰伦", duration = 223_000)
        val wrong = candidate(id = "wrong", title = "稻香 (钢琴版)", artist = "群星")
        val right = candidate(id = "right", title = "稻香", artist = "周杰伦", durationMs = 223_400)

        val ranked = ScrapeMatch.rank(local, listOf(wrong, right))
        assertEquals("right", ranked.first().id)
    }

    private fun song(
        title: String = "晴天",
        artist: String = "周杰伦",
        album: String = "叶惠美",
        duration: Long = 269_000,
        path: String = "/storage/emulated/0/Music/qingtian.mp3"
    ) = Song(
        id = 1L,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        path = path,
        albumId = 1L,
        artistId = 1L,
        dateAdded = 0L,
        dateModified = 0L,
        size = 0L,
        mimeType = "audio/mpeg"
    )

    private fun candidate(
        id: String = "c1",
        title: String? = "晴天",
        artist: String? = "周杰伦",
        album: String? = "叶惠美",
        durationMs: Long? = null
    ) = CoverCandidate(
        id = id,
        source = CoverSource.ITUNES,
        imageUrl = "https://example.com/a.jpg",
        album = album,
        artist = artist,
        title = title,
        durationMs = durationMs
    )
}
