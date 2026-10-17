package com.cycling.data.repository

import com.cycling.data.local.dao.LyricsCacheDao
import com.cycling.data.local.dao.LyricsIndexDao
import com.cycling.data.local.dao.LyricsOverrideDao
import com.cycling.data.local.entity.LyricsCacheEntity
import com.cycling.data.local.entity.LyricsOverrideEntity
import com.cycling.data.local.lyrics.EmbeddedLyricsHelper
import com.cycling.data.local.lyrics.LyricsFileHelper
import com.cycling.domain.lyrics.provider.OnlineLyrics
import com.cycling.domain.lyrics.provider.OnlineLyricsProvider
import com.cycling.domain.lyrics.model.karaoke.KaraokeLine
import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.model.LyricsSource
import com.cycling.domain.model.Song
import com.cycling.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LyricsRepositoryImplTest {

    private lateinit var embeddedLyricsHelper: EmbeddedLyricsHelper
    private lateinit var lyricsFileHelper: LyricsFileHelper
    private lateinit var lyricsCacheDao: LyricsCacheDao
    private lateinit var lyricsOverrideDao: LyricsOverrideDao
    private lateinit var lyricsIndexDao: LyricsIndexDao
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var netEase: OnlineLyricsProvider
    private lateinit var lrclib: OnlineLyricsProvider
    private lateinit var repository: LyricsRepositoryImpl

    private val song = Song(
        id = 1L,
        title = "Song",
        artist = "Artist",
        album = "Album",
        duration = 180_000L,
        path = "/storage/emulated/0/Music/song.mp3",
        albumId = 1L,
        artistId = 1L,
        dateAdded = 0L,
        dateModified = 0L,
        size = 1L,
        mimeType = "audio/mpeg"
    )

    private val lrc = "[00:01.00]first line\n[00:05.00]second line"

    private val yrc = "[1000,2000](1000,1000,0)first (2000,1000,0)line"

    @Before
    fun setup() {
        embeddedLyricsHelper = mockk()
        lyricsFileHelper = mockk()
        lyricsCacheDao = mockk()
        lyricsOverrideDao = mockk()
        lyricsIndexDao = mockk(relaxed = true)
        settingsRepository = mockk()
        netEase = mockk()
        lrclib = mockk()

        every { netEase.provider } returns LyricsProvider.NETEASE
        every { lrclib.provider } returns LyricsProvider.LRCLIB
        every { settingsRepository.onlineLyricsEnabled } returns flowOf(true)
        every { settingsRepository.lyricsProviderOrder } returns
            flowOf(listOf(LyricsProvider.NETEASE, LyricsProvider.LRCLIB))

        coEvery { lyricsFileHelper.getLyricsContent(any()) } returns null
        coEvery { embeddedLyricsHelper.extractLyrics(any()) } returns null
        coEvery { lyricsCacheDao.get(any()) } returns null
        coEvery { lyricsCacheDao.put(any()) } returns Unit
        coEvery { lyricsOverrideDao.get(any()) } returns null
        coEvery { lyricsOverrideDao.put(any()) } returns Unit
        coEvery { lyricsOverrideDao.delete(any()) } returns Unit

        repository = LyricsRepositoryImpl(
            embeddedLyricsHelper,
            lyricsFileHelper,
            lyricsCacheDao,
            lyricsOverrideDao,
            lyricsIndexDao,
            settingsRepository,
            setOf(netEase, lrclib)
        )
    }

    private fun anyProviderServes(content: String) {
        coEvery { netEase.fetch(any()) } returns OnlineLyrics(content)
        coEvery { lrclib.fetch(any()) } returns OnlineLyrics(content)
    }

    /**
     * A cache that behaves like the table rather than like a stub, so that deleting a row really
     * does hide it from the next read — which is the whole of what a manual refresh does.
     */
    private fun cacheHolding(vararg rows: LyricsCacheEntity) {
        val rowsByKey = rows.associateBy { it.cacheKey }.toMutableMap()
        coEvery { lyricsCacheDao.get(any()) } answers { rowsByKey[firstArg()] }
        coEvery { lyricsCacheDao.delete(any()) } answers { rowsByKey.remove(firstArg()); Unit }
    }

    // ── local sources ────────────────────────────────────────────────

    @Test
    fun `sidecar file wins and no provider is consulted`() = runTest {
        coEvery { lyricsFileHelper.getLyricsContent(song.path) } returns lrc

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.FILE, result.source)
        assertEquals(2, result.syncedLyrics?.lines?.size)
        coVerify(exactly = 0) { netEase.fetch(any()) }
        coVerify(exactly = 0) { lrclib.fetch(any()) }
    }

    @Test
    fun `embedded tags are used when there is no sidecar file`() = runTest {
        coEvery { embeddedLyricsHelper.extractLyrics(song.path) } returns lrc

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.EMBEDDED, result.source)
        coVerify(exactly = 0) { netEase.fetch(any()) }
    }

    @Test
    fun `an untimed sidecar file does not count as an answer`() = runTest {
        coEvery { lyricsFileHelper.getLyricsContent(song.path) } returns "just some words, no timings"
        anyProviderServes(lrc)

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.ONLINE, result.source)
        coVerify(exactly = 1) { netEase.fetch(any()) }
    }

    @Test
    fun `an untimed embedded tag does not block the online fallback`() = runTest {
        // The common case for MP3s: a USLT frame holds unsynchronised lyrics the player cannot
        // follow, and downloading timed ones is strictly better than showing nothing.
        coEvery { embeddedLyricsHelper.extractLyrics(song.path) } returns "just some words, no timings"
        anyProviderServes(lrc)

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.ONLINE, result.source)
        assertEquals(2, result.syncedLyrics?.lines?.size)
    }

    // ── the user's own lyrics ────────────────────────────────────────

    @Test
    fun `the user's own lyrics win over every automatic source`() = runTest {
        coEvery { lyricsOverrideDao.get(song.id) } returns
            LyricsOverrideEntity(song.id, "[00:01.00]my own line", updatedAt = 0L)
        coEvery { lyricsFileHelper.getLyricsContent(song.path) } returns lrc
        anyProviderServes(lrc)

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.USER, result.source)
        assertEquals("[00:01.00]my own line", result.rawContent)
        coVerify(exactly = 0) { netEase.fetch(any()) }
    }

    @Test
    fun `lyrics the user typed without timings are still served`() = runTest {
        // The player cannot follow these, but the detail page can, and discarding something the
        // user typed because it lacks timings would be the wrong trade.
        coEvery { lyricsOverrideDao.get(song.id) } returns
            LyricsOverrideEntity(song.id, "just some words", updatedAt = 0L)

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.USER, result.source)
        assertEquals("just some words", result.rawContent)
        assertNull(result.syncedLyrics)
    }

    @Test
    fun `saving lyrics stores them against the song, not against a fingerprint`() = runTest {
        val result = repository.saveLyrics(song, lrc)

        assertEquals(LyricsSource.USER, result.source)
        assertEquals(2, result.syncedLyrics?.lines?.size)
        coVerify {
            lyricsOverrideDao.put(
                match { it.songId == song.id && it.rawContent == lrc }
            )
        }
    }

    @Test
    fun `clearing the user's lyrics hands the song back to the automatic sources`() = runTest {
        repository.clearLyrics(song)

        coVerify(exactly = 1) { lyricsOverrideDao.delete(song.id) }
    }

    // ── cache ────────────────────────────────────────────────────────

    @Test
    fun `a downloaded copy is served without touching the network`() = runTest {
        coEvery { lyricsCacheDao.get(any()) } returns cachedEntity()

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.ONLINE, result.source)
        assertTrue(result.syncedLyrics?.lines?.isNotEmpty() == true)
        coVerify(exactly = 0) { netEase.fetch(any()) }
        coVerify(exactly = 0) { lrclib.fetch(any()) }
    }

    @Test
    fun `the cache is keyed by title, artist and length`() = runTest {
        repository.getLyrics(song)

        coVerify { lyricsCacheDao.get("song|artist|180") }
    }

    @Test
    fun `the cache is consulted even when online lookup is switched off`() = runTest {
        every { settingsRepository.onlineLyricsEnabled } returns flowOf(false)
        coEvery { lyricsCacheDao.get(any()) } returns cachedEntity()

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.ONLINE, result.source)
    }

    @Test
    fun `an unreadable cache entry falls through to the network`() = runTest {
        coEvery { lyricsCacheDao.get(any()) } returns cachedEntity(rawContent = "garbage")
        anyProviderServes(lrc)

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.ONLINE, result.source)
        assertNotEquals("garbage", result.rawContent)
        coVerify(exactly = 1) { netEase.fetch(any()) }
    }

    @Test
    fun `a remembered miss is served without touching the network`() = runTest {
        coEvery { lyricsCacheDao.get(any()) } returns missEntity()

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.NONE, result.source)
        coVerify(exactly = 0) { netEase.fetch(any()) }
    }

    @Test
    fun `a stale remembered miss is looked up again`() = runTest {
        coEvery { lyricsCacheDao.get(any()) } returns missEntity(createdAt = 0L)
        anyProviderServes(lrc)

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.ONLINE, result.source)
        coVerify(exactly = 1) { netEase.fetch(any()) }
    }

    @Test
    fun `a day-old line-level copy is looked up again`() = runTest {
        // NetEase lists one recording many times over and the closest entry does not always carry
        // the word-by-word document, so a line-level copy is provisional and has to be re-checked.
        coEvery { lyricsCacheDao.get(any()) } returns cachedEntity(rawContent = lrc, createdAt = aDayAgo())
        anyProviderServes(yrc)

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.ONLINE, result.source)
        assertTrue(result.syncedLyrics?.lines?.all { it is KaraokeLine } == true)
        coVerify(exactly = 1) { netEase.fetch(any()) }
    }

    @Test
    fun `a day-old word-by-word copy is still served without touching the network`() = runTest {
        // Nothing beats it, so re-downloading it every day would only spend requests.
        coEvery { lyricsCacheDao.get(any()) } returns cachedEntity(rawContent = yrc, createdAt = aDayAgo())

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.ONLINE, result.source)
        assertTrue(result.syncedLyrics?.lines?.all { it is KaraokeLine } == true)
        coVerify(exactly = 0) { netEase.fetch(any()) }
        coVerify(exactly = 0) { lrclib.fetch(any()) }
    }

    // ── online ───────────────────────────────────────────────────────

    @Test
    fun `online lyrics are downloaded, cached and returned`() = runTest {
        anyProviderServes(lrc)

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.ONLINE, result.source)
        assertEquals(2, result.syncedLyrics?.lines?.size)
        coVerify {
            lyricsCacheDao.put(
                match { it.cacheKey == "song|artist|180" && it.source == "netease" && it.rawContent == lrc }
            )
        }
    }

    @Test
    fun `no download happens when online lookup is switched off`() = runTest {
        every { settingsRepository.onlineLyricsEnabled } returns flowOf(false)

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.NONE, result.source)
        assertNull(result.syncedLyrics)
        coVerify(exactly = 0) { netEase.fetch(any()) }
        coVerify(exactly = 0) { lyricsCacheDao.put(any()) }
    }

    @Test
    fun `a failing provider degrades to the next one in the list`() = runTest {
        anyProviderServes(lrc)
        coEvery { netEase.fetch(any()) } throws RuntimeException("rate limited")

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.ONLINE, result.source)
        coVerify(exactly = 1) { lrclib.fetch(any()) }
    }

    @Test
    fun `the user's provider order decides who is asked first`() = runTest {
        every { settingsRepository.lyricsProviderOrder } returns
            flowOf(listOf(LyricsProvider.LRCLIB, LyricsProvider.NETEASE))
        anyProviderServes(lrc)

        repository.getLyrics(song)

        coVerify(exactly = 1) { lrclib.fetch(any()) }
        coVerify(exactly = 0) { netEase.fetch(any()) }
    }

    @Test
    fun `lyrics the parser cannot read are rejected and remembered as a miss`() = runTest {
        anyProviderServes("no timestamps here")

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.NONE, result.source)
        coVerify { lyricsCacheDao.put(match { it.rawContent.isBlank() }) }
    }

    @Test
    fun `a failing write does not lose the lyrics`() = runTest {
        anyProviderServes(lrc)
        coEvery { lyricsCacheDao.put(any()) } throws RuntimeException("disk full")

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.ONLINE, result.source)
        assertEquals(2, result.syncedLyrics?.lines?.size)
    }

    @Test
    fun `nothing anywhere yields an empty result`() = runTest {
        coEvery { netEase.fetch(any()) } returns null
        coEvery { lrclib.fetch(any()) } returns null

        val result = repository.getLyrics(song)

        assertEquals(LyricsSource.NONE, result.source)
        assertNull(result.rawContent)
    }

    // ── choosing a version ───────────────────────────────────────────

    @Test
    fun `candidates list every version, word-by-word ones included`() = runTest {
        coEvery { netEase.fetchCandidates(any()) } returns listOf(
            OnlineLyrics(yrc, label = "Golden Best"),
            OnlineLyrics(lrc, label = "TODAY IS ANOTHER DAY")
        )
        coEvery { lrclib.fetchCandidates(any()) } returns listOf(OnlineLyrics(lrc))

        val candidates = repository.findLyricsCandidates(song)

        // LRCLIB's copy parses to the same lyrics as NetEase's line-level one, so it is one version
        // as far as the user is concerned and is collapsed away.
        assertEquals(2, candidates.size)
        assertEquals(LyricsProvider.NETEASE, candidates[0].provider)
        assertTrue(candidates[0].wordByWord)
        assertEquals("Golden Best", candidates[0].label)
        assertFalse(candidates[1].wordByWord)
        assertEquals("first line", candidates[1].preview)
    }

    @Test
    fun `the version already in use is marked as current`() = runTest {
        coEvery { lyricsCacheDao.get(any()) } returns cachedEntity(rawContent = lrc)
        coEvery { netEase.fetchCandidates(any()) } returns listOf(OnlineLyrics(yrc), OnlineLyrics(lrc))
        coEvery { lrclib.fetchCandidates(any()) } returns emptyList()

        val candidates = repository.findLyricsCandidates(song)

        assertEquals(listOf(false, true), candidates.map { it.isCurrent })
    }

    @Test
    fun `selecting a version caches it and serves it back`() = runTest {
        coEvery { netEase.fetchCandidates(any()) } returns listOf(OnlineLyrics(yrc))
        coEvery { lrclib.fetchCandidates(any()) } returns emptyList()
        val candidate = repository.findLyricsCandidates(song).single()

        val result = repository.selectLyricsCandidate(song, candidate)

        assertEquals(LyricsSource.ONLINE, result.source)
        assertTrue(result.syncedLyrics?.isWordByWord == true)
        coVerify { lyricsCacheDao.put(match { it.rawContent == yrc && it.source == "netease" }) }
    }

    @Test
    fun `no versions are offered when online lookup is switched off`() = runTest {
        every { settingsRepository.onlineLyricsEnabled } returns flowOf(false)

        assertTrue(repository.findLyricsCandidates(song).isEmpty())
        coVerify(exactly = 0) { netEase.fetchCandidates(any()) }
    }

    // ── manual refresh ───────────────────────────────────────────────

    @Test
    fun `a manual refresh drops the downloaded copy and asks the providers again`() = runTest {
        cacheHolding(cachedEntity())
        anyProviderServes(lrc)

        val result = repository.refreshLyrics(song)

        assertEquals(LyricsSource.ONLINE, result.source)
        coVerify(exactly = 1) { netEase.fetch(any()) }
    }

    @Test
    fun `a manual refresh looks again after a remembered miss`() = runTest {
        // 用户点了“重新获取歌词”多半就是因为上一次哪里都没找到。
        cacheHolding(missEntity())
        anyProviderServes(lrc)

        val result = repository.refreshLyrics(song)

        assertEquals(LyricsSource.ONLINE, result.source)
        coVerify(exactly = 1) { netEase.fetch(any()) }
    }

    @Test
    fun `a manual refresh still prefers the sidecar file to the network`() = runTest {
        cacheHolding(missEntity())
        coEvery { lyricsFileHelper.getLyricsContent(song.path) } returns lrc

        val result = repository.refreshLyrics(song)

        assertEquals(LyricsSource.FILE, result.source)
        coVerify(exactly = 0) { netEase.fetch(any()) }
    }

    private fun cachedEntity(
        rawContent: String = lrc,
        createdAt: Long = System.currentTimeMillis()
    ) = LyricsCacheEntity(
        cacheKey = "song|artist|180",
        source = "netease",
        rawContent = rawContent,
        createdAt = createdAt
    )

    private fun missEntity(createdAt: Long = System.currentTimeMillis()) = LyricsCacheEntity(
        cacheKey = "song|artist|180",
        source = "none",
        rawContent = "",
        createdAt = createdAt
    )

    /** Just past the point a line-level copy is re-checked, and well inside a settled one's week. */
    private fun aDayAgo(): Long = System.currentTimeMillis() - 24L * 60 * 60 * 1000 - 60_000
}
