@file:Suppress("SpellCheckingInspection")

package com.cycling.salt.audiotag.format.mp4

import com.cycling.salt.audiotag.rw.data.Metadata
import com.cycling.salt.audiotag.rw.data.Picture
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Mp4WriteTest {

    // ------------------------------------------------------------------
    // Atom encoding tests (existing)
    // ------------------------------------------------------------------

    @Test
    fun testEncodeAtomNameTitle() {
        val encoded = Mp4Constants.encodeAtomName("TITLE")
        assertContentEquals(b(0xA9, 0x6E, 0x61, 0x6D), encoded)
    }

    @Test
    fun testEncodeAtomNameArtist() {
        val encoded = Mp4Constants.encodeAtomName("ARTIST")
        assertContentEquals(b(0xA9, 0x41, 0x52, 0x54), encoded)
    }

    @Test
    fun testEncodeAtomNameAlbum() {
        val encoded = Mp4Constants.encodeAtomName("ALBUM")
        assertContentEquals(b(0xA9, 0x61, 0x6C, 0x62), encoded)
    }

    @Test
    fun testEncodeAtomNameTrackNumber() {
        val encoded = Mp4Constants.encodeAtomName("TRACKNUMBER")
        assertContentEquals(b(0x74, 0x72, 0x6B, 0x6E), encoded)
    }

    @Test
    fun testEncodeAtomNameCompilation() {
        val encoded = Mp4Constants.encodeAtomName("COMPILATION")
        assertContentEquals(b(0x63, 0x70, 0x69, 0x6C), encoded)
    }

    @Test
    fun testBuildBox() {
        val content = b(0x01, 0x02, 0x03, 0x04)
        val box = Mp4Constants.buildBox("test", content)
        assertTrue(box.size == 12)
        assertContentEquals("test".encodeToByteArray(), box.copyOfRange(4, 8))
        assertContentEquals(content, box.copyOfRange(8, box.size))
    }

    @Test
    fun testBuildIlstAtomSingleField() {
        val ilst = Mp4Constants.buildIlstAtom(listOf(Metadata("TITLE", "Test Song")))
        assertContentEquals("ilst".encodeToByteArray(), ilst.copyOfRange(4, 8))
        assertTrue(ilstContains(ilst, b(0xA9, 0x6E, 0x61, 0x6D)))
        assertTrue(ilstContains(ilst, "data".encodeToByteArray()))
    }

    @Test
    fun testBuildIlstAtomMultipleFields() {
        val ilst = Mp4Constants.buildIlstAtom(listOf(
            Metadata("TITLE", "Test"),
            Metadata("ARTIST", "Artist"),
            Metadata("ALBUM", "Album"),
        ))
        assertTrue(ilstContains(ilst, "ilst".encodeToByteArray()))
        assertTrue(ilstContains(ilst, b(0xA9, 0x6E, 0x61, 0x6D)))
        assertTrue(ilstContains(ilst, b(0xA9, 0x41, 0x52, 0x54)))
        assertTrue(ilstContains(ilst, b(0xA9, 0x61, 0x6C, 0x62)))
    }

    // ------------------------------------------------------------------
    // T-05: Cover art atom building
    // ------------------------------------------------------------------

    @Test
    fun `buildCovrAtom for JPEG`() {
        val jpegData = b(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10)
        val pic = Picture(
            pictureType = Picture.PictureType.FrontCover,
            mediaType = "image/jpeg",
            description = "",
            width = 0, height = 0, colorDepth = 0, colorsNumber = 0,
            pictureData = jpegData,
            dataLength = jpegData.size,
        )
        val covr = Mp4Constants.buildCovrAtom(pic)

        assertContentEquals("covr".encodeToByteArray(), covr.copyOfRange(4, 8))
        assertTrue(ilstContains(covr, "data".encodeToByteArray()))
        assertTrue(ilstContains(covr, jpegData))

        val dataPos = findSubAtom(covr, "data")
        assertTrue(dataPos >= 0)
        val typeIndex = readU32BE(covr, dataPos + 8)
        assertEquals(13L, typeIndex)
    }

    @Test
    fun `buildCovrAtom for PNG`() {
        val pngData = b(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val pic = Picture(
            pictureType = Picture.PictureType.FrontCover,
            mediaType = "image/png",
            description = "",
            width = 0, height = 0, colorDepth = 0, colorsNumber = 0,
            pictureData = pngData,
            dataLength = pngData.size,
        )
        val covr = Mp4Constants.buildCovrAtom(pic)

        assertContentEquals("covr".encodeToByteArray(), covr.copyOfRange(4, 8))
        val dataPos = findSubAtom(covr, "data")
        assertTrue(dataPos >= 0)
        val typeIndex = readU32BE(covr, dataPos + 8)
        assertEquals(14L, typeIndex, "PNG should use typeIndex 14")
    }

    @Test
    fun `buildIlstAtom with pictures includes covr`() {
        val jpegData = b(0xFF, 0xD8, 0xFF)
        val pic = Picture(
            pictureType = Picture.PictureType.FrontCover,
            mediaType = "image/jpeg", description = "",
            width = 0, height = 0, colorDepth = 0, colorsNumber = 0,
            pictureData = jpegData, dataLength = jpegData.size,
        )
        val ilst = Mp4Constants.buildIlstAtom(
            metadatas = listOf(Metadata("TITLE", "Test")),
            pictures = listOf(pic),
        )

        assertTrue(ilstContains(ilst, "ilst".encodeToByteArray()))
        assertTrue(ilstContains(ilst, b(0xA9, 0x6E, 0x61, 0x6D)))
        assertTrue(ilstContains(ilst, "covr".encodeToByteArray()))
        assertTrue(ilstContains(ilst, jpegData))
    }

    @Test
    fun `buildIlstAtom with null pictures still works`() {
        val ilst = Mp4Constants.buildIlstAtom(
            metadatas = listOf(Metadata("TITLE", "Test")),
            pictures = null,
        )
        assertTrue(ilstContains(ilst, "ilst".encodeToByteArray()))
        assertTrue(ilstContains(ilst, b(0xA9, 0x6E, 0x61, 0x6D)))
        assertTrue(!ilstContains(ilst, "covr".encodeToByteArray()))
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Build byte array from int literals. */
    private fun b(vararg ints: Int): ByteArray = ByteArray(ints.size) { i -> ints[i].toByte() }

    /** Check if [data] contains [sub] as a contiguous subarray. */
    private fun ilstContains(data: ByteArray, sub: ByteArray): Boolean {
        if (sub.size > data.size) return false
        for (start in 0..data.size - sub.size) {
            var match = true
            for (i in sub.indices) {
                if (data[start + i] != sub[i]) { match = false; break }
            }
            if (match) return true
        }
        return false
    }

    private fun findSubAtom(data: ByteArray, type: String): Int {
        val typeBytes = type.encodeToByteArray()
        var off = 8
        while (off + 8 <= data.size) {
            val sz = readU32BE(data, off).toInt()
            if (sz < 8) break
            if (data.copyOfRange(off + 4, off + 8).contentEquals(typeBytes)) return off
            off += sz
        }
        return -1
    }

    private fun readU32BE(data: ByteArray, off: Int): Long =
        ((data[off].toLong() and 0xFF) shl 24) or
        ((data[off + 1].toLong() and 0xFF) shl 16) or
        ((data[off + 2].toLong() and 0xFF) shl 8) or
        (data[off + 3].toLong() and 0xFF)
}
