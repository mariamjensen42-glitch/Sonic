@file:Suppress("SpellCheckingInspection")

package com.cycling.salt.audiotag.format.mp4

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
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * End-to-end MP4/M4A read + write roundtrip on a synthetic file.
 *
 * Upstream only tested atom encoding, never the whole write path, so the writer's box-location
 * logic (moov → udta → meta → ilst) and the `trkn` encoding had no regression coverage. Sonic writes
 * M4A tags in production, hence this test.
 */
@OptIn(UnstableAudioTagApi::class)
class Mp4RoundtripTest {

    @Test
    fun `reads title and cover from synthetic m4a`() {
        runWithFile(buildSyntheticMp4()) { path ->
            val data = AudioTag.read(path, "m4a", ReadStrategy.All).getOrThrow()

            assertNotNull(data.metadatas)
            assertEquals("Old Title", data.metadatas.find { it.key == "TITLE" }?.value)
            assertEquals("Old Artist", data.metadatas.find { it.key == "ARTIST" }?.value)

            assertNotNull(data.pictures)
            assertContentEquals(JPEG_BYTES, data.pictures.first().pictureData)
        }
    }

    @Test
    fun `write roundtrip replaces metadata and keeps cover art`() {
        runWithFile(buildSyntheticMp4()) { srcPath ->
            val before = AudioTag.read(srcPath, "m4a", ReadStrategy.All).getOrThrow()
            val pictures = before.pictures.orEmpty()

            val dstPath = Path(SystemTemporaryDirectory, "mp4_roundtrip_dst_${System.nanoTime()}.m4a")
            try {
                AudioTag.write(
                    srcPath,
                    dstPath,
                    "m4a",
                    WriteOperation.AllMetadata.create(
                        metadatas = listOf(
                            Metadata("TITLE", "New Title"),
                            Metadata("ARTIST", "New Artist"),
                            Metadata("TRACKNUMBER", "3/12"),
                            Metadata("DISCNUMBER", "2/2"),
                        ),
                        pictures = pictures,
                    ),
                ).getOrThrow()

                val after = AudioTag.read(dstPath, "m4a", ReadStrategy.All).getOrThrow()
                assertNotNull(after.metadatas)
                assertEquals("New Title", after.metadatas.find { it.key == "TITLE" }?.value)
                assertEquals("New Artist", after.metadatas.find { it.key == "ARTIST" }?.value)
                // trkn / disk 现在是 iTunes 二进制原子；写了 "3/12" 就必须读回 "3/12"。
                assertEquals("3/12", after.metadatas.find { it.key == "TRACKNUMBER" }?.value)
                assertEquals("2/2", after.metadatas.find { it.key == "DISCNUMBER" }?.value)

                // Cover art survives a tag write when the caller passes the read pictures through.
                assertNotNull(after.pictures)
                assertContentEquals(JPEG_BYTES, after.pictures.first().pictureData)

                // Audio payload must be untouched.
                assertContentEquals(mdatOf(buildSyntheticMp4()), mdatOf(readBytes(dstPath)))
            } finally {
                if (SystemFileSystem.exists(dstPath)) SystemFileSystem.delete(dstPath)
            }
        }
    }

    @Test
    fun `write creates udta meta ilst when the file has no tags`() {
        runWithFile(buildSyntheticMp4(withMetadata = false)) { srcPath ->
            val before = AudioTag.read(srcPath, "m4a", ReadStrategy.All).getOrThrow()
            assertNull(before.metadatas)

            val dstPath = Path(SystemTemporaryDirectory, "mp4_notags_dst_${System.nanoTime()}.m4a")
            try {
                AudioTag.write(
                    srcPath,
                    dstPath,
                    "m4a",
                    WriteOperation.AllMetadata.create(listOf(Metadata("TITLE", "Fresh Title"))),
                ).getOrThrow()

                val after = AudioTag.read(dstPath, "m4a", ReadStrategy.All).getOrThrow()
                assertNotNull(after.metadatas)
                assertEquals("Fresh Title", after.metadatas.find { it.key == "TITLE" }?.value)
                assertContentEquals(mdatOf(buildSyntheticMp4(withMetadata = false)), mdatOf(readBytes(dstPath)))
            } finally {
                if (SystemFileSystem.exists(dstPath)) SystemFileSystem.delete(dstPath)
            }
        }
    }

    @Test
    fun `non numeric track value degrades to zero instead of throwing`() {
        runWithFile(buildSyntheticMp4()) { srcPath ->
            val dstPath = Path(SystemTemporaryDirectory, "mp4_bad_trkn_${System.nanoTime()}.m4a")
            try {
                AudioTag.write(
                    srcPath,
                    dstPath,
                    "m4a",
                    WriteOperation.AllMetadata.create(
                        listOf(Metadata("TRACKNUMBER", "not-a-number"))
                    ),
                ).getOrThrow()

                val after = AudioTag.read(dstPath, "m4a", ReadStrategy.All).getOrThrow()
                // 0 is filtered out by the reader, so this must not be a crash and must not leave a
                // bogus track number behind.
                assertTrue(after.metadatas.orEmpty().none { it.key == "TRACKNUMBER" })
            } finally {
                if (SystemFileSystem.exists(dstPath)) SystemFileSystem.delete(dstPath)
            }
        }
    }

