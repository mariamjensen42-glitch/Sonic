package com.cycling.data.api.lyrics

import com.cycling.data.api.lyrics.dto.LrclibTrackDto
import com.cycling.data.api.lyrics.dto.NetEaseLyricResponse
import com.cycling.data.api.lyrics.dto.NetEaseSearchResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decodes payloads captured from the two live endpoints.
 *
 * Worth pinning down because the failure mode is silent: an `@SerialName` or nullability mismatch
 * does not throw here, it just yields empty fields that the providers read as "no lyrics found".
 */
class LyricsDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes an LRCLIB track`() {
        val payload = """
            {
              "id": 3396226,
              "name": "Hello",
              "trackName": "Hello",
              "artistName": "Adele",
              "albumName": "25",
              "duration": 295.0,
              "instrumental": false,
              "plainLyrics": "Hello, it's me",
              "syncedLyrics": "[00:01.00] Hello, it's me"
            }
        """.trimIndent()

        val track = json.decodeFromString<LrclibTrackDto>(payload)

        assertEquals("Hello", track.trackName)
        assertEquals("Adele", track.artistName)
        assertEquals("[00:01.00] Hello, it's me", track.syncedLyrics)
        assertEquals(295_000L, track.durationMs)
    }

    @Test
    fun `LRCLIB duration is read as seconds and survives a whole number`() {
        val track = json.decodeFromString<LrclibTrackDto>("""{"duration": 180}""")

        assertEquals(180_000L, track.durationMs)
    }

    @Test
    fun `an LRCLIB entry may report no duration at all`() {
        val track = json.decodeFromString<LrclibTrackDto>("""{"trackName": "X"}""")

        assertNull(track.durationMs)
        assertNull(track.syncedLyrics)
    }

    @Test
    fun `decodes a NetEase search result`() {
        val payload = """
            {
              "result": {
                "songCount": 1,
                "songs": [
                  {
                    "id": 123456,
                    "name": "Hello",
                    "artists": [{"id": 6452, "name": "Adele"}],
                    "album": {"id": 34731264, "name": "25"},
                    "duration": 295000,
                    "fee": 0
                  }
                ]
              },
              "code": 200
            }
        """.trimIndent()

        val songs = json.decodeFromString<NetEaseSearchResponse>(payload).result?.songs.orEmpty()

        assertEquals(1, songs.size)
        assertEquals(123456L, songs[0].id)
        assertEquals("Adele", songs[0].artistNames)
        assertEquals(295_000L, songs[0].durationMs)
    }

    @Test
    fun `NetEase reports the track length as dt on newer responses`() {
        val payload = """{"result": {"songs": [{"id": 9, "name": "X", "dt": 180000}]}}"""

        val song = json.decodeFromString<NetEaseSearchResponse>(payload).result?.songs?.first()

        assertEquals(180_000L, song?.durationMs)
    }

    @Test
    fun `a NetEase error is a 200 with no result rather than a parse failure`() {
        val payload = """{"code": 406, "msg": "rate limited"}"""

        val response = json.decodeFromString<NetEaseSearchResponse>(payload)

        assertNull(response.result)
    }

    @Test
    fun `a NetEase search with no matches decodes to an empty list`() {
        val payload = """{"result": {"songCount": 0}, "code": 200}"""

        val songs = json.decodeFromString<NetEaseSearchResponse>(payload).result?.songs.orEmpty()

        assertTrue(songs.isEmpty())
    }

    @Test
    fun `decodes a NetEase lyric response`() {
        val payload = """{"sgc": false, "lrc": {"version": 1, "lyric": "[00:01.00]hi"}, "code": 200}"""

        val lyric = json.decodeFromString<NetEaseLyricResponse>(payload).lrc?.lyric

        assertEquals("[00:01.00]hi", lyric)
    }

    @Test
    fun `a NetEase track with no lyrics has no lrc body`() {
        val payload = """{"sgc": false, "code": 200}"""

        assertNull(json.decodeFromString<NetEaseLyricResponse>(payload).lrc)
    }

    @Test
    fun `a NetEase lyric body that is present but empty decodes to a blank string`() {
        val payload = """{"lrc": {"version": 1, "lyric": ""}, "tlyric": {"lyric": "ignored"}}"""

        val lyric = json.decodeFromString<NetEaseLyricResponse>(payload).lrc?.lyric

        assertEquals("", lyric)
    }
}
