package com.cycling.data.local.metadata

import com.cycling.domain.model.TagUpdate
import org.jaudiotagger.tag.FieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * jaudiotagger 的标签字段 -> 详情页模型的映射，以及「只写用户编辑的字段」的规则。
 *
 * 写文件本身要靠真实音频和写权限，这里把不依赖文件的部分钉住：字段映射、轨号/碟号的两种写法、
 * 哪些容器算有 writer。
 */
class AudioMetadataMappingTest {

    @Test
    fun `tag fields map to the detail page model`() {
        val fields = mapOf(
            FieldKey.YEAR to "2001-09-14",
            FieldKey.GENRE to "Mandopop",
            FieldKey.COMPOSER to "周杰伦",
            FieldKey.ALBUM_ARTIST to "周杰伦",
            FieldKey.TRACK to "3",
            FieldKey.TRACK_TOTAL to "10",
            FieldKey.DISC_NO to "1",
            FieldKey.RECORD_LABEL to "阿尔发唱片",
            FieldKey.COMMENT to "note",
            FieldKey.COPYRIGHT to "(c) 2001"
        )

        val metadata = audioMetadataFrom(
            extension = "flac",
            bitrate = 900,
            sampleRate = 44_100,
            channels = 2,
            variableBitrate = false,
            first = { key -> fields[key] }
        )

        assertEquals("FLAC", metadata.encodingType)
        assertEquals(900, metadata.bitrate)
        assertEquals(44_100, metadata.sampleRate)
        assertEquals(2, metadata.channels)
        assertEquals("2001", metadata.year)
        assertEquals("Mandopop", metadata.genre)
        assertEquals("周杰伦", metadata.albumArtist)
        assertEquals("3/10", metadata.trackNumber)
        assertEquals("1", metadata.discNumber)
        assertEquals("周杰伦", metadata.composer)
        assertEquals("阿尔发唱片", metadata.publisher)
        assertEquals("note", metadata.comment)
        assertEquals("(c) 2001", metadata.copyright)
    }

    @Test
    fun `a track tag that already carries the total is not doubled`() {
        val metadata = audioMetadataFrom(
            extension = "mp3",
            bitrate = 320,
            sampleRate = 44_100,
            channels = 2,
            variableBitrate = false,
            // MP4 的 trkn 与 ID3 的 TRCK 读回来就可能已经是 `5/12`。
            first = { key -> if (key == FieldKey.TRACK) "5/12" else null }
        )

        assertEquals("5/12", metadata.trackNumber)
        assertNull(metadata.discNumber)
        assertEquals("MP3 (CBR)", metadata.encodingType)
    }

    @Test
    fun `vorbis writes ordinals as number plus total`() {
        val writes = tagFieldWrites(
            TagUpdate(trackNumber = "4/12", discNumber = "2"),
            ordinalStyleFor("flac")
        ).toMap()

        assertEquals("4", writes[FieldKey.TRACK])
        assertEquals("12", writes[FieldKey.TRACK_TOTAL])
        assertEquals("2", writes[FieldKey.DISC_NO])
        // 没有总数就不该造一个空键出来。
        assertNull(writes[FieldKey.DISC_TOTAL])
    }

    @Test
    fun `id3 and mp4 keep the combined ordinal in one field`() {
        val writes = tagFieldWrites(
            TagUpdate(trackNumber = "4/12", discNumber = "2/2"),
            ordinalStyleFor("m4a")
        ).toMap()

        assertEquals("4/12", writes[FieldKey.TRACK])
        assertEquals("2/2", writes[FieldKey.DISC_NO])
        assertNull(writes[FieldKey.TRACK_TOTAL])
        assertNull(writes[FieldKey.DISC_TOTAL])
    }

    @Test
    fun `blank values are treated as untouched`() {
        val writes = tagFieldWrites(TagUpdate(title = null, artist = "   "), OrdinalStyle.SPLIT)

        assertTrue(writes.isEmpty())
    }

    @Test
    fun `text fields keep the order independent values they were given`() {
        val writes = tagFieldWrites(
            TagUpdate(title = " T ", album = "A", publisher = "P", copyright = "C"),
            OrdinalStyle.SPLIT
        ).toMap()

        assertEquals("T", writes[FieldKey.TITLE])
        assertEquals("A", writes[FieldKey.ALBUM])
        assertEquals("P", writes[FieldKey.RECORD_LABEL])
        assertEquals("C", writes[FieldKey.COPYRIGHT])
    }

    @Test
    fun `year is extracted from full dates and ordinals combine with totals`() {
        assertEquals("2003", yearFromTagDate("2003-07-31T07:00:00Z"))
        assertEquals("1999", yearFromTagDate("1999"))
        assertNull(yearFromTagDate("unknown"))
        assertNull(yearFromTagDate(null))

        assertEquals("3/12", combineOrdinal("3", "12"))
        assertEquals("3", combineOrdinal("3", null))
        assertEquals("3/12", combineOrdinal("3/12", "12"))
        assertNull(combineOrdinal(null, "12"))
    }

    @Test
    fun `channel count tolerates the header's descriptive strings`() {
        assertEquals(2, channelCountOf("2"))
        assertEquals(1, channelCountOf("1 (mono)"))
        assertEquals(6, channelCountOf(" 6"))
        assertEquals(0, channelCountOf(null))
        assertEquals(0, channelCountOf("unknown"))
    }

    @Test
    fun `bitrate is estimated from size and duration only when the header has none`() {
        // 8 MB / 180 s ≈ 355 kbps。
        assertEquals(355, estimatedBitrateKbps(8_000_000L, 180.0))
        assertEquals(0, estimatedBitrateKbps(8_000_000L, 0.0))
        assertEquals(0, estimatedBitrateKbps(0L, 180.0))
    }

    @Test
    fun `write support follows the writers of the tag library`() {
        for (extension in listOf("mp3", "flac", "ogg", "oga", "m4a", "m4b", "mp4", "wav", "aiff", "wma", "dsf")) {
            assertTrue("$extension should be writable", extension in TAG_WRITABLE_EXTENSIONS)
        }
        // DFF 只有 reader；Opus 与裸 AAC 连 jaudiotagger 都不认识，走系统解析器的只读兜底。
        assertTrue("dff" !in TAG_WRITABLE_EXTENSIONS)
        assertTrue("opus" !in TAG_WRITABLE_EXTENSIONS)
        assertTrue("aac" !in TAG_WRITABLE_EXTENSIONS)
        assertTrue("opus" in RETRIEVER_EXTENSIONS)
        assertTrue("dff" in TAG_READABLE_EXTENSIONS)
    }

    @Test
    fun `ordinal style follows the container`() {
        assertEquals(OrdinalStyle.SPLIT, ordinalStyleFor("FLAC"))
        assertEquals(OrdinalStyle.SPLIT, ordinalStyleFor("ogg"))
        assertEquals(OrdinalStyle.COMBINED, ordinalStyleFor("mp3"))
        assertEquals(OrdinalStyle.COMBINED, ordinalStyleFor("m4b"))
    }
}
