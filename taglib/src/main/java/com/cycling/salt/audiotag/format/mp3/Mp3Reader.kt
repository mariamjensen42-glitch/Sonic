@file:Suppress("SpellCheckingInspection")

package com.cycling.salt.audiotag.format.mp3

import com.cycling.salt.audiotag.rw.ReadStrategy
import com.cycling.salt.audiotag.rw.Reader
import com.cycling.salt.audiotag.rw.data.AudioTagData
import com.cycling.salt.audiotag.rw.data.Streaminfo
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.io.readByteString

/**
 * Reader for MP3 files with ID3v2 tag support (v2.3 and v2.4).
 *
 * Streaming-safe: reads ID3v2 tag headers and frames from a Source without loading
 * the entire file into memory. Audio data scanning for streaminfo is bounded to 16KB.
 * Unsynchronisation is handled at the frame parsing level in Id3v2Frame.kt.
 */
class Mp3Reader : Reader {

    override fun read(source: Source, strategy: ReadStrategy): AudioTagData {
        var id3Size = 0L
        var frames: List<Id3v2Frame>? = null

        // ===== 1. Parse ID3v2 tag =====
        if (source.request(3)) {
            val magic = source.readByteArray(3).decodeToString()
            if (magic == "ID3") {
                val hdr = source.readByteString(3)
                val verMajor = hdr[0].toInt() and 0xFF
                val flags = hdr[2].toInt() and 0xFF

                // Tag size: 4-byte syncsafe — read at once
                val sizeRaw = source.readByteString(4)
                var tagSize = 0
                for (i in 0 until 4) {
                    tagSize = (tagSize shl 7) or (sizeRaw[i].toInt() and 0x7F)
                }
                id3Size = ID3_HEADER_SIZE.toLong() + tagSize

                // Safety cap: refuse unreasonably large tags (OOM protection)
                if (tagSize > MAX_ID3_TAG_SIZE) {
                    source.skip(tagSize.toLong())
                    return AudioTagData(null, null, null)
                }

                if (verMajor in 2..4 && tagSize > 0 && source.request(tagSize.toLong())) {
                    val raw = source.readByteArray(tagSize)
                    var off = 0
                    if ((flags and 0x40) != 0) {
                        off = when (verMajor) {
                            3 -> readIntBE(raw, 0)
                            4 -> readSyncsafe(raw, 0)
                            else -> 0
                        }
                    }
                    if (off < raw.size) {
                        frames = if (off > 0) parseFrames(raw.copyOfRange(off, raw.size))
                                 else parseFrames(raw)
                    }
                }
            }
        }

        // ===== 2. Convert frames =====
        var metadataList: List<com.cycling.salt.audiotag.rw.data.Metadata>? = null
        var pictures: List<com.cycling.salt.audiotag.rw.data.Picture>? = null
        if (frames != null) {
            val (metas, pics) = framesToAudioTagData(frames, strategy.pictureReadMode, strategy.loadPictureBinary)
            if (strategy.metadatas) metadataList = metas
            if (strategy.pictureReadMode !is com.cycling.salt.audiotag.rw.PictureReadMode.None && pics.isNotEmpty()) {
                pictures = pics
            }
        }

        // ===== 3. Stream info =====
        val streaminfo = if (strategy.streaminfo) parseStreaminfo(source, id3Size) else null

        return AudioTagData(streaminfo, metadataList, pictures)
    }

    /**
     * Parse MPEG stream info from audio data immediately following the ID3v2 tag.
     *
     * Reads a bounded amount of audio data (max 16KB) from source to find a valid
     * MPEG sync frame for stream info extraction. Does not load the entire file.
     * Xing/Info headers within MPEG frames provide sample count for duration.
     */
    private fun parseStreaminfo(source: Source, id3Size: Long): Streaminfo? {
        try {
            if (!source.request(4)) return null
            // Read available audio data into a single buffer for scanning
            val tmp = source.readByteArray(16_384)
            if (tmp.size < 4) return null

            val syncIdx = findSyncInBuffer(tmp) ?: return null
            if (syncIdx + 4 > tmp.size) return null

            val b1 = tmp[syncIdx + 1].toInt() and 0xFF
            val b2 = tmp[syncIdx + 2].toInt() and 0xFF
            val b3 = tmp[syncIdx + 3].toInt() and 0xFF

            val versionBits = (b1 ushr 3) and 0x03
            val layerBits = (b1 ushr 1) and 0x03
            val hasCrc = (b1 and 0x01) == 0x00
            val srIdx = (b2 ushr 2) and 0x03
            val channelMode = (b3 ushr 6) and 0x03
            val sr = getSampleRate(versionBits, srIdx) ?: 44100
            val channels = if (channelMode == 3) 1 else 2

            // Xing/Info header only in Layer III
            if (layerBits != 0x01) {
                return Streaminfo(sr, channels, 16, 0L, id3Size)
            }

            val isMpeg1 = versionBits == 0x03
            val sideSize = if (isMpeg1) 32 else 17
            val crcBytes = if (hasCrc) 2 else 0
            val xingOffset = syncIdx + 4 + crcBytes + sideSize

            if (xingOffset + 8 <= tmp.size) {
                val magic = tmp.copyOfRange(xingOffset, xingOffset + 4).decodeToString()
                if (magic in listOf("Xing", "Info")) {
                    val flags = tmp[xingOffset + 4].toInt() and 0xFF
                    var xp = xingOffset + 5
                    if ((flags and 0x01) != 0) {
                        val numFrames = readIntBE(tmp, xp)
                        if (numFrames > 0 && sr > 0) {
                            return Streaminfo(sr, channels, 16, numFrames.toLong() * 1152, id3Size)
                        }
                    }
                }
            }

            return Streaminfo(sr, channels, 16, 0L, id3Size)
        } catch (_: Exception) {
            return null
        }
    }

    private fun findSyncInBuffer(buf: ByteArray): Int? {
        var i = 0
        while (i + 1 < buf.size) {
            if ((buf[i].toInt() and 0xFF) == 0xFF &&
                (buf[i + 1].toInt() and 0xE0) == 0xE0
            ) return i
            i++
        }
        return null
    }

    private fun getSampleRate(ver: Int, idx: Int): Int? = when (ver) {
        0x00 -> when (idx) { 0 -> 11025; 1 -> 12000; 2 -> 8000; else -> null }
        0x02 -> when (idx) { 0 -> 22050; 1 -> 24000; 2 -> 16000; else -> null }
        0x03 -> when (idx) { 0 -> 44100; 1 -> 48000; 2 -> 32000; else -> null }
        else -> null
    }

    companion object {
        /** Maximum accepted ID3v2 tag size (100 MB). Tags exceeding this are skipped. */
        private const val MAX_ID3_TAG_SIZE = 104_857_600

        internal fun readIntBE(d: ByteArray, off: Int): Int {
            return ((d[off].toInt() and 0xFF) shl 24) or
                    ((d[off + 1].toInt() and 0xFF) shl 16) or
                    ((d[off + 2].toInt() and 0xFF) shl 8) or
                    (d[off + 3].toInt() and 0xFF)
        }
        internal fun readSyncsafe(d: ByteArray, off: Int): Int {
            return ((d[off].toInt() and 0x7F) shl 21) or
                    ((d[off + 1].toInt() and 0x7F) shl 14) or
                    ((d[off + 2].toInt() and 0x7F) shl 7) or
                    (d[off + 3].toInt() and 0x7F)
        }
    }
}
