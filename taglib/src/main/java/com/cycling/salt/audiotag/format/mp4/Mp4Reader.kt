@file:Suppress("SpellCheckingInspection")

package com.cycling.salt.audiotag.format.mp4

import com.cycling.salt.audiotag.rw.PictureReadMode
import com.cycling.salt.audiotag.rw.ReadStrategy
import com.cycling.salt.audiotag.rw.Reader
import com.cycling.salt.audiotag.rw.data.AudioTagData
import com.cycling.salt.audiotag.rw.data.Metadata
import com.cycling.salt.audiotag.rw.data.Picture
import com.cycling.salt.audiotag.rw.data.Streaminfo
import kotlinx.io.Source
import kotlinx.io.readByteArray

/**
 * Reader for MP4 / M4A / AAC audio files.
 *
 * Parses ISOBMFF container: scans ftyp → ... → moov via two-pass approach.
 * First pass locates the moov box by walking top-level box headers (handles
 * moov at any offset, including after mdat). Second pass reads only the moov
 * box's bytes into memory for parsing mvhd, udta/meta/ilst, and trak/stsd/mp4a.
 *
 * Streaminfo from stsd → mp4a (via parseTrak) takes priority over mvhd defaults.
 */
class Mp4Reader : Reader {

    companion object {
        /** Maximum bytes to scan when searching for moov box (100 MB). */
        private const val MAX_SCAN_SIZE = 104_857_600L

        /** Maximum moov box size to accept (50 MB). */
        private const val MAX_MOOV_SIZE = 52_428_800
    }

    override fun read(source: Source, strategy: ReadStrategy): AudioTagData {
        val readMetadatas = strategy.metadatas
        val readPictures = strategy.pictureReadMode !is PictureReadMode.None
        val loadBin = strategy.loadPictureBinary

        var streaminfo: Streaminfo? = null
        val metadatas = mutableListOf<Metadata>()
        val pictures = mutableListOf<Picture>()

        // ---- Pass 1: Scan for moov box offset ----
        val moovBox = findMoovBox(source) ?: return AudioTagData(null, null, null)

        // ---- Pass 2: Read moov box data and parse ----
        if (!source.request(moovBox.second.toLong())) {
            return AudioTagData(null, null, null)
        }
        val data = source.readByteArray(moovBox.second)
        if (data.size < 12) return AudioTagData(null, null, null)
        if (data.readU32(0).toInt() < 8) return AudioTagData(null, null, null)

        // Parse moov children (skip the moov header: 8 or 16 bytes)
        val moovHeaderSize = if (data.readU32(0) == 1L) 16 else 8
        parseMoov(data, moovHeaderSize, data.size, readMetadatas, readPictures, loadBin,
            metadatas, pictures).let { streaminfo = streaminfo ?: it }

        return AudioTagData(
            streaminfo = if (strategy.streaminfo) streaminfo else null,
            metadatas = if (readMetadatas && metadatas.isNotEmpty()) metadatas else null,
            pictures = if (readPictures && pictures.isNotEmpty()) pictures else null,
        )
    }

    /**
     * Scan the source ISOBMFF box headers to find the moov box.
     * Returns (moovBoxOffset, moovBoxSize) or null if not found.
     *
     * Walks top-level box headers sequentially from the source. Skips past
     * non-moov boxes (including large mdat) without loading them into memory.
     */
    private fun findMoovBox(source: Source): Pair<Int, Int>? {
        var scanned = 0L

        // First box: read at least a full box header
        while (scanned < MAX_SCAN_SIZE) {
            if (!source.request(8)) return null
            val headerBytes = source.readByteArray(8)
            scanned += 8

            var boxSize = readU32BE(headerBytes, 0).toInt()
            val boxType = headerBytes.copyOfRange(4, 8).decodeToString()

            if (boxSize == 0) break // end of file marker
            if (boxSize == 1) {
                // 64-bit extended size
                if (!source.request(8)) return null
                val extBytes = source.readByteArray(8)
                scanned += 8
                val high = readU32BE(extBytes, 0).toInt()
                val low = readU32BE(extBytes, 4).toInt()
                val extSize = (high.toLong() shl 32) or (low.toLong() and 0xFFFFFFFFL)
                boxSize = if (extSize <= Int.MAX_VALUE) extSize.toInt() else 8
            }
            if (boxSize < 8) break

            if (boxType == "moov") {
                if (boxSize > MAX_MOOV_SIZE) return null
                return (scanned - 8).toInt() to boxSize
            }

            // Skip past this box
            val skipBytes = (boxSize - 8).coerceAtLeast(0).toLong()
            if (skipBytes > 0 && !source.request(skipBytes)) return null
            if (skipBytes > 0) {
                source.skip(skipBytes)
                scanned += skipBytes
            }
        }
        return null
    }

