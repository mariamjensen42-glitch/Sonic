@file:Suppress("SpellCheckingInspection")

package com.cycling.salt.audiotag.format.mp4

import com.cycling.salt.audiotag.UnstableAudioTagApi
import com.cycling.salt.audiotag.rw.WriteOperation
import com.cycling.salt.audiotag.rw.Writer
import com.cycling.salt.audiotag.util.SystemFileSystemUtil
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.io.write

/**
 * MP4/M4A ISOBMFF metadata writer.
 *
 * Strategy:
 * 1. Read existing file and locate the moov box
 * 2. Find the ilst metadata container within moov → udta → meta → ilst
 * 3. Replace existing ilst content with new metadata from [WriteOperation]
 * 4. Rebuild moov with updated sizes for all parent boxes
 * 5. Write new moov to temp file and atomic-replace the original
 */
@OptIn(UnstableAudioTagApi::class, kotlin.uuid.ExperimentalUuidApi::class)
class Mp4Writer : Writer {

    @UnstableAudioTagApi
    override fun write(src: Path, dst: Path, vararg operation: WriteOperation) {
        val tmp = SystemFileSystemUtil.tempFilePath()
        try {
            val fileBytes = SystemFileSystem.source(src).buffered().use { source ->
                source.readByteArray()
            }
            if (fileBytes.size < 12) {
                // Not a valid MP4
                if (SystemFileSystem.exists(dst)) SystemFileSystem.delete(dst)
                copyFile(src, dst)
                return
            }

            val op = operation.find { it is WriteOperation.AllMetadata } as WriteOperation.AllMetadata?
            val hasMetadata = op != null && (op.metadatas.isNotEmpty() || (op.pictures != null && op.pictures.isNotEmpty()))
            if (op == null || !hasMetadata) {
                // No metadata or pictures to write — just copy
                if (SystemFileSystem.exists(dst)) SystemFileSystem.delete(dst)
                copyFile(src, dst)
                return
            }

            // Locate moov box in the file data
            val moovStart = findMoovInArray(fileBytes)
            if (moovStart < 0) {
                // No moov found — just copy
                if (SystemFileSystem.exists(dst)) SystemFileSystem.delete(dst)
                copyFile(src, dst)
                return
            }

            val oldMoovSize = readU32BE(fileBytes, moovStart).toInt()
            val moovHeaderSize = if (oldMoovSize == 1) 16 else 8

            // Parse the moov box to find where ilst lives
            val moovData = fileBytes.copyOfRange(moovStart, moovStart + oldMoovSize.coerceAtMost(fileBytes.size - moovStart))
            val ilstInfo = findIlstInMoov(moovData) // returns (ilstOffset relative to moov, ilstSize)

            // Build new ilst atom with metadata and pictures
            val newIlst = Mp4Constants.buildIlstAtom(op.metadatas, op.pictures)

            // Build the new moov by replacing the ilst portion
            val newMoov = if (ilstInfo != null) {
                val (ilstOffset, oldIlstSize) = ilstInfo
                // Rebuild moov with new ilst
                rebuildMoov(moovData, ilstOffset, oldIlstSize, newIlst)
            } else {
                // No existing ilst — add ilst container (may need to create udta/meta/ilst path)
                // For simplicity: if no ilst exists, append ilst to the end of moov data
                addIlstToMoov(moovData, newIlst)
            }

            // Write temp file: pre-moov + new moov + post-moov
            val moovEnd = moovStart + oldMoovSize
            SystemFileSystem.sink(tmp).buffered().use { sink ->
                sink.write(fileBytes.copyOfRange(0, moovStart))
                sink.write(newMoov)
                if (moovEnd < fileBytes.size) {
                    sink.write(fileBytes.copyOfRange(moovEnd, fileBytes.size))
                }
            }

            // Atomic replace
            if (SystemFileSystem.exists(dst)) SystemFileSystem.delete(dst)
            SystemFileSystem.atomicMove(tmp, dst)
        } finally {
            if (SystemFileSystem.exists(tmp)) SystemFileSystem.delete(tmp)
        }
    }

