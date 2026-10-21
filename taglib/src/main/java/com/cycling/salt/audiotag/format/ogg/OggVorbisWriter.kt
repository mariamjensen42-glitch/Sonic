@file:Suppress("SpellCheckingInspection")

package com.cycling.salt.audiotag.format.ogg

import com.cycling.salt.audiotag.UnstableAudioTagApi
import com.cycling.salt.audiotag.rw.WriteOperation
import com.cycling.salt.audiotag.rw.Writer
import com.cycling.salt.audiotag.util.SystemFileSystemUtil
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.io.write
import kotlin.uuid.ExperimentalUuidApi

/**
 * Writer for OGG/Vorbis audio files.
 *
 * Strategy:
 * 1. Read entire source file into memory
 * 2. Scan OGG pages from the byte array
 * 3. Extract vendor string from the original comment page
 * 4. Build a new Vorbis comment page with updated metadata
 * 5. Reassemble: original page 0 + new comment page + original pages 2+
 * 6. Write to temp file and atomic-move to destination
 */
@OptIn(ExperimentalUuidApi::class, UnstableAudioTagApi::class)
internal class OggVorbisWriter : Writer {

    @UnstableAudioTagApi
    override fun write(src: Path, dst: Path, vararg operation: WriteOperation) {
        val op = operation.find { it is WriteOperation.AllMetadata } as WriteOperation.AllMetadata?

        // No-op: no operation provided — just copy
        if (op == null) {
            copyFile(src, dst)
            return
        }

        val fileBytes = SystemFileSystem.source(src).buffered().use { source ->
            source.readByteArray()
        }
        if (fileBytes.isEmpty()) {
            copyFile(src, dst)
            return
        }

        val pages = scanOggPages(fileBytes)
        // Need at least 3 pages: id header (0), comment (1), setup/audio (2+)
        if (pages.size < 3) {
            copyFile(src, dst)
            return
        }

        val page0 = pages[0]
        val page1 = pages[1]

        // Extract vendor string from original page 1's comment packet
        val vendor = extractVendorFromPage(page1)

        // Build new comment packet: packet type 0x03 + VorbisComment
        val vorbisCommentBytes = OggPageParser.serializeVorbisComment(vendor, op.metadatas)
        val commentPacket = byteArrayOf(0x03) + vorbisCommentBytes

        // Build new OGG page 1
        val newPage1 = buildOggPage(
            streamSerial = page0.streamSerial,
            pageSequence = 1,
            granulePosition = 0L,
            headerType = 0,
            packetData = commentPacket,
        )

        // Assemble output: page0 + new page1 + pages[2..]
        val outputSize = page0.totalSize + newPage1.size +
            (fileBytes.size - (pages[2].offset))
        val output = ByteArray(outputSize)
        var pos = 0

        // Page 0
        fileBytes.copyInto(output, 0, 0, page0.totalSize)
        pos += page0.totalSize

        // New page 1
        newPage1.copyInto(output, pos)
        pos += newPage1.size

        // Pages 2+
        val remainingStart = pages[2].offset
        fileBytes.copyInto(output, pos, remainingStart, fileBytes.size)

        // Write temp file and atomic move
        val tmp = SystemFileSystemUtil.tempFilePath()
        try {
            SystemFileSystem.sink(tmp).buffered().use { sink ->
                sink.write(output)
            }
            if (SystemFileSystem.exists(dst)) SystemFileSystem.delete(dst)
            SystemFileSystem.atomicMove(tmp, dst)
        } finally {
            if (SystemFileSystem.exists(tmp)) SystemFileSystem.delete(tmp)
        }
    }

    // ------------------------------------------------------------------
    // OGG page scanning (byte-array based)
    // ------------------------------------------------------------------

    /** Parsed information about a single OGG page in a byte array. */
    private data class PageInfo(
        val offset: Int,
        val totalSize: Int,
        val headerType: Int,
        val granulePosition: Long,
        val streamSerial: Int,
        val pageSequence: Int,
        val rawHeader: ByteArray,
        val segmentTable: ByteArray,
        val data: ByteArray,
    )

