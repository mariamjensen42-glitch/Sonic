package com.cycling.data.local.metadata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 封面图片的格式嗅探。
 *
 * 这一步错了不会崩，只会让下载来的歌没有封面：`ArtworkFactory` 按扩展名决定写进容器里的
 * mime，猜错（例如把 PNG 当 jpg）有的播放器就整张图不认。音源的封面地址又常常没有后缀
 * （`...?x-oss-process=image/resize`），所以只能看 magic number。
 */
class CoverImageSniffTest {

    @Test
    fun `jpeg magic`() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0, 0)
        assertEquals("jpg", sniffImageExtension(bytes))
    }

    @Test
    fun `png magic`() {
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertEquals("png", sniffImageExtension(bytes))
    }

    @Test
    fun `gif magic`() {
        assertEquals("gif", sniffImageExtension("GIF89a".toByteArray(Charsets.US_ASCII)))
    }

    @Test
    fun `webp magic`() {
        // RIFF....WEBP
        val header = "RIFF".toByteArray(Charsets.US_ASCII) +
            byteArrayOf(0x24, 0, 0, 0) +
            "WEBP".toByteArray(Charsets.US_ASCII)
        assertEquals("webp", sniffImageExtension(header))
    }

    @Test
    fun `riff that is not webp is rejected`() {
        // RIFF 也可能是 wav：只看前 4 字节会把音频当封面塞进标签。
        val header = "RIFF".toByteArray(Charsets.US_ASCII) +
            byteArrayOf(0x24, 0, 0, 0) +
            "WAVE".toByteArray(Charsets.US_ASCII)
        assertNull(sniffImageExtension(header))
    }

    @Test
    fun `html error page is rejected`() {
        // 音源 403 时常常回一段 HTML，长度和体积都像图片，只有 magic 能识别出来。
        assertNull(sniffImageExtension("<html><body>403</body></html>".toByteArray()))
    }

    @Test
    fun `short payload does not throw`() {
        assertNull(sniffImageExtension(byteArrayOf(0xFF.toByte())))
        assertNull(sniffImageExtension(ByteArray(0)))
    }
}
