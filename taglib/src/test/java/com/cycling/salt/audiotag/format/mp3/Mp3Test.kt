@file:Suppress("SpellCheckingInspection")

package com.cycling.salt.audiotag.format.mp3

import com.cycling.salt.audiotag.AudioTag
import com.cycling.salt.audiotag.UnstableAudioTagApi
import com.cycling.salt.audiotag.rw.ReadStrategy
import com.cycling.salt.audiotag.rw.WriteOperation
import com.cycling.salt.audiotag.rw.data.Metadata
import kotlin.test.Test

@OptIn(UnstableAudioTagApi::class)
class Mp3Test {
    // Replace with path to a test MP3 file on your system
    private val path = kotlinx.io.files.Path("test.mp3")

    @Test
    fun testRead() {
        // This test requires an actual MP3 file with ID3v2 tags.
        // Replace path above with a real file path.
        println("MP3 test - provide a valid MP3 file path to run")
    }

    @Test
    fun testReadWriteRoundtrip() {
        // Example usage pattern (not auto-run without an MP3 file):
        //
        // val result = AudioTag.read(
        //     path = path,
        //     extension = "mp3",
        //     strategy = ReadStrategy.All
        // )
        //
        // result.onSuccess { audioTag ->
        //     println("Stream info: ${audioTag.streaminfo}")
        //     audioTag.metadatas?.forEach { meta ->
        //         println("${meta.key} = ${meta.value}")
        //     }
        //     audioTag.pictures?.forEach { pic ->
        //         println("Picture: ${pic.pictureType}, size=${pic.pictureData.size}")
        //     }
        // }
        //
        // // Write test
        // AudioTag.write(
        //     src = path,
        //     dst = kotlinx.io.files.Path("output_test.mp3"),
        //     extension = "mp3",
        //     WriteOperation.AllMetadata.create(
        //         metadatas = listOf(
        //             Metadata("TITLE", "Test Title"),
        //             Metadata("ARTIST", "Test Artist"),
        //             Metadata("ALBUM", "Test Album")
        //         )
        //     )
        // )
    }

    @Test
    fun testParseFrameRoundtrip() {
        // Test text encoding
        val value = "Test Artist"
        val data = buildTextFrameData(value, TextEncoding.UTF_8)
        assert(data.isNotEmpty()) { "Text frame data should not be empty" }

        val encoding = data[0]
        val decoded = decodeId3Text(data.copyOfRange(1, data.size), encoding)
        assert(decoded == value) { "Roundtrip failed: '$decoded' != '$value'" }
    }

    @Test
    fun testSyncsafeInt() {
        val original = 123456
        val bytes = writeSyncsafeInt(original)

        // Read back
        var value = 0
        for (i in 0 until 4) {
            value = (value shl 7) or (bytes[i].toInt() and 0x7F)
        }

        assert(value == original) { "Syncsafe roundtrip failed: $value != $original" }

        // Test max value
        val max = 0x0FFFFFFF
        val maxBytes = writeSyncsafeInt(max)

        var maxRead = 0
        for (i in 0 until 4) {
            maxRead = (maxRead shl 7) or (maxBytes[i].toInt() and 0x7F)
        }
        assert(maxRead == max) { "Max syncsafe value failed: $maxRead != $max" }
    }

    @Test
    fun testParseNullTerminatedString() {
        val encoding = TextEncoding.UTF_8
        val hello = "Hello"
        val helloBytes = hello.encodeToByteArray()
        val data = ByteArray(1 + helloBytes.size + 1 + 3)
        data[0] = encoding
        helloBytes.copyInto(data, 1)
        data[1 + helloBytes.size] = 0x00
        data[1 + helloBytes.size + 1] = 0x41
        data[1 + helloBytes.size + 2] = 0x42
        data[1 + helloBytes.size + 3] = 0x43

        val (result, consumed) = parseNullTerminatedString(data, 1, encoding)
        assert(result == "Hello") { "Expected 'Hello', got '$result'" }
        assert(consumed == 6) { "Expected 6 bytes consumed, got $consumed" }
    }

    @Test
    fun testFrameIdMapping() {
        assert(Id3v2FrameId.fromMetadataKey("TITLE") == "TIT2")
        assert(Id3v2FrameId.fromMetadataKey("ARTIST") == "TPE1")
        assert(Id3v2FrameId.fromMetadataKey("ALBUM") == "TALB")
        assert(Id3v2FrameId.fromMetadataKey("TRACKNUMBER") == "TRCK")

        assert(Id3v2FrameId.toMetadataKey("TIT2") == "TITLE")
        assert(Id3v2FrameId.toMetadataKey("TPE1") == "ARTIST")
        assert(Id3v2FrameId.toMetadataKey("TALB") == "ALBUM")
    }

    @Test
    fun testTextEncoding() {
        val ascii = "Artist Name"
        val encoded = encodeId3Text(ascii, TextEncoding.ISO_8859_1)
        val decoded = decodeId3Text(encoded, TextEncoding.ISO_8859_1)
        assert(decoded == ascii) { "ISO-8859-1 roundtrip failed: '$decoded' != '$ascii'" }

        val utf8 = "艺术家名"
        val encodedUtf8 = encodeId3Text(utf8, TextEncoding.UTF_8)
        val decodedUtf8 = decodeId3Text(encodedUtf8, TextEncoding.UTF_8)
        assert(decodedUtf8 == utf8) { "UTF-8 roundtrip failed: '$decodedUtf8' != '$utf8'" }
    }
}
