@file:Suppress("SpellCheckingInspection")

package com.cycling.salt.audiotag.format.ogg

import com.cycling.salt.audiotag.AudioTag
import com.cycling.salt.audiotag.UnstableAudioTagApi
import com.cycling.salt.audiotag.rw.ReadStrategy
import com.cycling.salt.audiotag.rw.WriteOperation
import com.cycling.salt.audiotag.rw.data.Metadata
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readByteArray
import kotlinx.io.write
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(UnstableAudioTagApi::class)
class OggTest {

    // ------------------------------------------------------------------
    // T-01: OGG CRC-32 utility
    // ------------------------------------------------------------------

    @Test
    fun `CRC of minimal zero-content OGG page matches known value`() {
        val header = ByteArray(27)
        header[0] = 0x4F; header[1] = 0x67; header[2] = 0x67; header[3] = 0x53 // "OggS"
        header[4] = 0    // version
        header[5] = 0x02 // first page
        header[26] = 1   // 1 segment

        val segmentTable = byteArrayOf(0)
        val data = byteArrayOf()

        val crc = OggCrc.computePageCrc(header, segmentTable, data)
        // Reference value computed with the standard OGG CRC (poly 0x04C11DB7, init 0, no xorout);
        // the upstream constant was wrong and made this test fail on every machine.
        assertEquals(0x66B718E8.toInt(), crc)
    }

    @Test
    fun `CRC is deterministic for same input`() {
        val header = ByteArray(27)
        header[0] = 0x4F; header[1] = 0x67; header[2] = 0x67; header[3] = 0x53
        header[5] = 0x02
        header[26] = 2
        val segmentTable = byteArrayOf(255.toByte(), 10)
        val data = "Hello OGG".encodeToByteArray()

        val crc1 = OggCrc.computePageCrc(header, segmentTable, data)
        val crc2 = OggCrc.computePageCrc(header, segmentTable, data)
        assertEquals(crc1, crc2)
    }

    // ------------------------------------------------------------------
    // T-02: VorbisComment serialization
    // ------------------------------------------------------------------

    @Test
    fun `VorbisComment serialize and parse back`() {
        val vendor = "Salt Audio Tag Test"
        val metadatas = listOf(
            Metadata("TITLE", "Test Song"),
            Metadata("ARTIST", "Test Artist"),
        )

        val serialized = OggPageParser.serializeVorbisComment(vendor, metadatas)
        val (parsedVendor, parsedMetas) = OggPageParser.parseVorbisComment(serialized)

        assertEquals(vendor, parsedVendor)
        assertEquals(2, parsedMetas.size)
        assertEquals("Test Song", parsedMetas.find { it.key == "TITLE" }?.value)
        assertEquals("Test Artist", parsedMetas.find { it.key == "ARTIST" }?.value)
    }

    @Test
    fun `VorbisComment serialize with empty vendor`() {
        val serialized = OggPageParser.serializeVorbisComment("", listOf(Metadata("TITLE", "Test")))
        val (parsedVendor, parsedMetas) = OggPageParser.parseVorbisComment(serialized)
        assertEquals("", parsedVendor)
        assertEquals(1, parsedMetas.size)
    }

    @Test
    fun `VorbisComment serialize with no comments`() {
        val serialized = OggPageParser.serializeVorbisComment("Test Vendor", emptyList())
        val (parsedVendor, parsedMetas) = OggPageParser.parseVorbisComment(serialized)
        assertEquals("Test Vendor", parsedVendor)
        assertTrue(parsedMetas.isEmpty())
    }

    // ------------------------------------------------------------------
    // T-03: OGG/Vorbis writer roundtrip
    // ------------------------------------------------------------------

