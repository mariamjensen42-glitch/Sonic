@file:Suppress("SpellCheckingInspection")

package com.cycling.salt.audiotag.format.ogg

import com.cycling.salt.audiotag.rw.ReadStrategy
import com.cycling.salt.audiotag.rw.Reader
import com.cycling.salt.audiotag.rw.data.AudioTagData
import com.cycling.salt.audiotag.rw.data.Metadata
import com.cycling.salt.audiotag.rw.data.Streaminfo
import kotlinx.io.Source
import kotlinx.io.readByteArray

/**
 * Reader for OGG/Opus audio files.
 *
 * Reads the two Opus header packets (OpusHead, OpusTags),
 * extracts streaminfo and metadata, then calculates duration
 * from the last page's granule position minus pre-skip.
 */
class OggOpusReader : Reader {

    /** "OpusHead" magic bytes */
    private val OPUS_HEAD_MAGIC = byteArrayOf(0x4F, 0x70, 0x75, 0x73, 0x48, 0x65, 0x61, 0x64)

    /** "OpusTags" magic bytes */
    private val OPUS_TAGS_MAGIC = byteArrayOf(0x4F, 0x70, 0x75, 0x73, 0x54, 0x61, 0x67, 0x73)

    override fun read(source: Source, strategy: ReadStrategy): AudioTagData {
        var streaminfo: Streaminfo? = null
        var metadatas: List<Metadata>? = null

        // Read packets to get header data, plus the last granule for duration (single pass).
        val (packets, lastGranule) = OggPageParser.readPacketsAndLastGranule(source)
        if (packets.isEmpty()) return AudioTagData(null, null, null)

        val firstPacket = packets.first()

        // Validate OpusHead
        if (firstPacket.size < 19) return AudioTagData(null, null, null)
        if (!firstPacket.copyOfRange(0, 8).contentEquals(OPUS_HEAD_MAGIC)) {
            return AudioTagData(null, null, null)
        }

        val version = firstPacket[8].toInt() and 0xFF
        if (version != 1) {
            // Unsupported Opus version — attempt parsing anyway
        }

        val outputChannelCount = firstPacket[9].toInt() and 0xFF
        val preSkip = readLe16(firstPacket, 10)
        // inputSampleRate at offset 12 (4 bytes LE) — informational only
        // outputGain at offset 16 (2 bytes LE, signed)
        // channelMappingFamily at offset 18

        // Duration from last page granule (already read in the same pass as the packets)
        val sampleCount = if (lastGranule != null && lastGranule > preSkip) {
            lastGranule - preSkip
        } else {
            0L
        }

        if (strategy.streaminfo) {
            streaminfo = Streaminfo(
                sampleRate = 48000,
                channelCount = outputChannelCount,
                bits = 16,
                sampleCount = sampleCount,
                fileLevelMetadataLength = 0L,
            )
        }

        // Parse OpusTags (second packet)
        if (strategy.metadatas && packets.size >= 2) {
            val tagsPacket = packets[1]
            if (tagsPacket.size > 8 && tagsPacket.copyOfRange(0, 8).contentEquals(OPUS_TAGS_MAGIC)) {
                val (_, metaList) = OggPageParser.parseVorbisComment(
                    tagsPacket.copyOfRange(8, tagsPacket.size)
                )
                metadatas = metaList
            }
        }

        return AudioTagData(
            streaminfo = streaminfo,
            metadatas = metadatas,
            pictures = null,
        )
    }

    /** Read 2-byte little-endian unsigned 16-bit integer. */
    private fun readLe16(data: ByteArray, offset: Int): Long =
        ((data[offset].toLong() and 0xFF)) or
            ((data[offset + 1].toLong() and 0xFF) shl 8)
}
