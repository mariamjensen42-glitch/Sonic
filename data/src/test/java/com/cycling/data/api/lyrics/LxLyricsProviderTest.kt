package com.cycling.data.api.lyrics

import com.cycling.domain.model.LyricsQuery
import com.cycling.domain.online.model.OnlineLyric
import com.cycling.domain.online.repository.OnlinePluginRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 音源脚本歌词接入点：只测宿主这一侧的行为。
 *
 * 两件事值得钉住：本地歌曲不能去问脚本（它按平台 id 取词，问了也是白问一次启动开销），
 * 以及脚本给的两份文档（主歌词 + 翻译）要合成一份、翻译行紧跟主行 —— 这是解析器认的约定。
 */
class LxLyricsProviderTest {

    private val repository = mockk<OnlinePluginRepository>(relaxed = true)
    private val provider = LxLyricsProvider(repository)

    private val localQuery = LyricsQuery(
        title = "晴天",
        artist = "周杰伦",
        album = "叶惠美",
        durationMs = 269_000
    )

    private val onlineQuery = localQuery.copy(onlineSourceKey = "wy", onlineSongId = "186016")

    @Test
    fun `local tracks never reach the script`() = runTest {
        assertTrue(provider.fetchCandidates(localQuery).isEmpty())
        coVerify(exactly = 0) { repository.fetchLyric(any()) }
    }

    @Test
    fun `translation lines are interleaved under their main line`() = runTest {
        coEvery { repository.fetchLyric(any()) } returns OnlineLyric(
            lyric = "[00:01.00]晴天\n[00:05.00]故事的小黄花",
            translation = "[00:01.00]Sunny Day\n[00:05.00]The little yellow flower"
        )

        val candidates = provider.fetchCandidates(onlineQuery)

        assertEquals(1, candidates.size)
        assertEquals(
            "[00:01.00]晴天\n[00:01.00]Sunny Day\n" +
                "[00:05.00]故事的小黄花\n[00:05.00]The little yellow flower",
            candidates.single().rawContent
        )
    }

    @Test
    fun `script failure degrades to no lyrics`() = runTest {
        coEvery { repository.fetchLyric(any()) } throws IllegalStateException("脚本未返回歌词")

        assertTrue(provider.fetchCandidates(onlineQuery).isEmpty())
    }

    @Test
    fun `qrc xml is converted to yrc`() = runTest {
        coEvery { repository.fetchLyric(any()) } returns OnlineLyric(
            lyric = """<Lyric_1 LyricType="1" LyricContent="[0,3000]词(0,500)语(500,500)"/>"""
        )

        val raw = provider.fetchCandidates(onlineQuery).single().rawContent

        assertEquals("[0,3000](0,500,0)词(500,500,0)语", raw)
    }
}
