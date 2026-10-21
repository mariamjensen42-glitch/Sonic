@file:Suppress("SpellCheckingInspection")

package com.cycling.salt.audiotag.format.flac

import com.cycling.salt.audiotag.AudioTag
import com.cycling.salt.audiotag.UnstableAudioTagApi
import com.cycling.salt.audiotag.rw.ReadStrategy
import com.cycling.salt.audiotag.rw.WriteOperation
import com.cycling.salt.audiotag.rw.data.Metadata
import com.cycling.salt.audiotag.rw.data.Picture
import com.cycling.salt.audiotag.rw.data.Picture.PictureType
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.write
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Self-contained FLAC test using synthetic data.
 *
 * All test data is constructed in memory — no external files required.
 */
@OptIn(UnstableAudioTagApi::class)
class FlacTest {

    // ------------------------------------------------------------------
    // T-01: Basic read — streaminfo + metadata
    // ------------------------------------------------------------------

    @Test
    fun `read streaminfo and metadata from synthetic FLAC`() {
        val bytes = buildSyntheticFlac(
            metadatas = listOf(
                Metadata("TITLE", "Test Title"),
                Metadata("ARTIST", "Test Artist"),
            ),
        )
        runWithTempFile(bytes) { path ->
            val result = AudioTag.read(
                path = path, extension = "flac", strategy = ReadStrategy.All,
            )
            assertTrue(result.isSuccess)
            val data = result.getOrThrow()

            assertNotNull(data.streaminfo)
            assertEquals(44100, data.streaminfo?.sampleRate)
            assertEquals(2, data.streaminfo?.channelCount)
            assertEquals(16, data.streaminfo?.bits)

            assertNotNull(data.metadatas)
            assertEquals("Test Title", data.metadatas?.find { it.key == "TITLE" }?.value)
            assertEquals("Test Artist", data.metadatas?.find { it.key == "ARTIST" }?.value)
        }
    }

    // ------------------------------------------------------------------
    // T-02: Read with empty metadata
    // ------------------------------------------------------------------

    @Test
    fun `read synthetic FLAC with no metadata`() {
        val bytes = buildSyntheticFlac(metadatas = emptyList())
        runWithTempFile(bytes) { path ->
            val result = AudioTag.read(
                path = path, extension = "flac", strategy = ReadStrategy.All,
            )
            assertTrue(result.isSuccess)
            val data = result.getOrThrow()

            assertNotNull(data.streaminfo)
            assertTrue(data.metadatas == null || data.metadatas!!.isEmpty())
        }
    }

    // ------------------------------------------------------------------
    // T-03: Write metadata round-trip
    // ------------------------------------------------------------------

    @Test
    fun `write metadata round-trip preserves new values`() {
        val srcBytes = buildSyntheticFlac(
            metadatas = listOf(Metadata("TITLE", "Original")),
        )
        val tempDir = SystemTemporaryDirectory
        val srcPath = Path(tempDir, "flac_test_src.flac")
        val dstPath = Path(tempDir, "flac_test_dst.flac")

        try {
            SystemFileSystem.sink(srcPath).buffered().use { it.write(srcBytes) }

            val newMetas = listOf(
                Metadata("TITLE", "New Title"),
                Metadata("ARTIST", "New Artist"),
            )
            val writeResult = AudioTag.write(
                src = srcPath, dst = dstPath, extension = "flac",
                WriteOperation.AllMetadata.create(newMetas),
            )
            assertTrue(writeResult.isSuccess)

            val readResult = AudioTag.read(
                path = dstPath, extension = "flac", strategy = ReadStrategy.All,
            )
            assertTrue(readResult.isSuccess)
            val data = readResult.getOrThrow()

            assertNotNull(data.metadatas)
            assertEquals("New Title", data.metadatas?.find { it.key == "TITLE" }?.value)
            assertEquals("New Artist", data.metadatas?.find { it.key == "ARTIST" }?.value)
        } finally {
            if (SystemFileSystem.exists(srcPath)) SystemFileSystem.delete(srcPath)
            if (SystemFileSystem.exists(dstPath)) SystemFileSystem.delete(dstPath)
        }
    }

