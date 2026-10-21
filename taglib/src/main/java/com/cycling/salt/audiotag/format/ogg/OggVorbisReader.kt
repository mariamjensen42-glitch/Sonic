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
 * Reader for OGG/Vorbis audio files.
 *
 * Reads the three OGG Vorbis header packets (identification, comment, setup),
 * extracts streaminfo and metadata, then calculates duration from the last
 * page's granule position.
 */
class OggVorbisReader : Reader {

    override fun read(source: Source, strategy: ReadStrategy): AudioTagData {
        var streaminfo: Streaminfo? = null
        var metadatas: List<Metadata>? = null

        // Read header packets and the last granule position in a single pass. (The old code
        // consumed the whole source here and then scanned it again for the duration, which threw
        // EOFException on every file whose last page carried the EOS flag.)
        val (packets, lastGranule) = OggPageParser.readPacketsAndLastGranule(source)
        if (packets.isEmpty()) return AudioTagData(null, null, null)

        // First packet: Vorbis identification header
        // Packet type byte 0x01, then "v1" magic (0x76 0x6F 0x72 0x62 0x69 0x73)

        // Parse identification header from first packet
        val idHeader = packets.firstOrNull() ?: return AudioTagData(null, null, null)
        if (idHeader.isEmpty() || idHeader[0].toInt() and 0xFF != 0x01) {
            // Not a Vorbis identification header
            return AudioTagData(null, null, null)
        }

        // Ensure minimum Vorbis id header size: type(1) + "vorbis"(5) + version(4) + channels(1) + rate(4) + bitrate_max(4) + bitrate_nom(4) + bitrate_min(4) = 30
        if (idHeader.size < 30) return AudioTagData(null, null, null)

        // Vorbis identification header layout:
        //   type(1) + "vorbis"(6) + version(4) + channels(1) + sampleRate(4) + bitrates(12) + ...
        // Sonic fix: upstream read channels/rate from offsets 5/6, i.e. inside the "vorbis" magic,
        // so every stream reported a bogus sample rate and channel count.
        val channels = idHeader[11].toInt() and 0xFF
        val sampleRate = readLe32(idHeader, 12).toInt()

        // `lastGranule` was read above together with the packets.
        if (strategy.streaminfo) {
            streaminfo = Streaminfo(
                sampleRate = sampleRate,
                channelCount = channels,
                bits = 16,
                sampleCount = lastGranule ?: 0L,
                fileLevelMetadataLength = 0L,
            )
        }

        // Parse Vorbis comment header (second packet, type 0x03)
        if (strategy.metadatas && packets.size >= 2) {
            val commentPacket = packets[1]
            if (commentPacket.isNotEmpty() && (commentPacket[0].toInt() and 0xFF) == 0x03) {
                // Skip packet type byte (0x03)
                if (commentPacket.size > 1) {
                    val (_, metaList) = OggPageParser.parseVorbisComment(
                        commentPacket.copyOfRange(1, commentPacket.size)
                    )
                    metadatas = metaList
                }
            }
        }

        return AudioTagData(
            streaminfo = streaminfo,
            metadatas = metadatas,
            pictures = null,
        )
    }

    /** Read 4-byte little-endian unsigned 32-bit integer. */
    private fun readLe32(data: ByteArray, offset: Int): Long =
        ((data[offset].toLong() and 0xFF)) or
            ((data[offset + 1].toLong() and 0xFF) shl 8) or
            ((data[offset + 2].toLong() and 0xFF) shl 16) or
            ((data[offset + 3].toLong() and 0xFF) shl 24)
}