    /**
     * Find moov box start offset in the file byte array.
     * Returns the offset (index of moov header) or -1 if not found.
     */
    private fun findMoovInArray(data: ByteArray): Int {
        var off = 0
        while (off + 8 <= data.size) {
            val boxSize = readU32BE(data, off).toInt()
            val boxType = data.copyOfRange(off + 4, off + 8).decodeToString()
            if (boxSize == 0) break
            if (boxSize == 1 && off + 16 <= data.size) {
                val high = readU32BE(data, off + 8).toInt()
                val low = readU32BE(data, off + 12).toInt()
                val extSize = (high.toLong() shl 32) or (low.toLong() and 0xFFFFFFFFL)
                val realSize = if (extSize <= Int.MAX_VALUE) extSize.toInt() else data.size - off
                if (boxType == "moov") return off
                off += realSize
            } else {
                if (boxType == "moov") return off
                off += boxSize
            }
        }
        return -1
    }

    /**
     * Find ilst box within moov data.
     *
     * Sonic fix: offsets are absolute inside [moovData] (the old code mixed "relative to udta" and
     * "relative to moov" offsets, and never looked inside `meta`, which is a full box with a
     * 4-byte version/flags prefix). That made the writer miss an existing ilst on real M4A files.
     */
    private fun findIlstInMoov(moovData: ByteArray): Pair<Int, Int>? {
        val moovHdrSize = if (readU32BE(moovData, 0) == 1L) 16 else 8
        return findIlstInRange(moovData, moovHdrSize, moovData.size)
    }

    /** Recursively search moov children (`udta` → `meta` → `ilst`), returning absolute offsets. */
    private fun findIlstInRange(data: ByteArray, start: Int, end: Int): Pair<Int, Int>? {
        var off = start
        val limit = end.coerceAtMost(data.size)
        while (off + 8 <= limit) {
            val boxSize = readU32BE(data, off).toInt()
            val boxType = data.copyOfRange(off + 4, off + 8).decodeToString()
            if (boxSize < 8) break
            val boxEnd = off + boxSize
            if (boxEnd > limit) break

            if (boxType == "ilst") return off to boxSize

            val listChild: Pair<Int, Int>? = when (boxType) {
                "udta" -> findIlstInRange(data, off + 8, boxEnd)
                // meta is a full box: 4-byte version/flags come before its children.
                "meta" -> findIlstInRange(data, off + 12, boxEnd)
                else -> null
            }
            if (listChild != null) return listChild

            off += boxSize
        }
        return null
    }

    /**
     * Replace an ilst box within the moov data with a new one,
     * recalculating all parent box sizes.
     */
    private fun rebuildMoov(
        moovData: ByteArray,
        ilstOffset: Int,
        oldIlstSize: Int,
        newIlst: ByteArray,
    ): ByteArray {
        if (ilstOffset < 0 || ilstOffset + oldIlstSize > moovData.size) return moovData

        // `ilstOffset` is absolute inside moovData (see findIlstInMoov).
        val newMoov = moovData.copyOfRange(0, ilstOffset) +
            newIlst +
            moovData.copyOfRange(ilstOffset + oldIlstSize, moovData.size)

        val sizeDiff = newIlst.size - oldIlstSize

        // Update moov total size.
        val moovTotalSize = readU32BE(newMoov, 0).toInt() + sizeDiff
        writeU32BEAt(newMoov, 0, moovTotalSize)

        // Update every ancestor box (udta / meta) whose content contains the old ilst position.
        // The offsets do not shift because the replacement starts exactly at ilstOffset.
        val moovHdrSize = if (readU32BE(newMoov, 0) == 1L) 16 else 8
        val ancestors = mutableListOf<Int>()
        collectAncestors(newMoov, moovHdrSize, newMoov.size, ilstOffset, ancestors)
        for (ancestorOffset in ancestors) {
            val ancestorSize = readU32BE(newMoov, ancestorOffset).toInt()
            if (ancestorSize >= 8) {
                writeU32BEAt(newMoov, ancestorOffset, ancestorSize + sizeDiff)
            }
        }

        return newMoov
    }

    /** Collect offsets of boxes whose content range contains [target]. */
    private fun collectAncestors(
        data: ByteArray,
        start: Int,
        end: Int,
        target: Int,
        out: MutableList<Int>,
    ) {
        var off = start
        val limit = end.coerceAtMost(data.size)
        while (off + 8 <= limit) {
            val boxSize = readU32BE(data, off).toInt()
            if (boxSize < 8) break
            val boxEnd = (off + boxSize).coerceAtMost(data.size)
            if (target in (off + 8) until boxEnd) {
                out.add(off)
                val boxType = data.copyOfRange(off + 4, off + 8).decodeToString()
                val childStart = if (boxType == "meta") off + 12 else off + 8
                collectAncestors(data, childStart, boxEnd, target, out)
                return
            }
            off += boxSize
        }
    }