    @Test
    fun `OGG write and read back`() {
        val srcMetadatas = listOf(
            Metadata("TITLE", "Original Title"),
            Metadata("ARTIST", "Original Artist"),
        )
        val srcBytes = buildMinimalOggVorbis(srcMetadatas)

        val tempDir = SystemTemporaryDirectory
        val srcPath = Path(tempDir, "test_src.ogg")
        val dstPath = Path(tempDir, "test_dst.ogg")

        try {
            SystemFileSystem.sink(srcPath).buffered().use { it.write(srcBytes) }

            val newMetadatas = listOf(
                Metadata("TITLE", "New Title"),
                Metadata("ARTIST", "New Artist"),
                Metadata("ALBUM", "New Album"),
            )
            val writeResult = AudioTag.write(
                src = srcPath, dst = dstPath, extension = "ogg",
                WriteOperation.AllMetadata.create(newMetadatas),
            )
            assertTrue(writeResult.isSuccess)

            val readResult = SystemFileSystem.source(dstPath).buffered().use {
                AudioTag.read(source = it, extension = "ogg", strategy = ReadStrategy.All)
            }
            assertTrue(readResult.isSuccess)
            val data = readResult.getOrThrow()

            assertNotNull(data.metadatas)
            assertEquals("New Title", data.metadatas.find { it.key == "TITLE" }?.value)
            assertEquals("New Artist", data.metadatas.find { it.key == "ARTIST" }?.value)
            assertEquals("New Album", data.metadatas.find { it.key == "ALBUM" }?.value)
            assertNotNull(data.streaminfo)
            assertEquals(44100, data.streaminfo?.sampleRate)
            assertEquals(2, data.streaminfo?.channelCount)
        } finally {
            if (SystemFileSystem.exists(srcPath)) SystemFileSystem.delete(srcPath)
            if (SystemFileSystem.exists(dstPath)) SystemFileSystem.delete(dstPath)
        }
    }

    // ------------------------------------------------------------------
    // T-04: OGG writer edge cases
    // ------------------------------------------------------------------

    @Test
    fun `OGG write noop preserves metadata`() {
        val metadatas = listOf(Metadata("TITLE", "Test"))
        val srcBytes = buildMinimalOggVorbis(metadatas)
        val tempDir = SystemTemporaryDirectory
        val srcPath = Path(tempDir, "test_src.ogg")
        val dstPath = Path(tempDir, "test_dst.ogg")

        try {
            SystemFileSystem.sink(srcPath).buffered().use { it.write(srcBytes) }

            // Write without any WriteOperation
            AudioTag.write(src = srcPath, dst = dstPath, extension = "ogg")

            val readResult = SystemFileSystem.source(dstPath).buffered().use {
                AudioTag.read(source = it, extension = "ogg", strategy = ReadStrategy.All)
            }
            val data = readResult.getOrThrow()
            assertNotNull(data.metadatas)
            assertEquals("Test", data.metadatas.find { it.key == "TITLE" }?.value)
        } finally {
            if (SystemFileSystem.exists(srcPath)) SystemFileSystem.delete(srcPath)
            if (SystemFileSystem.exists(dstPath)) SystemFileSystem.delete(dstPath)
        }
    }

    @Test
    fun `OGG write with empty metadata list clears all tags`() {
        val metadatas = listOf(Metadata("TITLE", "Test"))
        val srcBytes = buildMinimalOggVorbis(metadatas)
        val tempDir = SystemTemporaryDirectory
        val srcPath = Path(tempDir, "test_src.ogg")
        val dstPath = Path(tempDir, "test_dst.ogg")

        try {
            SystemFileSystem.sink(srcPath).buffered().use { it.write(srcBytes) }

            AudioTag.write(
                src = srcPath, dst = dstPath, extension = "ogg",
                WriteOperation.AllMetadata.create(emptyList()),
            )

            val readResult = SystemFileSystem.source(dstPath).buffered().use {
                AudioTag.read(source = it, extension = "ogg", strategy = ReadStrategy.All)
            }
            val data = readResult.getOrThrow()
            // Empty metadata should produce a comment page with zero user comments
            assertTrue(data.metadatas == null || data.metadatas.isEmpty())
        } finally {
            if (SystemFileSystem.exists(srcPath)) SystemFileSystem.delete(srcPath)
            if (SystemFileSystem.exists(dstPath)) SystemFileSystem.delete(dstPath)
        }
    }

