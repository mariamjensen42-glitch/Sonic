package com.cycling.data.api.cover

import com.cycling.domain.model.CoverSource
import com.cycling.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The four cover sources answer with wildly different JSON; these tests pin the shapes down so a
 * provider change is noticed without a device and without the network.
 */
class CoverProvidersTest {

    private val song = Song(
        id = 1L,
        title = "安静",
        artist = "周杰伦",
        album = "范特西",
        duration = 180_000L,
        path = "/music/1.mp3",
        albumId = 1L,
        artistId = 1L,
        dateAdded = 0L,
        dateModified = 0L,
        size = 1L,
        mimeType = "audio/mpeg"
    )

    @Test
    fun `netease detail becomes candidates with upgraded urls`() {
        val body = """
            {"songs":[
                {"id":1,"name":"安静","duration":180500,"no":2,"disc":"01",
                 "artists":[{"name":"周杰伦"}],
                 "album":{"name":"范特西","picUrl":"http://p1.music.126.net/x/1.jpg",
                          "publishTime":1270000000000,"artist":{"name":"周杰伦"}}},
                {"id":2,"name":"安静 (Live)","duration":240000,"artists":[{"name":"周杰伦"}],
                 "album":{"name":"Live","picUrl":"https://p1.music.126.net/x/2.jpg"}}
            ]}
        """.trimIndent()

        val candidates = parseNetEaseCover(body, song)

        assertEquals(2, candidates.size)
        assertEquals(CoverSource.NETEASE, candidates[0].source)
        // `http://` is upgraded: the app blocks cleartext traffic, so the URL would not download.
        assertEquals("https://p1.music.126.net/x/1.jpg", candidates[0].imageUrl)
        assertEquals("范特西", candidates[0].album)
        assertEquals("周杰伦", candidates[0].artist)
        // Metadata the detail endpoint supplies is kept, so the same request can fill the form.
        assertEquals("2010", candidates[0].year)
        assertEquals("周杰伦", candidates[0].albumArtist)
        assertEquals("2", candidates[0].trackNumber)
        assertEquals("1", candidates[0].discNumber)
        // The closer duration is the best guess; the longer live version is still listed.
        assertTrue(candidates[0].isBestMatch)
        assertEquals("Live", candidates[1].album)
    }

    @Test
    fun `qq search builds the cover url from the album mid`() {
        val body = """
            {"code":0,"data":{"song":{"list":[
                {"songname":"安静","albumname":"范特西","albummid":"000Abc","interval":181,
                 "pubtime":1270000000,"singer":[{"name":"周杰伦"}]}
            ]}}}
        """.trimIndent()

        val candidates = parseQqCover(body, song)

        assertEquals(1, candidates.size)
        assertEquals(
            "https://y.qq.com/music/photo_new/T002R500x500M000000Abc.jpg",
            candidates[0].imageUrl
        )
        assertEquals(CoverSource.QQ_MUSIC, candidates[0].source)
        assertEquals("2010", candidates[0].year)
    }

    @Test
    fun `qq parser tolerates a jsonp wrapper`() {
        val body = """callback({"code":0,"data":{"song":{"list":[{"songname":"安静","albummid":"mid1"}]}}});"""

        assertEquals(1, parseQqCover(body, song).size)
    }

    @Test
    fun `kugou capitalised keys and image template are handled`() {
        val body = """
            {"status":1,"data":{"lists":[
                {"SongName":"安静","SingerName":"周杰伦","AlbumName":"范特西","Duration":180,
                 "Image":"http://imge.kugou.com/stdmusic/{size}/20200909/x.jpg"}
            ]}}
        """.trimIndent()

        val candidates = parseKugouCover(body, song)

        assertEquals(1, candidates.size)
        assertEquals("https://imge.kugou.com/stdmusic/500/20200909/x.jpg", candidates[0].imageUrl)
        assertEquals("周杰伦", candidates[0].artist)
    }

    @Test
    fun `itunes artwork is requested at a larger size`() {
        val body = """
            {"resultCount":1,"results":[
                {"trackName":"安静","artistName":"周杰伦","collectionName":"范特西",
                 "artworkUrl100":"https://is1-ssl.mzstatic.com/image/thumb/x/100x100bb.jpg",
                 "trackTimeMillis":180000,"releaseDate":"2003-07-31T07:00:00Z",
                 "primaryGenreName":"Mandopop","trackNumber":3,"trackCount":10,
                 "discNumber":1,"discCount":2}
            ]}
        """.trimIndent()

        val candidates = parseITunesCover(body, song)

        assertEquals(1, candidates.size)
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/x/600x600bb.jpg",
            candidates[0].imageUrl
        )
        assertEquals(CoverSource.ITUNES, candidates[0].source)
        // iTunes is the richest source for the fill-in action: year, genre, track, disc and artist.
        assertEquals("周杰伦", candidates[0].albumArtist)
        assertEquals("2003", candidates[0].year)
        assertEquals("Mandopop", candidates[0].genre)
        assertEquals("3/10", candidates[0].trackNumber)
        assertEquals("1/2", candidates[0].discNumber)
    }

    @Test
    fun `unreadable bodies yield no candidates instead of throwing`() {
        assertTrue(parseNetEaseCover("<html>blocked</html>", song).isEmpty())
        assertTrue(parseQqCover("", song).isEmpty())
        assertTrue(parseKugouCover("not json", song).isEmpty())
        assertTrue(parseITunesCover("{}", song).isEmpty())
    }
}