    /** Read 4-byte big-endian unsigned 32-bit integer from byte array. */
    private fun readU32BE(data: ByteArray, off: Int): Long =
        ((data[off].toLong() and 0xFF) shl 24) or
            ((data[off + 1].toLong() and 0xFF) shl 16) or
            ((data[off + 2].toLong() and 0xFF) shl 8) or
            (data[off + 3].toLong() and 0xFF)

    /**
     * Parse moov box and its children.
     * Returns Streaminfo if available.
     */
    private fun parseMoov(
        data: ByteArray,
        start: Int,
        end: Int,
        readMetadatas: Boolean,
        readPictures: Boolean,
        loadBin: Boolean,
        metadatas: MutableList<Metadata>,
        pictures: MutableList<Picture>,
    ): Streaminfo? {
        var streaminfo: Streaminfo? = null
        var off = start

        while (off + 8 <= end) {
            var boxSize = data.readU32(off).toInt()
            val boxType = data.copyOfRange(off + 4, off + 8).decodeToString()

            if (boxSize < 8) break
            if (off + boxSize > end) boxSize = end - off

            val childOff = off + 8
            val childEnd = (off + boxSize).coerceAtMost(data.size)

            when {
                boxType == "mvhd" && childOff + 24 <= data.size -> {
                    streaminfo = parseMvhd(data, childOff)
                }
                boxType == "udta" && readMetadatas -> {
                    parseUdta(data, childOff, childEnd, readPictures, readMetadatas, loadBin, metadatas, pictures)
                }
                boxType == "trak" -> {
                    parseTrak(data, childOff, childEnd).let { streaminfo = streaminfo ?: it }
                }
            }

            off += boxSize
        }

        return streaminfo
    }

    /** Parse mvhd: version(1) flags(3) creation(4) modification(4) timescale(4) duration(4) */
    private fun parseMvhd(data: ByteArray, off: Int): Streaminfo? {
        // min mvhd size: version(1)+flags(3)+created(4)+modified(4)+timescale(4)+duration(4)=20
        if (off + 20 > data.size) return null
        val version = data[off].toInt() and 0xFF
        val timescale = data.readU32(off + 12).toInt()
        if (timescale <= 0) return null
        val duration = if (version == 0) {
            data.readU32(off + 16).toInt().toLong()
        } else {
            // version 1: duration is 8 bytes at offset 20
            if (off + 28 > data.size) return null
            (data.readU32(off + 20).toLong() shl 32) or data.readU32(off + 24)
        }
        // Use 44100 as default sample rate; actual rate is in stsd/mp4a
        return Streaminfo(sampleRate = 44100, channelCount = 2, bits = 16,
            sampleCount = duration, fileLevelMetadataLength = 0)
    }

    /** Parse udta → meta → ilst for metadata atoms */
    private fun parseUdta(
        data: ByteArray,
        start: Int,
        end: Int,
        readPictures: Boolean,
        readMetadatas: Boolean,
        loadBin: Boolean,
        metadatas: MutableList<Metadata>,
        pictures: MutableList<Picture>,
    ) {
        var off = start
        while (off + 8 <= end) {
            var boxSize = data.readU32(off).toInt()
            val boxType = data.copyOfRange(off + 4, off + 8).decodeToString()

            if (boxSize < 8) break
            if (off + boxSize > end) boxSize = end - off

            val childOff = off + 8
            val childEnd = (off + boxSize).coerceAtMost(data.size)

            when (boxType) {
                "meta" -> {
                    // meta has a 4-byte version/flag header (full box)
                    val metaDataOff = childOff + 4
                    parseMeta(data, metaDataOff, childEnd, readPictures, readMetadatas, loadBin, metadatas, pictures)
                }
                // Some files have ilst directly under udta (nonstandard)
                "ilst" -> {
                    parseIlst(data, childOff, childEnd, readPictures, readMetadatas, loadBin, metadatas, pictures)
                }
            }

            off += boxSize
        }
    }