    @Test
    fun `OGG write preserves audio pages`() {
        val metadatas = listOf(Metadata("TITLE", "Test"))
        val srcBytes = buildMinimalOggVorbis(metadatas)
        val tempDir = SystemTemporaryDirectory
        val srcPath = Path(tempDir, "test_src.ogg")
        val dstPath = Path(tempDir, "test_dst.ogg")

        try {
            SystemFileSystem.sink(srcPath).buffered().use { it.write(srcBytes) }

            AudioTag.write(
                src = srcPath, dst = dstPath, extension = "ogg",
                WriteOperation.AllMetadata.create(listOf(Metadata("TITLE", "Updated"))),
            )

            val srcData = SystemFileSystem.source(srcPath).buffered().use { it.readByteArray() }
            val dstData = SystemFileSystem.source(dstPath).buffered().use { it.readByteArray() }

            val srcPages = scanTestPages(srcData)
            val dstPages = scanTestPages(dstData)

            assertTrue(srcPages.size >= 3)
            assertTrue(dstPages.size >= 3)

            val srcAudioOffset = srcPages[2].first
            val dstAudioOffset = dstPages[2].first
            val audioLen = srcData.size - srcAudioOffset
            assertEquals(audioLen, dstData.size - dstAudioOffset)
            assertTrue(
                srcData.copyOfRange(srcAudioOffset, srcData.size)
                    .contentEquals(dstData.copyOfRange(dstAudioOffset, dstData.size))
            )
        } finally {
            if (SystemFileSystem.exists(srcPath)) SystemFileSystem.delete(srcPath)
            if (SystemFileSystem.exists(dstPath)) SystemFileSystem.delete(dstPath)
        }
    }