    @Test
    fun `write empty metadata clear all tags`() {
        val srcBytes = buildSyntheticFlac(
            metadatas = listOf(Metadata("TITLE", "Original")),
        )
        val tempDir = SystemTemporaryDirectory
        val srcPath = Path(tempDir, "flac_test_src2.flac")
        val dstPath = Path(tempDir, "flac_test_dst2.flac")

        try {
            SystemFileSystem.sink(srcPath).buffered().use { it.write(srcBytes) }

            val writeResult = AudioTag.write(
                src = srcPath, dst = dstPath, extension = "flac",
                WriteOperation.AllMetadata.create(emptyList()),
            )
            assertTrue(writeResult.isSuccess)

            val readResult = AudioTag.read(
                path = dstPath, extension = "flac", strategy = ReadStrategy.All,
            )
            assertTrue(readResult.isSuccess)
            val data = readResult.getOrThrow()

            assertTrue(data.metadatas == null || data.metadatas!!.isEmpty())
        } finally {
            if (SystemFileSystem.exists(srcPath)) SystemFileSystem.delete(srcPath)
            if (SystemFileSystem.exists(dstPath)) SystemFileSystem.delete(dstPath)
        }
    }

    // ------------------------------------------------------------------
    // T-04: Picture read
    // ------------------------------------------------------------------

