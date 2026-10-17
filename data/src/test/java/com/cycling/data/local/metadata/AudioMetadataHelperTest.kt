package com.cycling.data.local.metadata

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/**
 * ReplayGain 读取器是"扫头部字节找标签"，没有格式解析器兜底，所以这里把三种真实写法的字节
 * 布局都钉住：Vorbis comment 的单字节明文、ID3v2 TXXX 的 UTF-16 值、以及完全没有标签的文件。
 */
class AudioMetadataHelperTest {

    private val helper = AudioMetadataHelper()

    @Test
    fun `reads a vorbis comment style replay gain`() = runTest {
        val file = tempFile(
            "fLaC\u0004\u0000\u0000\u0022" +
                "REPLAYGAIN_TRACK_GAIN=-7.53 dB\n" +
                "REPLAYGAIN_TRACK_PEAK=0.988831"
        )

        assertEquals(-7.53f, helper.extractReplayGainDb(file.path)!!, 0.001f)
    }

    @Test
    fun `reads an id3v2 utf-16 replay gain`() = runTest {
        // ID3v2.3 的 TXXX 帧：描述与值之间用 NUL 分隔，值按 UTF-16 存放，
        // 于是每个 ASCII 字符后面都跟着一个 0x00。
        val id3Body = utf16Bytes("REPLAYGAIN_TRACK_GAIN") + "\u0000\u0000" +
            utf16Bytes("-3.21 dB")
        val file = tempFile("ID3\u0003\u0000\u0000\u0000\u0000\u0000\u0000TXXX$id3Body")

        assertEquals(-3.21f, helper.extractReplayGainDb(file.path)!!, 0.001f)
    }

    @Test
    fun `ignores a peak tag that has no gain tag`() = runTest {
        val file = tempFile("REPLAYGAIN_TRACK_PEAK=0.988831\nREPLAYGAIN_ALBUM_GAIN=-1.00 dB")

        assertNull(helper.extractReplayGainDb(file.path))
    }

    @Test
    fun `returns null when the file carries no replay gain`() = runTest {
        val file = tempFile("fLaC\u0004\u0000\u0000\u0022TITLE=Some Song\nARTIST=Somebody")

        assertNull(helper.extractReplayGainDb(file.path))
    }

    @Test
    fun `returns null for a missing file`() = runTest {
        assertNull(helper.extractReplayGainDb("/no/such/file.mp3"))
    }

    private fun utf16Bytes(text: String): String =
        text.map { "$it\u0000" }.joinToString(separator = "")

    private fun tempFile(content: String): File {
        val file = File.createTempFile("replaygain", ".bin")
        file.deleteOnExit()
        file.writeBytes(content.toByteArray(Charsets.ISO_8859_1))
        return file
    }
}
