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

    private fun track(
        durationSeconds: Double?,
        synced: String? = "[00:01.00]la",
        instrumental: Boolean = false,
        title: String = "Song",
        artist: String = "Artist"
    ) = LrclibTrackDto(
        trackName = title,
        artistName = artist,
        duration = durationSeconds,
        instrumental = instrumental,
        syncedLyrics = synced
    )

    private fun best(candidates: List<LrclibTrackDto>, forQuery: LyricsQuery = query) = bestMatch(
        candidates,
        forQuery,
        LrclibTrackDto::trackName,
        LrclibTrackDto::artistName,
        LrclibTrackDto::durationMs
    )

    @Test
    fun `picks the candidate closest in length, not the first one`() {
        val candidates = listOf(track(240.0), track(181.0), track(300.0))

        assertEquals(candidates[1], best(candidates))
    }

    @Test
    fun `rejects every candidate outside the length tolerance`() {
        val candidates = listOf(track(240.0), track(300.0))

        assertNull(best(candidates))
    }

    @Test
    fun `accepts a candidate of unknown length when the local song has none either`() {
        val candidates = listOf(track(null))
        val undated = query.copy(durationMs = 0)

        assertEquals(candidates[0], best(candidates, undated))
    }

    @Test
    fun `ranks candidates of unknown length last`() {
        val candidates = listOf(track(null), track(180.5))

        assertEquals(candidates[1], best(candidates))
    }

    @Test
    fun `ranks candidates by how close they are, dropping the ones too far out`() {
        val closest = track(181.0)
        val alsoClose = track(179.0)
        val farOff = track(300.0)

        val ranked = rankCandidates(
            listOf(closest, farOff, alsoClose),
            query,
            LrclibTrackDto::trackName,
            LrclibTrackDto::artistName,
            LrclibTrackDto::durationMs
        )

        assertEquals(listOf(closest, alsoClose), ranked)
    }

    // ── 曲名 / 歌手是硬门槛 ──────────────────────────────────────────

    @Test
    fun `a candidate of the right length but another title is rejected`() {
        // 这是「匹配不正确」的主因：关键词检索会把同名歌、翻唱一起返回，
        // 只要时长撞上就被当成答案。
        val anotherSong = track(180.0, title = "Another Song")

        assertNull(best(listOf(anotherSong)))
    }

    @Test
    fun `a cover of the same title by another artist is rejected`() {
        val cover = track(180.0, artist = "Someone Else")

        assertNull(best(listOf(cover)))
    }

    @Test
    fun `a missing artist tag does not veto the match`() {
        // 本地没有艺术家标签是常态，这时只能靠曲名与时长把关。
        val untagged = query.copy(artist = "")
        val candidate = track(180.0)

        assertEquals(candidate, best(listOf(candidate), untagged))
    }

    @Test
    fun `a title with a release suffix is still the same song`() {
        val remastered = track(186.0, title = "Song - Remastered 2011")

        assertEquals(remastered, best(listOf(remastered)))
    }

    @Test
    fun `a ten-second difference is accepted when the title matches exactly`() {
        // 旧规则是 ±5 秒，母带差异与片头片尾的长度差会把正确的歌词一起丢掉。
        val slightlyLonger = track(190.0)

        assertEquals(slightlyLonger, best(listOf(slightlyLonger)))
    }

    @Test
    fun `a live take is rejected when only its title base matches`() {
        // 现场版是另一次演奏：底名相同但差半分钟，不能因为"曲名像"就拿来用。
        val live = track(240.0, title = "Song (Live)")

        assertNull(best(listOf(live)))
    }

    @Test
    fun `a live take of the right length is accepted`() {
        val live = track(182.0, title = "Song (Live)")

        assertEquals(live, best(listOf(live)))
    }

    @Test
    fun `an exact title outranks one that only matches after the suffix is stripped`() {
        val acoustic = track(180.0, title = "Song (Acoustic Version)")
        val exact = track(185.0, title = "Song")

        assertEquals(listOf(exact, acoustic), rankCandidates(
            listOf(acoustic, exact),
            query,
            LrclibTrackDto::trackName,
            LrclibTrackDto::artistName,
            LrclibTrackDto::durationMs
        ))
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
        artists: List<String> = listOf("Artist"),
        title: String = "Song",
        ncmMusicIds: List<String> = emptyList(),
        qqMusicIds: List<String> = emptyList()
    ) = AmllSongItemDto(
        id = id,
        musicNames = listOf(title),
        artistNames = artists,
        albumNames = listOf(album),
        ncmMusicIds = ncmMusicIds,
        qqMusicIds = qqMusicIds
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

    // ── 本地没有歌手标签：只采信有旁证的结果 ─────────────────────────

    @Test
    fun `AMLL accepts an untagged file whose album is on the document`() {
        // 缺歌手标签时不能一刀切放弃（那类文件的 AMLL 歌词就永远取不到），但也不能随便挑：
        // 专辑同名是「同一个发行版本」的旁证。
        val untagged = query.copy(artist = "")
        val onOtherAlbum = amllSong(1, "Something Else", artists = listOf("Someone Else"))
        val onAlbum = amllSong(2, "Album", artists = listOf("Someone Else"))

        assertEquals(listOf(onAlbum), selectAmllCandidates(listOf(onOtherAlbum, onAlbum), untagged))
    }

    @Test
    fun `AMLL rejects an untagged file when several unrelated submissions share the title`() {
        // 同名曲目由不同的人唱过，没有歌手也没有专辑可对时，挑一份等于随机挑别人的歌词。
        val untagged = query.copy(artist = "")
        val candidates = listOf(
            amllSong(1, "A", artists = listOf("First")),
            amllSong(2, "B", artists = listOf("Second"))
        )

        assertTrue(selectAmllCandidates(candidates, untagged).isEmpty())
    }

    @Test
    fun `AMLL accepts the only submission the library has for an untagged file`() {
        val untagged = query.copy(artist = "")
        val only = amllSong(1, "Whatever", artists = listOf("Someone Else"))

        assertEquals(listOf(only), selectAmllCandidates(listOf(only), untagged))
    }

    @Test
    fun `AMLL prefers the submission that declares this track's platform id`() {
        // 在线曲目带平台 id，词库登记的 ncmMusicIds 与之相同就是同一条母带 —— 比专辑更硬。
        val online = query.copy(onlineSourceKey = "wy", onlineSongId = "186016")
        val wrongMaster = amllSong(1, "Single")
        val sameMaster = amllSong(2, "Compilation", ncmMusicIds = listOf("186016"))

        assertEquals(listOf(sameMaster, wrongMaster), rankAmllCandidates(listOf(wrongMaster, sameMaster), online))
    }

    @Test
    fun `AMLL ignores a platform id belonging to a different source`() {
        // `qqMusicIds` 里的 id 与网易云的 id 没有任何关系，不能混着比。
        val online = query.copy(onlineSourceKey = "wy", onlineSongId = "186016")
        val qqOnly = amllSong(1, "Album", qqMusicIds = listOf("186016"))

        assertEquals(listOf(qqOnly), rankAmllCandidates(listOf(qqOnly), online))
    }

    @Test
    fun `AMLL puts the exact title ahead of one that only matches after stripping the suffix`() {
        val live = amllSong(1, "Album", title = "Song (Live)")
        val exact = amllSong(2, "Album")

        assertEquals(listOf(exact, live), rankAmllCandidates(listOf(live, exact), query))
    }

    // ── 搜索词的变体 ────────────────────────────────────────────────

    @Test
    fun `AMLL searches the denoised title first, then the raw tag`() {
        // 词库索引的是提交者登记的写法，`Song (Live)` 这类别名只有原文才命中；
        // 先拿去噪曲名试，命中率高且省一次请求。
        val live = query.copy(title = "Song (Live) - Remastered 2011")

        assertEquals(listOf("Song", "Song (Live) - Remastered 2011"), amllSearchTerms(live))
    }

    @Test
    fun `AMLL searches once when the title has nothing to strip`() {
        assertEquals(listOf("Song"), amllSearchTerms(query))
    }
}