    /**
     * Add a new ilst box to moov, ensuring the moov → udta → meta → ilst path exists.
     *
     * Sonic fix: the upstream version had mixed offsets and left stale parent sizes; this version
     * inserts at unambiguous positions and fixes moov/udta/meta sizes in place.
     */
    private fun addIlstToMoov(moovData: ByteArray, newIlst: ByteArray): ByteArray {
        val moovHdrSize = if (readU32BE(moovData, 0) == 1L) 16 else 8
        val udta = findChildBox(moovData, moovHdrSize, moovData.size, "udta")

        if (udta == null) {
            // No udta at all: append udta → meta → ilst to the end of moov.
            val metaBox = Mp4Constants.buildBox("meta", byteArrayOf(0, 0, 0, 0) + newIlst)
            val udtaBox = Mp4Constants.buildBox("udta", metaBox)
            val newMoov = moovData.copyOfRange(0, moovData.size) + udtaBox
            writeU32BEAt(newMoov, 0, readU32BE(moovData, 0).toInt() + udtaBox.size)
            return newMoov
        }

        val meta = findChildBox(moovData, udta.first + 8, udta.first + udta.second, "meta")
        return if (meta == null) {
            // udta exists but has no meta: append meta → ilst at the end of udta.
            val metaBox = Mp4Constants.buildBox("meta", byteArrayOf(0, 0, 0, 0) + newIlst)
            val insert = udta.first + udta.second
            val newMoov = moovData.copyOfRange(0, insert) +
                metaBox +
                moovData.copyOfRange(insert, moovData.size)
            writeU32BEAt(newMoov, 0, readU32BE(moovData, 0).toInt() + metaBox.size)
            writeU32BEAt(newMoov, udta.first, udta.second + metaBox.size)
            newMoov
        } else {
            // meta exists: insert ilst right after meta's version/flags (meta is a full box).
            val insert = meta.first + 8 + 4
            val newMoov = moovData.copyOfRange(0, insert) +
                newIlst +
                moovData.copyOfRange(insert, moovData.size)
            writeU32BEAt(newMoov, 0, readU32BE(moovData, 0).toInt() + newIlst.size)
            writeU32BEAt(newMoov, udta.first, udta.second + newIlst.size)
            writeU32BEAt(newMoov, meta.first, meta.second + newIlst.size)
            newMoov
        }
    }

    /** Find a direct child box of [type] whose header lies in `[start, end)`. */
    private fun findChildBox(
        data: ByteArray,
        start: Int,
        end: Int,
        type: String,
    ): Pair<Int, Int>? {
        var off = start
        val limit = end.coerceAtMost(data.size)
        while (off + 8 <= limit) {
            val boxSize = readU32BE(data, off).toInt()
            if (boxSize < 8) break
            val boxType = data.copyOfRange(off + 4, off + 8).decodeToString()
            if (boxType == type) return off to boxSize
            off += boxSize
        }
        return null
    }

    /** Read 4-byte big-endian unsigned 32-bit integer from byte array. */
    private fun readU32BE(data: ByteArray, off: Int): Long =
        ((data[off].toLong() and 0xFF) shl 24) or
            ((data[off + 1].toLong() and 0xFF) shl 16) or
            ((data[off + 2].toLong() and 0xFF) shl 8) or
            (data[off + 3].toLong() and 0xFF)

    /** Write a 32-bit big-endian unsigned integer into byte array at offset. */
    private fun writeU32BEAt(data: ByteArray, off: Int, value: Int) {
        data[off] = (value ushr 24 and 0xFF).toByte()
        data[off + 1] = (value ushr 16 and 0xFF).toByte()
        data[off + 2] = (value ushr 8 and 0xFF).toByte()
        data[off + 3] = (value and 0xFF).toByte()
    }

    /** Copy a file from [src] to [dst] using buffered I/O. */
    private fun copyFile(src: Path, dst: Path) {
        SystemFileSystem.source(src).buffered().use { source ->
            SystemFileSystem.sink(dst).buffered().use { sink ->
                val buf = source.readByteArray()
                sink.write(buf)
            }
        }
    }
}
