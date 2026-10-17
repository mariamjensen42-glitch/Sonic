package com.cycling.data.api.lyrics

import com.cycling.data.api.lyrics.dto.LrclibTrackDto
import com.cycling.domain.model.LyricsQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsMatchTest {

    private val query = LyricsQuery(
        title = "Song",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000
    )

    private fun track(durationSeconds: Double?, synced: String? = "[00:01.00]la", instrumental: Boolean = false) =
        LrclibTrackDto(
            trackName = "Song",
            artistName = "Artist",
            duration = durationSeconds,
            instrumental = instrumental,
            syncedLyrics = synced
        )

    @Test
    fun `picks the candidate closest in length, not the first one`() {
        val candidates = listOf(track(240.0), track(181.0), track(300.0))

        val match = bestDurationMatch(candidates, query) { it.durationMs }

        assertEquals(candidates[1], match)
    }

    @Test
    fun `rejects every candidate outside the length tolerance`() {
        val candidates = listOf(track(240.0), track(300.0))

        assertNull(bestDurationMatch(candidates, query) { it.durationMs })
    }

    @Test
    fun `accepts a candidate of unknown length when the local song has none either`() {
        val candidates = listOf(track(null))
        val undated = query.copy(durationMs = 0)

        assertEquals(candidates[0], bestDurationMatch(candidates, undated) { it.durationMs })
    }

    @Test
    fun `ranks candidates of unknown length last`() {
        val candidates = listOf(track(null), track(180.5))

        assertEquals(candidates[1], bestDurationMatch(candidates, query) { it.durationMs })
    }

    @Test
    fun `ranks candidates by how close they are, dropping the ones too far out`() {
        val closest = track(181.0)
        val alsoClose = track(179.0)
        val farOff = track(300.0)

        val ranked = candidatesByDurationDistance(listOf(closest, farOff, alsoClose), query) { it.durationMs }

        assertEquals(listOf(closest, alsoClose), ranked)
    }

    @Test
    fun `selects synced lyrics from the matching candidate`() {
        val result = selectLrclibLyrics(listOf(track(180.0, synced = "[00:01.00]hello")), query)

        assertEquals("[00:01.00]hello", result?.rawContent)
    }

    @Test
    fun `ignores candidates that only have unsynced plain lyrics`() {
        val unsynced = track(180.0, synced = null)

        assertNull(selectLrclibLyrics(listOf(unsynced), query))
    }

    @Test
    fun `ignores instrumentals even when they carry timings`() {
        val instrumental = track(180.0, synced = "[00:01.00]la", instrumental = true)

        assertNull(selectLrclibLyrics(listOf(instrumental), query))
    }

    @Test
    fun `returns nothing when the provider has no candidates`() {
        assertNull(selectLrclibLyrics(emptyList(), query))
    }
}
