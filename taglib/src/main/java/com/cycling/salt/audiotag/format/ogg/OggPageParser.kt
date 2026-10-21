@file:Suppress("SpellCheckingInspection")

package com.cycling.salt.audiotag.format.ogg

import com.cycling.salt.audiotag.rw.data.Metadata
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.io.readUByte
import kotlinx.io.readULong

/**
 * OGG page as defined by RFC 7845 §3.
 *
 * @property version Page version number (must be 0)
 * @property headerType Bit flags: 0x01 = continued packet, 0x02 = first page, 0x04 = last page
 * @property granulePosition Absolute time position in PCM samples
 * @property streamSerial Serial number identifying the bitstream
 * @property pageSequence Page counter within the bitstream
 * @property checksum CRC-32 of the entire page data
 * @property segments Raw packet data reassembled from segment table entries
 */
internal data class OggPage(
    val version: Int,
    val headerType: Int,
    val granulePosition: Long,
    val streamSerial: Int,
    val pageSequence: Int,
    val checksum: ByteArray,
    val segments: List<ByteArray>,
)

/**
 * OGG container page parser.
 *
 * Handles capture pattern detection, header parsing, segment table reassembly,
 * and continued packet support across multiple pages.
 */
internal object OggPageParser {

    /** OGG capture pattern: "OggS" */
    private val CAPTURE_PATTERN = byteArrayOf(0x4F, 0x67, 0x67, 0x53)

    /**
     * Read a single OGG page from [source].
     * Returns null if the capture pattern is not found or the source is exhausted/truncated.
     *
     * Sonic fix: the stream may legitimately end right after a page (the readers used to scan it
     * twice and then hit EOF), so EOF must mean "no more pages", not an exception.
     */
    fun readPage(source: Source): OggPage? {
        val magic = runCatching { source.readByteArray(4) }.getOrNull() ?: return null
        if (!magic.contentEquals(CAPTURE_PATTERN)) return null

        return try {
            val version = source.readUByte().toInt()
            val headerType = source.readUByte().toInt()
            val granulePosition = source.readULong().toLong()
            val streamSerial = source.readInt()
            val pageSequence = source.readInt()
            val checksum = source.readByteArray(4)
            val pageSegments = source.readUByte().toInt()
            val segmentTable = source.readByteArray(pageSegments)

            // Total data length from segment table
            val totalDataLen = segmentTable.fold(0) { acc, b -> acc + (b.toInt() and 0xFF) }
            val rawData = source.readByteArray(totalDataLen)

            // Reassemble segments into packets
            val packets = reassemblePackets(segmentTable, rawData)

            OggPage(
                version = version,
                headerType = headerType,
                granulePosition = granulePosition,
                streamSerial = streamSerial,
                pageSequence = pageSequence,
                checksum = checksum,
                segments = packets,
            )
        } catch (e: kotlinx.io.IOException) {
            // Truncated trailing page: treat it as end of stream instead of failing the read.
            null
        }
    }

