package com.cycling.data.local.metadata

import com.cycling.domain.model.TagUpdate
import com.cycling.salt.audiotag.rw.data.AudioTagData
import com.cycling.salt.audiotag.rw.data.Metadata
import com.cycling.salt.audiotag.rw.data.Streaminfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SaltAudioTag 的元数据键 -> 详情页字段的映射，以及「只改用户编辑的字段、其余原样带回」的合并
 * 规则。写文件本身靠设备/真实音频，这里把不依赖文件的部分钉住。
 */
class AudioMetadataMappingTest {

    @Test
    fun `tag data maps to the detail page model`() {
        val data = AudioTagData(
            streaminfo = Streaminfo(
                sampleRate = 44_100,
                channelCount = 2,
                bits = 16,
                sampleCount = 44_100L * 180,
                fileLevelMetadataLength = 0
            ),
            metadatas = listOf(
                Metadata("TITLE", "安静"),
                Metadata("ARTIST", "周杰伦"),
                Metadata("ALBUM", "范特西"),
                Metadata("ALBUMARTIST", "周杰伦"),
                Metadata("DATE", "2001-09-14"),
                Metadata("GENRE", "Mandopop"),
                Metadata("TRACKNUMBER", "3"),
                Metadata("TRACKTOTAL", "10"),
                Metadata("DISCNUMBER", "1"),
                Metadata("COMPOSER", "周杰伦"),
                Metadata("ORGANIZATION", "阿尔发唱片"),
                Metadata("COMMENT", "note"),
                Metadata("COPYRIGHT", "(c) 2001")
            ),
            pictures = emptyList()
        )

        val metadata = audioMetadataFromTagData(data, extension = "flac", fileSize = 8_000_000L)

        assertEquals("FLAC", metadata.encodingType)
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
        // 码率是由时长和文件大小估出来的，只要有合理值即可。
        assertTrue(metadata.bitrate > 0)
    }

    @Test
    fun `mp4 disc atom mapped as DISCID is understood`() {
        val data = AudioTagData(
            streaminfo = null,
            metadatas = listOf(
                Metadata("DISCID", "2/2"),
                Metadata("TRACKNUMBER", "5/12")
            ),
            pictures = null
        )

        val metadata = audioMetadataFromTagData(data, extension = "m4a", fileSize = 1L)

        assertEquals("2/2", metadata.discNumber)
        assertEquals("5/12", metadata.trackNumber)
    }

    @Test
    fun `vorbis merge replaces edited keys and keeps everything else`() {
        val existing = listOf(
            Metadata("TITLE", "old title"),
            Metadata("ALBUMARTIST", "old album artist"),
            Metadata("REPLAYGAIN_TRACK_GAIN", "-7.53 dB"),
            Metadata("METADATA_BLOCK_PICTURE", "base64…")
        )

        val merged = mergeTagUpdate(
            existing = existing,
            update = TagUpdate(title = "new title", albumArtist = "new album artist"),
            style = TagStyle.VORBIS
        )

        assertEquals("new title", merged.first { it.key == "TITLE" }.value)
        assertEquals("new album artist", merged.first { it.key == "ALBUMARTIST" }.value)
        // 未编辑的键原样保留 —— 漏传就等于把它们从文件里删掉。
        assertEquals("-7.53 dB", merged.first { it.key == "REPLAYGAIN_TRACK_GAIN" }.value)
        assertEquals("base64…", merged.first { it.key == "METADATA_BLOCK_PICTURE" }.value)
        assertEquals(1, merged.count { it.key == "TITLE" })
        assertEquals(1, merged.count { it.key == "ALBUMARTIST" })
    }

    @Test
    fun `mp4 merge uses ilst keys and skips the publisher atom`() {
        val merged = mergeTagUpdate(
            existing = listOf(Metadata("ALBUMARTIST", "stale alias")),
            update = TagUpdate(
                albumArtist = "Various",
                publisher = "Label",
                year = "2010",
                trackNumber = "3/12"
            ),
            style = TagStyle.MP4
        )

        assertEquals("Various", merged.first { it.key == "ALBUM_ARTIST" }.value)
        assertEquals("2010", merged.first { it.key == "DATE" }.value)
        assertEquals("3/12", merged.first { it.key == "TRACKNUMBER" }.value)
        // MP4 没有发行方原子；不写，也不能写成别的字段。
        assertTrue(merged.none { it.value == "Label" })
        // 旧的 ALBUMARTIST 属于被替换的别名，不能留成两份。
        assertTrue(merged.none { it.key == "ALBUMARTIST" })
    }

    @Test
    fun `blank update values leave the existing tags untouched`() {
        val existing = listOf(Metadata("TITLE", "kept"))

        val merged = mergeTagUpdate(existing, TagUpdate(title = null, artist = "   "), TagStyle.VORBIS)

        assertEquals(listOf(Metadata("TITLE", "kept")), merged)
    }

    @Test
    fun `vorbis ordinals split into number and total and replace stale aliases`() {
        val merged = mergeTagUpdate(
            existing = listOf(
                Metadata("TRACKNUMBER", "1"),
                Metadata("TRACKTOTAL", "9"),
                Metadata("DISCID", "1")
            ),
            update = TagUpdate(trackNumber = "4/12", discNumber = "2"),
            style = TagStyle.VORBIS
        )

        assertEquals("4", merged.first { it.key == "TRACKNUMBER" }.value)
        assertEquals("12", merged.first { it.key == "TRACKTOTAL" }.value)
        assertEquals("2", merged.first { it.key == "DISCNUMBER" }.value)
        // 旧的碟号别名与总数不能残留，否则读回来会拼成 `2/9` 这类旧值。
        assertTrue(merged.none { it.key == "DISCID" })
        assertTrue(merged.none { it.key == "DISCTOTAL" })
    }

    @Test
    fun `year is extracted from full dates and ordinals combine with totals`() {
        assertEquals("2003", yearFromTagDate("2003-07-31T07:00:00Z"))
        assertEquals("1999", yearFromTagDate("1999"))
        assertNull(yearFromTagDate("unknown"))

        assertEquals("3/12", combineOrdinal("3", "12"))
        assertEquals("3", combineOrdinal("3", null))
        assertEquals("3/12", combineOrdinal("3/12", "12"))
        assertNull(combineOrdinal(null, "12"))
    }

    @Test
    fun `m4b is offered to the library as m4a and write support follows the writers`() {
        assertEquals("m4a", tagLibExtensionFor("m4b"))
        assertEquals(TagStyle.MP4, tagStyleFor("m4b"))

        assertTrue("flac" in MULTI_FORMAT_WRITE_EXTENSIONS)
        assertTrue("m4b" in MULTI_FORMAT_WRITE_EXTENSIONS)
        // Opus 只能读不能写；裸 aac(ADTS) 不是 MP4 容器；MP3 由 mp3agic 单独处理。
        assertTrue("opus" !in MULTI_FORMAT_WRITE_EXTENSIONS)
        assertTrue("aac" !in MULTI_FORMAT_WRITE_EXTENSIONS)
        assertTrue("mp3" !in MULTI_FORMAT_WRITE_EXTENSIONS)
    }
}
