package com.cycling.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LyricsQueryTest {

    @Test
    fun `cache key normalises case and padding`() {
        val query = LyricsQuery(title = "  Hello World ", artist = "ADELE", album = "X", durationMs = 180_000)

        assertEquals("hello world|adele|180", query.cacheKey)
    }

    @Test
    fun `cache key ignores sub-second duration differences`() {
        val first = LyricsQuery("Song", "Artist", "Album", durationMs = 180_400)
        val second = LyricsQuery("Song", "Artist", "Album", durationMs = 180_900)

        assertEquals(first.cacheKey, second.cacheKey)
    }

    @Test
    fun `cache key separates tracks that differ only by length`() {
        val studio = LyricsQuery("Song", "Artist", "Album", durationMs = 180_000)
        val extended = LyricsQuery("Song", "Artist", "Album", durationMs = 420_000)

        assertNotEquals(studio.cacheKey, extended.cacheKey)
    }

    @Test
    fun `of copies the identifying fields from the song`() {
        val song = SongFixture.create(title = "Title", artist = "Artist", album = "Album", duration = 245_000L)

        val query = LyricsQuery.of(song)

        assertEquals(LyricsQuery("Title", "Artist", "Album", 245_000L), query)
    }
}
