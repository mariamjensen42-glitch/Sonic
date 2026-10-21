package com.cycling.data.api.lyrics

import com.cycling.data.api.lyrics.dto.AmllSongItemDto
import com.cycling.data.api.lyrics.dto.LrclibTrackDto
import com.cycling.domain.model.LyricsQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    private fun amllSong(
        id: Long,
        album: String,
        artists: List<String> = listOf("Artist")
    ) = AmllSongItemDto(
        id = id,
        musicNames = listOf("Song"),
        artistNames = artists,
        albumNames = listOf(album)
    )

    @Test
    fun `AMLL keeps only submissions that name the same artist`() {
        val sameArtist = amllSong(1, "Album")
        val someoneElse = amllSong(2, "Album", artists = listOf("Someone Else"))

        assertEquals(
            listOf(sameArtist),
            selectAmllCandidates(listOf(sameArtist, someoneElse), query)
        )
    }

    @Test
    fun `AMLL matches a collaboration split across tags and names`() {
        // 本地标签常写 "A feat. B"，而词库里 artistNames 是 ["A", "B"] —— 两边都要能对上。
        val candidate = amllSong(1, "Album", artists = listOf("Brendon Urie", "Taylor Swift"))
        val duet = query.copy(artist = "Taylor Swift feat. Brendon Urie")

        assertEquals(listOf(candidate), selectAmllCandidates(listOf(candidate), duet))
    }

    @Test
    fun `AMLL ignores a title match whose artist is unrelated`() {
        // 服务端只按曲名搜，所以同名不同歌手的结果必须在这里被挡掉 —— 宁可没有歌词，
        // 也不能把别人的歌词显示出来。
        val wrongArtist = amllSong(1, "Album", artists = listOf("Different Artist"))

        assertTrue(selectAmllCandidates(listOf(wrongArtist), query).isEmpty())
    }

    @Test
    fun `AMLL still prefers the submission from the local album after filtering`() {
        val live = amllSong(1, "Song (Live)")
        val studio = amllSong(2, "Album")

        assertEquals(listOf(studio, live), selectAmllCandidates(listOf(live, studio), query))
    }

    @Test
    fun `ranks the AMLL submission timed to the same album first`() {
        val live = amllSong(1, "Song (Live)")
        val studio = amllSong(2, "Album")

        assertEquals(listOf(studio, live), rankAmllCandidates(listOf(live, studio), query))
    }

    @Test
    fun `keeps AMLL's own order when no album matches`() {
        val single = amllSong(1, "Single")
        val compilation = amllSong(2, "Compilation")

        assertEquals(listOf(single, compilation), rankAmllCandidates(listOf(single, compilation), query))
    }

    @Test
    fun `an AMLL album that only contains the local one still outranks no signal`() {
        val deluxe = amllSong(1, "Album (Deluxe Edition)")
        val unrelated = amllSong(2, "Something Else")

        assertEquals(listOf(deluxe, unrelated), rankAmllCandidates(listOf(unrelated, deluxe), query))
    }

    @Test
    fun `AMLL ranking never drops a candidate`() {
        val candidates = listOf(amllSong(1, "A"), amllSong(2, "B"), amllSong(3, "Album"))

        assertEquals(candidates.size, rankAmllCandidates(candidates, query).size)
    }
}