    /**
     * Read multiple OGG pages and flatten their packet data, together with the last granule
     * position seen.
     *
     * Handles continued packets where the last segment of a page is 0xFF
     * and the next page continues with headerType bit 0 set.
     *
     * Sonic fix: readers need the duration, and the old code scanned the source a second time
     * with [readLastGranulePosition] after this function had already consumed it — which threw
     * `EOFException` for every file that ended on an EOS page.
     */
    fun readPacketsAndLastGranule(
        source: Source,
        maxPages: Int = Int.MAX_VALUE
    ): Pair<List<ByteArray>, Long?> {
        val allPackets = mutableListOf<ByteArray>()
        val carryForward = mutableListOf<Byte>()
        var lastGranule: Long? = null
        var pagesRead = 0

        while (pagesRead < maxPages) {
            val page = readPage(source) ?: break
            pagesRead++
            lastGranule = page.granulePosition

            val isContinued = (page.headerType and 0x01) != 0
            val isLastPage = (page.headerType and 0x04) != 0

            // If continued and we have carry-over bytes, prepend them to the first segment
            val segments = page.segments.toMutableList()
            if (isContinued && carryForward.isNotEmpty()) {
                if (segments.isNotEmpty()) {
                    segments[0] = carryForward.toByteArray() + segments[0]
                } else {
                    segments.add(carryForward.toByteArray())
                }
                carryForward.clear()
            }

            // Process segments: a segment with size < 255 ends a packet
            var currentPacket = mutableListOf<Byte>()
            for ((i, segment) in segments.withIndex()) {
                currentPacket.addAll(segment.toList())
                val segmentSize = segment.size
                // A segment of exactly 255 means "packet continues"
                // A segment of < 255 means "packet ends here"
                if (segmentSize < 255) {
                    if (currentPacket.isNotEmpty()) {
                        allPackets.add(currentPacket.toByteArray())
                        currentPacket = mutableListOf()
                    }
                }
                // If segment is 255 and this is the last segment of the page, packet continues on next page
                if (segmentSize == 255 && i == segments.lastIndex) {
                    carryForward.addAll(currentPacket.toList())
                    currentPacket = mutableListOf()
                }
            }
            // If there were no segments at all on this page, retain forward
            if (segments.isEmpty()) {
                carryForward.addAll(currentPacket.toList())
                currentPacket = mutableListOf()
            }
            // If we finished a packet exactly at page boundary, add it
            if (currentPacket.isNotEmpty()) {
                allPackets.add(currentPacket.toByteArray())
            }

            if (isLastPage) break
        }

        return allPackets to lastGranule
    }

    /** Read multiple OGG pages and flatten their packet data (see [readPacketsAndLastGranule]). */
    fun readPages(source: Source, maxPages: Int = Int.MAX_VALUE): List<ByteArray> =
        readPacketsAndLastGranule(source, maxPages).first

    /**
     * Parse VorbisComment binary data into Metadata list.
     *
     * Binary format (same for Vorbis and Opus):
     *   vendor_length (4 bytes LE)
     *   vendor_string (vendor_length bytes, UTF-8)
     *   user_comment_list_length (4 bytes LE)
     *   N × [comment_length (4 bytes LE) + comment (comment_length bytes, "KEY=VALUE")]
     *   framing bit (1 byte, 0x01)
     */
    fun parseVorbisComment(data: ByteArray): Pair<String, List<Metadata>> {
        var offset = 0

        // Vendor string
        val vendorLen = readLe32(data, offset).toInt()
        offset += 4
        val vendorString = if (vendorLen > 0 && offset + vendorLen <= data.size) {
            data.copyOfRange(offset, offset + vendorLen).decodeToString()
        } else ""
        offset += vendorLen

        // User comment list
        val commentCount = readLe32(data, offset).toInt()
        offset += 4

        val metadatas = mutableListOf<Metadata>()
        for (i in 0 until commentCount) {
            if (offset + 4 > data.size) break
            val commentLen = readLe32(data, offset).toInt()
            offset += 4
            if (offset + commentLen > data.size) break
            val comment = data.copyOfRange(offset, offset + commentLen).decodeToString()
            offset += commentLen

            val eqIdx = comment.indexOf('=')
            if (eqIdx > 0) {
                val rawKey = comment.substring(0, eqIdx).trim().uppercase()
                val value = comment.substring(eqIdx + 1).trim()
                val mappedKey = VORBIS_COMMENT_TO_METADATA[rawKey]
                if (mappedKey != null && value.isNotEmpty()) {
                    metadatas.add(Metadata(mappedKey, value))
                }
            }
        }

        return vendorString to metadatas
    }

    /**
     * Serialize a VorbisComment into its binary representation.
     *
     * This is the inverse of [parseVorbisComment]. Produces bytes in the format:
     *   vendor_length (4 LE)
     *   vendor_string (N bytes UTF-8)
     *   user_comment_list_length (4 LE)
     *   N × [comment_length (4 LE) + "KEY=VALUE" (M bytes UTF-8)]
     *   framing bit (1 byte, 0x01)
     *
     * @param vendorString the vendor/encoder string
     * @param metadatas the metadata entries to serialize
     * @return the serialized VorbisComment binary (without packet type byte)
     */
    fun serializeVorbisComment(
        vendorString: String,
        metadatas: List<Metadata>,
    ): ByteArray {
        val buf = mutableListOf<Byte>()

        val vendorBytes = vendorString.encodeToByteArray()
        writeLe32(buf, vendorBytes.size)
        buf.addAll(vendorBytes.toList())

        writeLe32(buf, metadatas.size)
        for (meta in metadatas) {
            val entry = "${meta.key}=${meta.value}".encodeToByteArray()
            writeLe32(buf, entry.size)
            buf.addAll(entry.toList())
        }

        // Framing bit
        buf.add(0x01)

        return buf.toByteArray()
    }