    /** Scan a byte array for all OGG pages. */
    private fun scanOggPages(data: ByteArray): List<PageInfo> {
        val pages = mutableListOf<PageInfo>()
        var offset = 0

        while (offset + 27 <= data.size) {
            // Check for "OggS" capture pattern
            if (data[offset] == 0x4F.toByte() &&
                data[offset + 1] == 0x67.toByte() &&
                data[offset + 2] == 0x67.toByte() &&
                data[offset + 3] == 0x53.toByte()
            ) {
                // Read 27-byte header
                val rawHeader = data.copyOfRange(offset, offset + 27)
                val headerType = data[offset + 5].toInt() and 0xFF
                val granulePosition = readLe64(data, offset + 6)
                val streamSerial = readLe32(data, offset + 14)
                val pageSequence = readLe32(data, offset + 18)
                val pageSegments = data[offset + 26].toInt() and 0xFF

                if (offset + 27 + pageSegments > data.size) break

                val segmentTable = data.copyOfRange(offset + 27, offset + 27 + pageSegments)
                val totalDataLen = segmentTable.sumOf { it.toInt() and 0xFF }

                if (offset + 27 + pageSegments + totalDataLen > data.size) break

                val pageData = data.copyOfRange(
                    offset + 27 + pageSegments,
                    offset + 27 + pageSegments + totalDataLen,
                )

                val totalSize = 27 + pageSegments + totalDataLen

                pages.add(
                    PageInfo(
                        offset = offset,
                        totalSize = totalSize,
                        headerType = headerType,
                        granulePosition = granulePosition,
                        streamSerial = streamSerial,
                        pageSequence = pageSequence,
                        rawHeader = rawHeader,
                        segmentTable = segmentTable,
                        data = pageData,
                    )
                )

                offset += totalSize
            } else {
                offset++
            }
        }

        return pages
    }

    // ------------------------------------------------------------------
    // Page building
    // ------------------------------------------------------------------

    /**
     * Build a complete OGG page containing [packetData] as a single packet.
     */
    private fun buildOggPage(
        streamSerial: Int,
        pageSequence: Int,
        granulePosition: Long,
        headerType: Int,
        packetData: ByteArray,
    ): ByteArray {
        val packetSize = packetData.size

        // Build segment table
        val segmentCount = (packetSize + 254) / 255
        val segmentTable = ByteArray(segmentCount)
        for (i in 0 until segmentCount) {
            segmentTable[i] = if (i < segmentCount - 1) {
                255.toByte()
            } else {
                ((packetSize - 1) % 255 + 1).toByte()
            }
        }

        // Build 27-byte header (checksum=0 placeholder)
        val header = ByteArray(27)
        // "OggS"
        header[0] = 0x4F
        header[1] = 0x67
        header[2] = 0x67
        header[3] = 0x53
        // Version = 0
        header[4] = 0
        // Header type
        header[5] = headerType.toByte()
        // Granule position (LE 64-bit)
        writeLe64At(header, 6, granulePosition)
        // Stream serial (LE 32-bit)
        writeLe32At(header, 14, streamSerial)
        // Page sequence (LE 32-bit)
        writeLe32At(header, 18, pageSequence)
        // Checksum placeholder (zero)
        // Already zero from initialization
        // Page segments count
        header[26] = segmentCount.toByte()

        // Compute CRC
        val crc = OggCrc.computePageCrc(header, segmentTable, packetData)
        writeU32BeAt(header, 22, crc)

        // Assemble: header + segment table + data
        val pageSize = 27 + segmentCount + packetSize
        val page = ByteArray(pageSize)
        header.copyInto(page, 0)
        segmentTable.copyInto(page, 27)
        packetData.copyInto(page, 27 + segmentCount)

        return page
    }

    // ------------------------------------------------------------------
    // Vendor string extraction
    // ------------------------------------------------------------------

