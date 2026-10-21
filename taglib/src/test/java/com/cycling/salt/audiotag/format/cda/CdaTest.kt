/*
 * Salt Audio Tag
 * Copyright (C) 2025 Moriafly
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA
 */

package com.cycling.salt.audiotag.format.cda

import com.cycling.salt.audiotag.AudioTag
import com.cycling.salt.audiotag.UnstableAudioTagApi
import com.cycling.salt.audiotag.rw.ReadStrategy
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.write
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Synthetic CDA test.
 *
 * The upstream version of this test read `E:\Track03.cda` from a developer's machine, so it could
 * never pass anywhere else. A CDA file is a 44-byte RIFF chunk, which is easy to build in memory.
 */
@OptIn(UnstableAudioTagApi::class)
class CdaTest {

    @Test
    fun `reads synthetic CDA track and disc info`() {
        val bytes = buildSyntheticCda(
            trackNumber = 3,
            discId = 123,
            totalDurationFrames = 75 * 180 // 180 seconds, 75 CD frames per second
        )
        val path = Path(SystemTemporaryDirectory, "cda_test_${System.nanoTime()}.cda")

        try {
            SystemFileSystem.sink(path).buffered().use { it.write(bytes) }

            val data = AudioTag.read(
                path = path,
                extension = "cda",
                strategy = ReadStrategy.All,
            ).getOrThrow()

            assertNotNull(data.streaminfo)
            assertEquals(44_100, data.streaminfo?.sampleRate)
            assertEquals(2, data.streaminfo?.channelCount)
            assertEquals(180L * 44_100L, data.streaminfo?.sampleCount)

            assertNotNull(data.metadatas)
            assertEquals(
                "123",
                data.metadatas?.find { it.key == "DISCID" }?.value
            )
            assertEquals(
                "3",
                data.metadatas?.find { it.key == "TRACKNUMBER" }?.value
            )
        } finally {
            if (SystemFileSystem.exists(path)) SystemFileSystem.delete(path)
        }
    }

    /** Builds the 44-byte RIFF/CDDA block a `.cda` file consists of. */
    private fun buildSyntheticCda(
        trackNumber: Int,
        discId: Int,
        totalDurationFrames: Int,
    ): ByteArray = ByteArray(44).also { b ->
        "RIFF".encodeToByteArray().copyInto(b, 0)
        writeLe32(b, 4, 36)
        "CDDA".encodeToByteArray().copyInto(b, 8)
        "fmt ".encodeToByteArray().copyInto(b, 12)
        writeLe32(b, 16, 24)
        writeLe16(b, 20, 1) // format version
        writeLe16(b, 22, trackNumber)
        writeLe32(b, 24, discId)
        writeLe32(b, 28, 0) // start offset in frames
        writeLe32(b, 32, totalDurationFrames)
        // Bytes 36..39 are redundant position info and are skipped by the reader.
        b[40] = 0 // frames
        b[41] = 0 // seconds
        b[42] = 3 // minutes
        // b[43] is unused padding, the reader only requires a 44-byte block.
    }

    private fun writeLe16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = (value ushr 8 and 0xFF).toByte()
    }

    private fun writeLe32(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = (value ushr 8 and 0xFF).toByte()
        data[offset + 2] = (value ushr 16 and 0xFF).toByte()
        data[offset + 3] = (value ushr 24 and 0xFF).toByte()
    }
}
