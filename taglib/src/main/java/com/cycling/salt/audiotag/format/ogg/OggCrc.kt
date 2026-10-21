@file:Suppress("SpellCheckingInspection")

package com.cycling.salt.audiotag.format.ogg

/**
 * OGG page CRC-32 computation.
 *
 * Per RFC 7845 §3, the CRC-32 uses polynomial 0x04C11DB7 in "forward" (non-reflected)
 * direction with initial value 0x00000000 and no final XOR.
 *
 * The CRC is computed over the entire page content with the checksum field
 * (bytes 22–25 of the page header) treated as zero.
 */
internal object OggCrc {

    private val CRC_TABLE = genCrcTable()

    private fun genCrcTable(): IntArray {
        val table = IntArray(256)
        for (i in 0..255) {
            var crc = i shl 24
            repeat(8) {
                crc = if ((crc and Int.MIN_VALUE) != 0) {
                    (crc shl 1) xor 0x04C11DB7
                } else {
                    crc shl 1
                }
            }
            table[i] = crc
        }
        return table
    }

    fun computePageCrc(
        header: ByteArray,
        segmentTable: ByteArray,
        data: ByteArray,
    ): Int {
        var crc = 0

        // Bytes 0–21 of header
        for (i in 0..21) {
            val b = if (i < header.size) header[i].toInt() and 0xFF else 0
            crc = (crc shl 8) xor CRC_TABLE[((crc ushr 24) and 0xFF) xor b]
        }

        // 4 zero bytes for the checksum field (bytes 22–25)
        repeat(4) {
            crc = (crc shl 8) xor CRC_TABLE[(crc ushr 24) and 0xFF]
        }

        // Byte 26: page_segments count
        val segCount = if (header.size > 26) header[26].toInt() and 0xFF else 0
        crc = (crc shl 8) xor CRC_TABLE[((crc ushr 24) and 0xFF) xor segCount]

        // Segment table
        for (b in segmentTable) {
            crc = (crc shl 8) xor CRC_TABLE[((crc ushr 24) and 0xFF) xor (b.toInt() and 0xFF)]
        }

        // Page data
        for (b in data) {
            crc = (crc shl 8) xor CRC_TABLE[((crc ushr 24) and 0xFF) xor (b.toInt() and 0xFF)]
        }

        return crc
    }
}