    /** Write a 4-byte little-endian integer into the buffer. */
    fun writeLe32(buf: MutableList<Byte>, value: Int) {
        buf.add((value and 0xFF).toByte())
        buf.add((value ushr 8 and 0xFF).toByte())
        buf.add((value ushr 16 and 0xFF).toByte())
        buf.add((value ushr 24 and 0xFF).toByte())
    }

    /**
     * Scan source to the last OGG page and extract its granule position.
     * Used for duration calculation.
     */
    fun readLastGranulePosition(source: Source): Long? {
        // Eager approach: scan forward through all pages
        var lastGranule: Long? = null
        while (true) {
            val page = readPage(source) ?: break
            lastGranule = page.granulePosition
        }
        return lastGranule
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    /**
     * Reassemble OGG segments into packets based on segment table.
     * A segment value of 255 means "continued on next segment."
     * A value < 255 ends the current packet.
     */
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
                // Packet ends here
                packets.add(currentPacket.toByteArray())
                currentPacket = mutableListOf()
            }
            // segSize == 255: packet continues into the next segment
        }

        // Any remaining bytes form a continued packet (carries to next page)
        if (currentPacket.isNotEmpty()) {
            packets.add(currentPacket.toByteArray())
        }

        return packets
    }

    /** Read 4-byte little-endian unsigned 32-bit integer. */
    private fun readLe32(data: ByteArray, offset: Int): Long =
        ((data[offset].toLong() and 0xFF)) or
            ((data[offset + 1].toLong() and 0xFF) shl 8) or
            ((data[offset + 2].toLong() and 0xFF) shl 16) or
            ((data[offset + 3].toLong() and 0xFF) shl 24)

    /**
     * VorbisComment field name to Metadata key mapping.
     * Covers all common fields per Vorbis I specification.
     */
    val VORBIS_COMMENT_TO_METADATA: Map<String, String> = mapOf(
        "TITLE" to "TITLE",
        "ARTIST" to "ARTIST",
        "ALBUM" to "ALBUM",
        "DATE" to "DATE",
        "GENRE" to "GENRE",
        "TRACKNUMBER" to "TRACKNUMBER",
        "DISCNUMBER" to "DISCNUMBER",
        "COMPOSER" to "COMPOSER",
        "COMMENT" to "COMMENT",
        "DESCRIPTION" to "DESCRIPTION",
        "LYRICS" to "LYRICS",
        "BPM" to "BPM",
        "COPYRIGHT" to "COPYRIGHT",
        "PUBLISHER" to "PUBLISHER",
        "ENCODER" to "ENCODED_BY",
        "ENCODEDBY" to "ENCODED_BY",
        "ENCODED_BY" to "ENCODED_BY",
        "ISRC" to "ISRC",
        "ALBUMARTIST" to "ALBUM_ARTIST",
        "ALBUM_ARTIST" to "ALBUM_ARTIST",
        "PERFORMER" to "ARTIST",
        "ARTISTARTIST" to "ALBUM_ARTIST",
        "ORGANIZATION" to "PUBLISHER",
        "LICENSE" to "COPYRIGHT",
        "CONTACT" to "DESCRIPTION",
        // Sonic 增补：总量保留为独立键，读取侧才能拼回 `3/12`；映射成 TRACKNUMBER 会把
        // 「音轨号」写成两个同名条目，取第一个就丢掉总数。
        "TRACKTOTAL" to "TRACKTOTAL",
        "DISCTOTAL" to "DISCTOTAL",
        // Sonic 增补：封面图在 Ogg Vorbis 里就是普通注释，读取时若丢掉，回写标签会把封面抹掉。
        // 这里让它们原样穿过映射表，写回时再由调用方原样写回。
        "METADATA_BLOCK_PICTURE" to "METADATA_BLOCK_PICTURE",
        "COVERART" to "COVERART",
        "COVERARTMIME" to "COVERARTMIME",
    )
}
