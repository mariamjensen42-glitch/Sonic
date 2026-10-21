package com.cycling.data.api.cover

import com.cycling.domain.model.CoverCandidate
import com.cycling.domain.model.CoverSource
import com.cycling.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverMatchTest {

    private val song = Song(
        id = 1L,
        title = "Song",
        artist = "Artist",
        album = "Album",
        duration = 180_000L,
        path = "/music/1.mp3",
        albumId = 1L,
        artistId = 1L,
        dateAdded = 0L,
        dateModified = 0L,
        size = 1L,
        mimeType = "audio/mpeg"
    )

    private data class Entry(val url: String, val durationMs: Long?)

    private fun rank(entries: List<Entry>): List<CoverCandidate> = rankCoverCandidates(
        entries = entries,
        song = song,
        durationMsOf = { it.durationMs },
        candidateOf = { entry ->
            CoverCandidate(id = entry.url, source = CoverSource.NETEASE, imageUrl = entry.url)
        }
    )

    @Test
    fun `closest duration is first and marked as the best match`() {
        val candidates = rank(
            listOf(
                Entry("far.jpg", 220_000L),
                Entry("close.jpg", 181_000L),
                Entry("closer.jpg", 179_500L)
            )
        )

        assertEquals(listOf("closer.jpg", "close.jpg", "far.jpg"), candidates.map { it.imageUrl })
        assertTrue(candidates[0].isBestMatch)
        assertFalse(candidates[1].isBestMatch)
    }

    @Test
    fun `entries beyond the tolerance are listed but not highlighted`() {
        val candidates = rank(listOf(Entry("wrong.jpg", 400_000L)))

        assertEquals(1, candidates.size)
        assertFalse(candidates[0].isBestMatch)
    }

    @Test
    fun `undated entries are ranked last and do not become the best match`() {
        val candidates = rank(listOf(Entry("undated.jpg", null), Entry("dated.jpg", 180_500L)))

        assertEquals(listOf("dated.jpg", "undated.jpg"), candidates.map { it.imageUrl })
        assertTrue(candidates[0].isBestMatch)
    }

    @Test
    fun `duplicate image urls are collapsed`() {
        val candidates = rank(
            listOf(
                Entry("same.jpg", 180_000L),
                Entry("same.jpg", 180_100L),
                Entry("other.jpg", 181_000L)
            )
        )

        assertEquals(listOf("same.jpg", "other.jpg"), candidates.map { it.imageUrl })
    }

    @Test
    fun `an empty entry list yields no candidates`() {
        assertTrue(rank(emptyList()).isEmpty())
    }
}