    @Test
    fun `read picture from synthetic FLAC`() {
        val jpegData = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0.toByte(), 0x10.toByte())
        val bytes = buildSyntheticFlac(
            metadatas = listOf(Metadata("TITLE", "With Cover")),
            pictures = listOf(
                Picture(
                    pictureType = PictureType.FrontCover,
                    mediaType = "image/jpeg",
                    description = "Front Cover",
                    width = 0, height = 0, colorDepth = 0, colorsNumber = 0,
                    pictureData = jpegData,
                ),
            ),
        )
        runWithTempFile(bytes) { path ->
            val result = AudioTag.read(
                path = path, extension = "flac", strategy = ReadStrategy.All,
            )
            assertTrue(result.isSuccess)
            val data = result.getOrThrow()

            assertNotNull(data.pictures)
            assertTrue(data.pictures!!.isNotEmpty())
            assertEquals(PictureType.FrontCover, data.pictures!!.first().pictureType)
            assertTrue(data.pictures!!.first().pictureData.contentEquals(jpegData))
        }
    }

    // ------------------------------------------------------------------
    // T-05: Write with pictures round-trip
    // ------------------------------------------------------------------

    @Test
    fun `write pictures round-trip`() {
        val srcBytes = buildSyntheticFlac(
            metadatas = listOf(Metadata("TITLE", "Original")),
        )
        val tempDir = SystemTemporaryDirectory
        val srcPath = Path(tempDir, "flac_pic_src.flac")
        val dstPath = Path(tempDir, "flac_pic_dst.flac")

        try {
            SystemFileSystem.sink(srcPath).buffered().use { it.write(srcBytes) }

            val jpegData = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0.toByte(), 0x10.toByte(), 0x0A.toByte())
            val pic = Picture(
                pictureType = PictureType.FrontCover,
                mediaType = "image/jpeg",
                description = "New Cover",
                width = 0, height = 0, colorDepth = 0, colorsNumber = 0,
                pictureData = jpegData,
            )

            val writeResult = AudioTag.write(
                src = srcPath, dst = dstPath, extension = "flac",
                WriteOperation.AllMetadata.create(
                    metadatas = listOf(Metadata("TITLE", "With Cover"), Metadata("ARTIST", "Artist")),
                    pictures = listOf(pic),
                ),
            )
            assertTrue(writeResult.isSuccess)

            val readResult = AudioTag.read(
                path = dstPath, extension = "flac", strategy = ReadStrategy.All,
            )
            assertTrue(readResult.isSuccess)
            val data = readResult.getOrThrow()

            assertEquals("With Cover", data.metadatas?.find { it.key == "TITLE" }?.value)
            assertNotNull(data.pictures)
            assertEquals(1, data.pictures!!.size)
            assertEquals(PictureType.FrontCover, data.pictures!!.first().pictureType)
            assertTrue(data.pictures!!.first().pictureData.contentEquals(jpegData))
        } finally {
            if (SystemFileSystem.exists(srcPath)) SystemFileSystem.delete(srcPath)
            if (SystemFileSystem.exists(dstPath)) SystemFileSystem.delete(dstPath)
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Write [data] to a temp file, run [block], then delete. */
    private fun runWithTempFile(data: ByteArray, block: (Path) -> Unit) {
        val path = Path(SystemTemporaryDirectory, "flac_test_${System.nanoTime()}.flac")
        try {
            SystemFileSystem.sink(path).buffered().use { it.write(data) }
            block(path)
        } finally {
            if (SystemFileSystem.exists(path)) SystemFileSystem.delete(path)
        }
    }

    // ------------------------------------------------------------------
    // Synthetic FLAC builder
    // ------------------------------------------------------------------

    private fun buildSyntheticFlac(
        metadatas: List<Metadata>,
        pictures: List<Picture>? = null,
    ): ByteArray {
        val data = mutableListOf<Byte>()

        // fLaC marker
        data.addAll(listOf(0x66, 0x4C, 0x61, 0x43).map { it.toByte() })

        // STREAMINFO block data (34 bytes)
        val siStreaminfo = mutableListOf<Byte>()
        // minBlockSize = 4096
        siStreaminfo.addAll(listOf(0x10, 0x00).map { it.toByte() })
        // maxBlockSize = 4096
        siStreaminfo.addAll(listOf(0x10, 0x00).map { it.toByte() })
        // minFrameSize = 0 (24-bit)
        siStreaminfo.addAll(listOf(0x00, 0x00, 0x00).map { it.toByte() })
        // maxFrameSize = 0 (24-bit)
        siStreaminfo.addAll(listOf(0x00, 0x00, 0x00).map { it.toByte() })
        // sampleRate=44100: bits 19-12 = 0x0A, bits 11-4 = 0xC4, bits 3-0 = 0x40 | channel(1) = 0x42
        siStreaminfo.addAll(listOf(0x0A, 0xC4, 0x42, 0xF0).map { it.toByte() })
        // sampleCount low 32 bits = 0
        siStreaminfo.addAll(listOf(0x00, 0x00, 0x00, 0x00).map { it.toByte() })
        // MD5 = 16 zero bytes
        siStreaminfo.addAll(ByteArray(16).toList())

        val siBytes = siStreaminfo.toByteArray()

        // Streaminfo block header (type 0, not last)
        val siHeader = mutableListOf<Byte>()
        siHeader.add(0x00) // type 0, isLast = false
        siHeader.add(((siBytes.size ushr 16) and 0xFF).toByte())
        siHeader.add(((siBytes.size ushr 8) and 0xFF).toByte())
        siHeader.add((siBytes.size and 0xFF).toByte())

        data.addAll(siHeader)
        data.addAll(siBytes.toList())

        // --- VorbisComment block ---
        val vcData = mutableListOf<Byte>()
        val vendor = "Salt Audio Tag Test"
        val vendorBytes = vendor.encodeToByteArray()
        // vendor_length LE
        vcData.addAll(writeLe32(vendorBytes.size))
        vcData.addAll(vendorBytes.toList())
        // user_comment_list_length LE
        vcData.addAll(writeLe32(metadatas.size))
        for (meta in metadatas) {
            val comment = "${meta.key}=${meta.value}"
            val cb = comment.encodeToByteArray()
            vcData.addAll(writeLe32(cb.size))
            vcData.addAll(cb.toList())
        }

        val hasPictures = pictures != null && pictures.isNotEmpty()
        val vcIsLast = !hasPictures

        val vcHeader = mutableListOf<Byte>()
        vcHeader.add(((if (vcIsLast) 0x80 else 0x00) or 0x04).toByte()) // type 4
        vcHeader.add(((vcData.size ushr 16) and 0xFF).toByte())
        vcHeader.add(((vcData.size ushr 8) and 0xFF).toByte())
        vcHeader.add((vcData.size and 0xFF).toByte())

        data.addAll(vcHeader)
        data.addAll(vcData)

        // --- Picture blocks ---
        if (hasPictures) {
            for ((index, pic) in pictures!!.withIndex()) {
                val picTypeInt = pictureTypeToFlacInt(pic.pictureType)
                val mimeBytes = pic.mediaType.encodeToByteArray()
                val descBytes = pic.description.encodeToByteArray()

                val picData = mutableListOf<Byte>()

                // picture_type BE
                picData.addAll(writeBe32(picTypeInt))
                // mime_type_length BE
                picData.addAll(writeBe32(mimeBytes.size))
                picData.addAll(mimeBytes.toList())
                // description_length BE
                picData.addAll(writeBe32(descBytes.size))
                picData.addAll(descBytes.toList())
                // width, height, colorDepth, colorsNumber (all BE)
                picData.addAll(writeBe32(pic.width))
                picData.addAll(writeBe32(pic.height))
                picData.addAll(writeBe32(pic.colorDepth))
                picData.addAll(writeBe32(pic.colorsNumber))
                // picture_data_length BE
                picData.addAll(writeBe32(pic.pictureData.size))
                picData.addAll(pic.pictureData.toList())

                val picBlockData = picData.toByteArray()
                val isLastPic = index == pictures!!.lastIndex

                val picHeader = mutableListOf<Byte>()
                picHeader.add(((if (isLastPic) 0x80 else 0x00) or 0x06).toByte()) // type 6
                picHeader.add(((picBlockData.size ushr 16) and 0xFF).toByte())
                picHeader.add(((picBlockData.size ushr 8) and 0xFF).toByte())
                picHeader.add((picBlockData.size and 0xFF).toByte())

                data.addAll(picHeader)
                data.addAll(picBlockData.toList())
            }
        }

        // Dummy audio data
        data.addAll(ByteArray(100) { 0 }.toList())

        return data.toByteArray()
    }

    companion object {
        private fun writeLe32(value: Int): List<Byte> = listOf(
            (value and 0xFF).toByte(),
            ((value ushr 8) and 0xFF).toByte(),
            ((value ushr 16) and 0xFF).toByte(),
            ((value ushr 24) and 0xFF).toByte(),
        )

        private fun writeBe32(value: Int): List<Byte> = listOf(
            ((value ushr 24) and 0xFF).toByte(),
            ((value ushr 16) and 0xFF).toByte(),
            ((value ushr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte(),
        )

        private fun pictureTypeToFlacInt(type: PictureType): Int = when (type) {
            PictureType.Other -> 0
            PictureType.PngFileIcon32x32 -> 1
            PictureType.GeneralFileIcon -> 2
            PictureType.FrontCover -> 3
            PictureType.BackCover -> 4
            PictureType.LinerNotesPage -> 5
            PictureType.MediaLabel -> 6
            PictureType.Lead -> 7
            PictureType.Artist -> 8
            PictureType.Conductor -> 9
            PictureType.Band -> 10
            PictureType.Composer -> 11
            PictureType.Lyricist -> 12
            PictureType.RecordingLocation -> 13
            PictureType.DuringRecording -> 14
            PictureType.DuringPerformance -> 15
            PictureType.MovieScreenCapture -> 16
            PictureType.BrightColoredFish -> 17
            PictureType.Illustration -> 18
            PictureType.BandLogo -> 19
            PictureType.PublisherLogotype -> 20
            PictureType.Unknown -> 127
        }
    }
}