    /** Parse meta → ilst */
    private fun parseMeta(
        data: ByteArray,
        start: Int,
        end: Int,
        readPictures: Boolean,
        readMetadatas: Boolean,
        loadBin: Boolean,
        metadatas: MutableList<Metadata>,
        pictures: MutableList<Picture>,
    ) {
        var off = start
        while (off + 8 <= end) {
            var boxSize = data.readU32(off).toInt()
            val boxType = data.copyOfRange(off + 4, off + 8).decodeToString()

            if (boxSize < 8) break
            if (off + boxSize > end) boxSize = end - off

            if (boxType == "ilst") {
                parseIlst(data, off + 8, (off + boxSize).coerceAtMost(data.size),
                    readPictures, readMetadatas, loadBin, metadatas, pictures)
            }

            off += boxSize
        }
    }

    /** Parse ilst: each child atom is a metadata item containing a data sub-atom */
    private fun parseIlst(
        data: ByteArray,
        start: Int,
        end: Int,
        readPictures: Boolean,
        readMetadatas: Boolean,
        loadBin: Boolean,
        metadatas: MutableList<Metadata>,
        pictures: MutableList<Picture>,
    ) {
        var off = start
        while (off + 8 <= end) {
            val atomSize = data.readU32(off).toInt()
            val atomName = data.copyOfRange(off + 4, off + 8).decodeToString()

            if (atomSize < 8 || off + atomSize > end) break

            // Each atom contains a "data" sub-atom: [size][data][typeIndex(4)][locale(4)][value...]
            val dataOff = off + 8
            val dataEnd = (off + atomSize).coerceAtMost(data.size)

            if (dataOff + 16 <= dataEnd) {
                val innerSize = data.readU32(dataOff).toInt()
                val innerType = data.copyOfRange(dataOff + 4, dataOff + 8).decodeToString()

                if (innerType == "data" && innerSize >= 16) {
                    val typeIndex = data.readU32(dataOff + 8).toInt() // 0=implicit, 1=UTF-8, 13=JPEG, 14=PNG, 21=BE-signed-int
                    // locale (4 bytes) is always 0
                    val valueOff = dataOff + 16
                    val valueLen = (dataOff + innerSize - 16).coerceAtMost(dataEnd - valueOff)

                    if (atomName == "covr" && readPictures) {
                        // Cover art: typeIndex signals data type
                        if (typeIndex == 13 || typeIndex == 14) {
                            val picData = if (loadBin) data.copyOfRange(valueOff, valueOff + valueLen) else byteArrayOf()
                            val mime = if (typeIndex == 13) "image/jpeg" else "image/png"
                            pictures.add(Picture(
                                pictureType = Picture.PictureType.FrontCover,
                                mediaType = mime,
                                description = "",
                                width = 0, height = 0, colorDepth = 0, colorsNumber = 0,
                                pictureData = picData,
                                dataLength = valueLen,
                            ))
                        }
                    } else if (valueLen > 0 && readMetadatas) {
                        val nameBytes = data.copyOfRange(off + 4, off + 8)
                        val atomNameText = nameBytes.decodeToString()
                        val metadataKey = Mp4Constants.atomToMetadataKey(nameBytes)
                        if (metadataKey != null) {
                            val isOrdinalAtom = atomNameText == "trkn" || atomNameText == "disk"
                            val value = when {
                                // trkn/disk 是二进制原子（iTunes 写 type 0，部分工具写 21），按
                                // [index BE16][total BE16] 解析；type 1 才是旧版本库写的文本。
                                isOrdinalAtom && typeIndex != 1 && valueLen >= 4 -> {
                                    val index = data.readU16(valueOff + 2)
                                    val total = if (valueLen >= 6) data.readU16(valueOff + 4) else 0
                                    if (total > 0) "$index/$total" else "$index"
                                }
                                typeIndex == 0 -> {
                                    // Implicit: can be UTF-8 bytes; skip BOM if present
                                    val slice = data.copyOfRange(valueOff, valueOff + valueLen)
                                    if (slice.size >= 3 && slice[0] == 0xEF.toByte() && slice[1] == 0xBB.toByte() && slice[2] == 0xBF.toByte())
                                        slice.copyOfRange(3, slice.size).decodeToString()
                                    else
                                        slice.decodeToString()
                                }
                                typeIndex == 1 -> {
                                    // UTF-8 text
                                    data.copyOfRange(valueOff, valueOff + valueLen).decodeToString()
                                }
                                typeIndex == 21 -> {
                                    // Big-endian signed integer (for trkn/disk)
                                    if (valueLen >= 4) {
                                        // trkn/disk: [index][total][padding] as BE u16s
                                        val index = data.readU16(valueOff + 2)
                                        val total = data.readU16(valueOff + 4)
                                        if (total > 0) "$index/$total" else "$index"
                                    } else ""
                                }
                                else -> ""
                            }
                            if (value.isNotBlank() && value != "0" && value != "0/0") {
                                metadatas.add(Metadata(metadataKey, value))
                            }
                        }
                    }
                }
            }

            off += atomSize
        }
    }