    /**
     * Extract the vendor string from page 1's comment packet.
     *
     * Page 1 contains the Vorbis comment header:
     *   packet_type (1 byte, 0x03)
     *   vendor_length (4 bytes LE)
     *   vendor_string (N bytes)
     */
    private fun extractVendorFromPage(page: PageInfo): String {
        val data = page.data
        // Reassemble packets from segment table
        val packets = reassemblePackets(page.segmentTable, data)
        val commentPacket = packets.firstOrNull() ?: return VENDOR_FALLBACK

        // Skip packet type byte (0x03)
        if (commentPacket.size < 5 || (commentPacket[0].toInt() and 0xFF) != 0x03) {
            return VENDOR_FALLBACK
        }

        val vendorLen = readLe32(commentPacket, 1)
        if (vendorLen <= 0 || 5 + vendorLen > commentPacket.size) return VENDOR_FALLBACK

        return commentPacket.copyOfRange(5, 5 + vendorLen).decodeToString()
    }

    // ------------------------------------------------------------------
    // Packet reassembly (from segment table + data)
    // ------------------------------------------------------------------

    companion object {
        private val VENDOR_FALLBACK = "Salt Audio Tag"

        /** Reassemble segment data into packets (same logic as OggPageParser). */
        private fun reassemblePackets(
            segmentTable: ByteArray,
            rawData: ByteArray,
        ): List<ByteArray> {
            val packets = mutableListOf<ByteArray>()
            var dataPos = 0
            var currentPacket = mutableListOf<Byte>()

            for (segSizeByte in segmentTable) {
                val segSize = segSizeByte.toInt() and 0xFF
                if (dataPos + segSize <= rawData.size) {
                    for (i in 0 until segSize) {
                        currentPacket.add(rawData[dataPos + i])
                    }
                    dataPos += segSize
                }
                if (segSize < 255) {
                    packets.add(currentPacket.toByteArray())
                    currentPacket = mutableListOf()
                }
            }

            if (currentPacket.isNotEmpty()) {
                packets.add(currentPacket.toByteArray())
            }

            return packets
        }

        /** Copy file from src to dst. */
        private fun copyFile(src: Path, dst: Path) {
            SystemFileSystemUtil.copy(src, dst)
        }

        // ------------------------------------------------------------------
        // Binary I/O helpers
        // ------------------------------------------------------------------

        /** Read 4 bytes LE as unsigned 32-bit int. */
        private fun readLe32(data: ByteArray, offset: Int): Int =
            (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)

        /** Read 8 bytes LE as signed 64-bit long. */
        private fun readLe64(data: ByteArray, offset: Int): Long =
            (data[offset].toLong() and 0xFF) or
                ((data[offset + 1].toLong() and 0xFF) shl 8) or
                ((data[offset + 2].toLong() and 0xFF) shl 16) or
                ((data[offset + 3].toLong() and 0xFF) shl 24) or
                ((data[offset + 4].toLong() and 0xFF) shl 32) or
                ((data[offset + 5].toLong() and 0xFF) shl 40) or
                ((data[offset + 6].toLong() and 0xFF) shl 48) or
                ((data[offset + 7].toLong() and 0xFF) shl 56)

        /** Write 4 bytes LE at offset. */
        private fun writeLe32At(data: ByteArray, offset: Int, value: Int) {
            data[offset] = (value and 0xFF).toByte()
            data[offset + 1] = (value ushr 8 and 0xFF).toByte()
            data[offset + 2] = (value ushr 16 and 0xFF).toByte()
            data[offset + 3] = (value ushr 24 and 0xFF).toByte()
        }

        /** Write 8 bytes LE at offset. */
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

        /** Write 4 bytes BE at offset. */
        private fun writeU32BeAt(data: ByteArray, offset: Int, value: Int) {
            data[offset] = (value ushr 24 and 0xFF).toByte()
            data[offset + 1] = (value ushr 16 and 0xFF).toByte()
            data[offset + 2] = (value ushr 8 and 0xFF).toByte()
            data[offset + 3] = (value and 0xFF).toByte()
        }
    }
}
