package com.cycling.data.api.lyrics

import com.cycling.data.api.lyrics.dto.NetEaseLyricBody
import com.cycling.data.api.lyrics.dto.NetEaseLyricResponse
import com.cycling.domain.lyrics.model.karaoke.KaraokeLine
import com.cycling.domain.lyrics.model.synced.SyncedLine
import com.cycling.domain.lyrics.parser.AutoParser
import com.cycling.domain.lyrics.utils.LyricsFormatGuesser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The merge is where NetEase's four lyric documents become the one blob everything else reads, so
 * these pin both what goes in and — the part that fails silently — that the blob parses back.
 */
class NetEaseLyricsMergeTest {

    private val lineLevel = NetEaseLyricResponse(
        lrc = NetEaseLyricBody(
            lyric = "[00:00.000] 作词 : X\n[00:20.500]Once I was seven years old\n[00:25.040]Go make yourself some friends"
        ),
        tlyric = NetEaseLyricBody(
            // The endpoint pads its timeline with empty translations; those carry nothing to show.
            lyric = "[00:00.950]\n[00:20.500]那年我七岁\n[00:25.040]去交些朋友"
        )
    )

    private val wordByWord = NetEaseLyricResponse(
        lrc = NetEaseLyricBody(lyric = "[00:20.500]Once I was seven years old"),
        tlyric = NetEaseLyricBody(lyric = "[00:20.500]行级译文"),
        yrc = NetEaseLyricBody(lyric = "[20250,4530](20250,630,0)Once (20880,240,0)I was seven years old"),
        ytlrc = NetEaseLyricBody(lyric = "[00:20.250]逐字译文")
    )

    @Test
    fun `emits each line-level translation under the line it translates`() {
        val merged = mergeNetEaseLyrics(lineLevel)

        assertEquals(
            """
            [00:00.000] 作词 : X
            [00:20.500]Once I was seven years old
            [00:20.500]那年我七岁
            [00:25.040]Go make yourself some friends
            [00:25.040]去交些朋友
            """.trimIndent(),
            merged
        )
    }

    @Test
    fun `a track with no translation keeps its lyric untouched`() {
        val untranslated = NetEaseLyricResponse(
            lrc = NetEaseLyricBody(lyric = "[00:01.00]hi"),
            tlyric = NetEaseLyricBody(lyric = "[00:01.00]")
        )

        assertEquals("[00:01.00]hi", mergeNetEaseLyrics(untranslated))
    }

    @Test
    fun `prefers word-by-word lyrics and their own translation`() {
        val merged = mergeNetEaseLyrics(wordByWord)

        assertEquals(
            """
            [20250,4530](20250,630,0)Once (20880,240,0)I was seven years old
            [20250,4530]逐字译文
            """.trimIndent(),
            merged
        )
    }

    @Test
    fun `a track with no lyric documents at all merges to nothing`() {
        assertNull(mergeNetEaseLyrics(NetEaseLyricResponse()))
        assertNull(mergeNetEaseLyrics(NetEaseLyricResponse(lrc = NetEaseLyricBody(lyric = ""))))
    }

    @Test
    fun `reports whether the endpoint sent a word-by-word document`() {
        // The provider walks the track's other entries while this stays false, so a `yrc` that is
        // present but empty has to read as "none" — an absent document and an empty one look the
        // same from here, and neither is worth showing.
        assertTrue(wordByWord.hasWordByWord())
        assertFalse(lineLevel.hasWordByWord())
        assertFalse(NetEaseLyricResponse(yrc = NetEaseLyricBody(lyric = "")).hasWordByWord())
    }

    @Test
    fun `the merged word-by-word blob parses back into karaoke lines`() {
        val merged = mergeNetEaseLyrics(wordByWord)!!

        val line = AutoParser(LyricsFormatGuesser()).parse(merged).lines.single()

        assertTrue(line is KaraokeLine)
        assertEquals(20250, line.start)
        assertEquals("逐字译文", (line as KaraokeLine).translation)
    }

    @Test
    fun `the merged line-level blob parses back into translated synced lines`() {
        val merged = mergeNetEaseLyrics(lineLevel)!!

        val lines = AutoParser(LyricsFormatGuesser()).parse(merged).lines

        assertTrue(lines.all { it is SyncedLine })
        assertEquals(2, lines.count { (it as SyncedLine).translation != null })
    }

    /**
     * Captured from `id=405599119`. Worth pinning because the two translations are timed against
     * different originals — `tlyric` against `lrc`, `ytlrc` against `yrc`, 250 ms apart — so a merge
     * that reaches for the wrong one silently loses every translation instead of failing.
     */
    private val capturedWordByWord = NetEaseLyricResponse(
        lrc = NetEaseLyricBody(
            lyric = "[00:20.500]Once I was seven years old my momma told me\n" +
                "[00:25.040]Go make yourself some friends or you'll be lonely"
        ),
        tlyric = NetEaseLyricBody(
            lyric = "[00:20.500]那年我七岁 妈妈就对我说\n[00:25.040]去交些朋友 不然你会孤独寂寞"
        ),
        yrc = NetEaseLyricBody(
            lyric = "[20250,4530](20250,630,0)Once (20880,240,0)I (21120,180,0)was (21300,660,0)seven " +
                "(21960,480,0)years (22440,420,0)old (22860,360,0)my (23220,600,0)momma " +
                "(23820,540,0)told (24360,420,0)me\n" +
                "[24780,3480](24780,480,0)Go (25260,420,0)make (25680,450,0)yourself " +
                "(26130,240,0)some (26370,360,0)friends (26730,150,0)or (26880,270,0)you'll " +
                "(27150,150,0)be (27300,960,0)lonely"
        ),
        ytlrc = NetEaseLyricBody(
            lyric = "[00:20.250]那年我七岁 妈妈就对我说\n[00:24.780]去交些朋友 不然你会孤独寂寞"
        )
    )

    @Test
    fun `pairs each word-by-word line with the translation timed against it`() {
        val merged = mergeNetEaseLyrics(capturedWordByWord)!!

        val lines = AutoParser(LyricsFormatGuesser()).parse(merged).lines

        assertTrue(lines.all { it is KaraokeLine })
        val karaoke = lines.map { it as KaraokeLine }
        assertEquals(2, karaoke.size)
        assertEquals("Once I was seven years old my momma told me", karaoke[0].syllables.joinToString("") { it.content })
        assertEquals("那年我七岁 妈妈就对我说", karaoke[0].translation)
        assertEquals("去交些朋友 不然你会孤独寂寞", karaoke[1].translation)
        assertEquals(20250, karaoke[0].start)
        assertEquals(24780, karaoke[1].start)
    }
}