    /** Parse trak → mdia → minf → stbl → stsd → mp4a for sample rate & channels */
    private fun parseTrak(data: ByteArray, start: Int, end: Int): Streaminfo? {
        // Walk down: trak → mdia → minf → stbl → stsd → mp4a
        var off = start
        var stblEnd = end

        // Navigate one level at a time
        stblEnd = findChildBox(data, off, end, "mdia")?.let { mdiaEnd ->
            findChildBox(data, off + 8, mdiaEnd.coerceAtMost(data.size), "minf")?.let { minfEnd ->
                findChildBox(data, off + 8 + 8, minfEnd.coerceAtMost(data.size), "stbl")?.let { stblBoxEnd ->
                    findChildBox(data, off + 8 + 8 + 8, stblBoxEnd.coerceAtMost(data.size), "stsd")?.let { stsdEnd ->
                        // stsd has a full box header (4-byte version/flags) + 4-byte entry count
                        val stsdOff = off + 8 + 8 + 8 + 8 + 8 // 5 levels: trak+mdia+minf+stbl+stsd headers
                        if (stsdOff + 8 <= data.size) {
                            val mp4aEnd = findChildBox(data, stsdOff + 8, stsdEnd.coerceAtMost(data.size), "mp4a")
                            if (mp4aEnd != null) {
                                val mp4aOff = stsdOff + 8 + 8 // stsd header(8) + version/entry(4+4)
                                return parseMp4a(data, mp4aOff, mp4aEnd.coerceAtMost(data.size))
                            }
                        }
                        null
                    }
                }
            }
        } ?: return null

        return null
    }

    /** Find a direct child box by type, return its end offset */
    private fun findChildBox(data: ByteArray, start: Int, end: Int, type: String): Int? {
        var off = start
        while (off + 8 <= end) {
            val sz = data.readU32(off).toInt()
            val tp = data.copyOfRange(off + 4, off + 8).decodeToString()
            if (sz < 8) break
            if (tp == type) return (off + sz).coerceAtMost(data.size)
            off += sz
        }
        return null
    }

    /** Parse mp4a box for sample rate and channel count */
    private fun parseMp4a(data: ByteArray, off: Int, end: Int): Streaminfo? {
        // mp4a: [reserved(6)] [dataRefIndex(2)] [version(2)] [revision(2)] [vendor(4)]
        // [channels(2)] [sampleSize(2)] [compressionID(2)] [packetSize(2)] [sampleRate(4)]
        if (off + 28 > data.size) return null
        val channels = data.readU16(off + 16)
        // val sampleSize = data.readU16(off + 18) // usually 16
        val sampleRate = (data.readU32(off + 24) ushr 16).toInt() // upper 16 bits of 32-bit fixed-point
        if (sampleRate <= 0) return null

        return Streaminfo(
            sampleRate = sampleRate,
            channelCount = channels.coerceIn(1, 8),
            bits = 16,
            sampleCount = 0,
            fileLevelMetadataLength = 0,
        )
    }
}