    // ------------------------------------------------------------------
    // Synthetic M4A builder: ftyp + moov(udta(meta(ilst))) + mdat
    // ------------------------------------------------------------------

    private fun buildSyntheticMp4(withMetadata: Boolean = true): ByteArray {
        val ftyp = box(
            "ftyp",
            "M4A ".encodeToByteArray() + bytes(0, 0, 0, 0) + "M4A ".encodeToByteArray()
        )

        val ilst = if (withMetadata) {
            box(
                "ilst",
                item(ATOM_TITLE, buildDataAtom(1, "Old Title".encodeToByteArray())) +
                    item(ATOM_ARTIST, buildDataAtom(1, "Old Artist".encodeToByteArray())) +
                    item(ATOM_COVER, buildDataAtom(13, JPEG_BYTES))
            )
        } else {
            null
        }

        val metaChildren = bytes(0, 0, 0, 0) + (ilst ?: ByteArray(0))
        val meta = box("meta", metaChildren)
        val udta = box("udta", meta)
        val moov = if (withMetadata) box("moov", udta) else box("moov", ByteArray(0))

        val mdat = box("mdat", bytes(1, 2, 3, 4, 5, 6, 7, 8))
        return ftyp + moov + mdat
    }

    private fun box(type: String, content: ByteArray): ByteArray {
        val size = 8 + content.size
        return bytes(
            size ushr 24 and 0xFF,
            size ushr 16 and 0xFF,
            size ushr 8 and 0xFF,
            size and 0xFF
        ) + type.encodeToByteArray().copyOf(4) + content
    }

    private fun item(atomName: ByteArray, dataAtom: ByteArray): ByteArray {
        val size = 8 + dataAtom.size
        return bytes(
            size ushr 24 and 0xFF,
            size ushr 16 and 0xFF,
            size ushr 8 and 0xFF,
            size and 0xFF
        ) + atomName + dataAtom
    }

    private fun buildDataAtom(typeIndex: Int, value: ByteArray): ByteArray {
        val size = 16 + value.size
        return bytes(
            size ushr 24 and 0xFF,
            size ushr 16 and 0xFF,
            size ushr 8 and 0xFF,
            size and 0xFF
        ) + "data".encodeToByteArray() +
            bytes(
                typeIndex ushr 24 and 0xFF,
                typeIndex ushr 16 and 0xFF,
                typeIndex ushr 8 and 0xFF,
                typeIndex and 0xFF
            ) + bytes(0, 0, 0, 0) + value
    }

    private fun mdatOf(data: ByteArray): ByteArray {
        val off = indexOfType(data, "mdat")
        if (off < 0) return ByteArray(0)
        val size = readU32(data, off - 4).toInt()
        return data.copyOfRange(off + 4, (off - 4 + size).coerceAtMost(data.size))
    }

    private fun indexOfType(data: ByteArray, type: String): Int {
        val target = type.encodeToByteArray()
        for (i in 0..data.size - 4) {
            var matches = true
            for (j in target.indices) {
                if (data[i + j] != target[j]) {
                    matches = false
                    break
                }
            }
            if (matches) return i
        }
        return -1
    }

    private fun readU32(data: ByteArray, off: Int): Long =
        ((data[off].toLong() and 0xFF) shl 24) or
            ((data[off + 1].toLong() and 0xFF) shl 16) or
            ((data[off + 2].toLong() and 0xFF) shl 8) or
            (data[off + 3].toLong() and 0xFF)

    private fun readBytes(path: Path): ByteArray =
        SystemFileSystem.source(path).buffered().use { it.readByteArray() }

    private fun runWithFile(data: ByteArray, block: (Path) -> Unit) {
        val path = Path(SystemTemporaryDirectory, "mp4_test_${System.nanoTime()}.m4a")
        try {
            SystemFileSystem.sink(path).buffered().use { it.write(data) }
            block(path)
        } finally {
            if (SystemFileSystem.exists(path)) SystemFileSystem.delete(path)
        }
    }
}

private fun bytes(vararg values: Int): ByteArray =
    ByteArray(values.size) { i -> values[i].toByte() }

// MP4 atom names use a raw 0xA9 byte for the "©" prefix, not its 2-byte UTF-8 encoding.
private val ATOM_TITLE = bytes(0xA9, 'n'.code, 'a'.code, 'm'.code)
private val ATOM_ARTIST = bytes(0xA9, 'A'.code, 'R'.code, 'T'.code)
private val ATOM_COVER = "covr".encodeToByteArray()

private val JPEG_BYTES: ByteArray = bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46)