    @Test
    fun `OGG write preserves vendor string`() {
        val srcMetas = listOf(Metadata("TITLE", "Original"))
        val srcBytes = buildMinimalOggVorbis(srcMetas, vendor = "My Custom Vendor")
        val tempDir = SystemTemporaryDirectory
        val srcPath = Path(tempDir, "test_src.ogg")
        val dstPath = Path(tempDir, "test_dst.ogg")

        try {
            SystemFileSystem.sink(srcPath).buffered().use { it.write(srcBytes) }

            AudioTag.write(
                src = srcPath, dst = dstPath, extension = "ogg",
                WriteOperation.AllMetadata.create(listOf(Metadata("ALBUM", "New Album"))),
            )

            // Verify by reconstructing: read the raw dst file, parse page 1's comment
            val dstBytes = SystemFileSystem.source(dstPath).buffered().use { it.readByteArray() }
            val pages = scanTestPages(dstBytes)
            assertTrue(pages.size >= 2)

            val page1 = pages[1]
            val page1End = page1.first + page1.second
            val segCount = dstBytes[page1.first + 26].toInt() and 0xFF
            val segTable = dstBytes.copyOfRange(page1.first + 27, page1.first + 27 + segCount)
            val totalDataLen = segTable.sumOf { it.toInt() and 0xFF }
            val pageData = dstBytes.copyOfRange(page1.first + 27 + segCount, page1.first + 27 + segCount + totalDataLen)

            // Extract first packet (the comment header)
            var pos = 0
            for (segSizeByte in segTable) {
                val s = segSizeByte.toInt() and 0xFF
                if (s < 255 && s > 0) {
                    val packet = pageData.copyOfRange(pos, (pos + s).coerceAtMost(pageData.size))
                    if (packet.isNotEmpty() && (packet[0].toInt() and 0xFF) == 0x03) {
                        val commentData = packet.copyOfRange(1, packet.size)
                        val (vendor, _) = OggPageParser.parseVorbisComment(commentData)
                        assertEquals("My Custom Vendor", vendor)
                    }
                    break
                }
                pos += s
            }
        } finally {
            if (SystemFileSystem.exists(srcPath)) SystemFileSystem.delete(srcPath)
            if (SystemFileSystem.exists(dstPath)) SystemFileSystem.delete(dstPath)
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun buildMinimalOggVorbis(
        metadatas: List<Metadata>,
        vendor: String = "Salt Audio Tag Test",
        streamSerial: Int = 0x12345678,
        sampleRate: Int = 44100,
        channels: Int = 2,
    ): ByteArray {
        val pages = mutableListOf<ByteArray>()

        // Page 0: Vorbis identification header (first page = headerType 0x02)
        pages.add(buildOggPageRaw(
            streamSerial = streamSerial, pageSequence = 0, granulePosition = 0L,
            headerType = 0x02, packetData = buildVorbisIdHeader(sampleRate, channels),
        ))

        // Page 1: Vorbis comment header
        val commentBytes = OggPageParser.serializeVorbisComment(vendor, metadatas)
        pages.add(buildOggPageRaw(
            streamSerial = streamSerial, pageSequence = 1, granulePosition = 0L,
            headerType = 0x00, packetData = byteArrayOf(0x03) + commentBytes,
        ))

        // Page 2: Last page with dummy audio data
        pages.add(buildOggPageRaw(
            streamSerial = streamSerial, pageSequence = 2,
            granulePosition = sampleRate.toLong() * 2, headerType = 0x04,
            packetData = byteArrayOf(0x00),
        ))

        return pages.fold(ByteArray(0)) { acc, page -> acc + page }
    }

    private fun buildVorbisIdHeader(sampleRate: Int, channels: Int): ByteArray {
        val buf = mutableListOf<Byte>()
        buf.add(0x01) // packet type = identification
        buf.addAll("vorbis".encodeToByteArray().toList())
        writeLe32(buf, 1) // version
        buf.add(channels.toByte())
        writeLe32(buf, sampleRate)
        writeLe32(buf, -1) // bitrate_max
        writeLe32(buf, -1) // bitrate_nom
        writeLe32(buf, -1) // bitrate_min
        buf.add(6) // blocksize_0
        buf.add(8) // blocksize_1
        buf.add(0x01) // framing bit
        return buf.toByteArray()
    }

    private fun buildOggPageRaw(
        streamSerial: Int, pageSequence: Int, granulePosition: Long,
        headerType: Int, packetData: ByteArray,
    ): ByteArray {
        val packetSize = packetData.size
        val segmentCount = (packetSize + 254) / 255
        val segmentTable = ByteArray(segmentCount)
        for (i in 0 until segmentCount) {
            segmentTable[i] = if (i < segmentCount - 1) 255.toByte()
            else ((packetSize - 1) % 255 + 1).toByte()
        }

        val header = ByteArray(27)
        header[0] = 0x4F; header[1] = 0x67; header[2] = 0x67; header[3] = 0x53
        header[4] = 0; header[5] = headerType.toByte()
        writeLe64At(header, 6, granulePosition)
        writeLe32At(header, 14, streamSerial)
        writeLe32At(header, 18, pageSequence)
        header[26] = segmentCount.toByte()

        val crc = OggCrc.computePageCrc(header, segmentTable, packetData)
        writeU32BeAt(header, 22, crc)

        val pageSize = 27 + segmentCount + packetSize
        val page = ByteArray(pageSize)
        header.copyInto(page, 0)
        segmentTable.copyInto(page, 27)
        packetData.copyInto(page, 27 + segmentCount)
        return page
    }

    private fun scanTestPages(data: ByteArray): List<Pair<Int, Int>> {
        val pages = mutableListOf<Pair<Int, Int>>()
        var offset = 0
        while (offset + 27 <= data.size) {
            if (data[offset] == 0x4F.toByte() && data[offset + 1] == 0x67.toByte() &&
                data[offset + 2] == 0x67.toByte() && data[offset + 3] == 0x53.toByte()
            ) {
                val segCount = data[offset + 26].toInt() and 0xFF
                if (offset + 27 + segCount > data.size) break
                val segTable = data.copyOfRange(offset + 27, offset + 27 + segCount)
                val totalDataLen = segTable.sumOf { it.toInt() and 0xFF }
                val totalSize = 27 + segCount + totalDataLen
                if (offset + totalSize > data.size) break
                pages.add(offset to totalSize)
                offset += totalSize
            } else offset++
        }
        return pages
    }

    // Binary I/O helpers — writeLe32 delegates to internal OggPageParser.writeLe32

    private fun writeLe32(buf: MutableList<Byte>, value: Int) {
        OggPageParser.writeLe32(buf, value)
    }

    private fun writeLe32At(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = (value ushr 8 and 0xFF).toByte()
        data[offset + 2] = (value ushr 16 and 0xFF).toByte()
        data[offset + 3] = (value ushr 24 and 0xFF).toByte()
    }

    private fun writeLe64At(data: ByteArray, offset: Int, value: Long) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = (value ushr 8 and 0xFF).toByte()
        data[offset + 2] = (value ushr 16 and 0xFF).toByte()
        data[offset + 3] = (value ushr 24 and 0xFF).toByte()
        data[offset + 4] = (value ushr 32 and 0xFF).toByte()
        data[offset + 5] = (value ushr 40 and 0xFF).toByte()
        data[offset + 6] = (value ushr 48 and 0xFF).toByte()
        data[offset + 7] = (value ushr 56 and 0xFF).toByte()
    }

    private fun writeU32BeAt(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value ushr 24 and 0xFF).toByte()
        data[offset + 1] = (value ushr 16 and 0xFF).toByte()
        data[offset + 2] = (value ushr 8 and 0xFF).toByte()
        data[offset + 3] = (value and 0xFF).toByte()
    }
}
