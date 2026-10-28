package com.cycling.data.online.lx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrcCodecTest {

    private val plainXml = """
        <?xml version="1.0" encoding="utf-8"?>
        <QrcInfos><LyricInfo LyricCount="1"><Lyric_1 LyricType="1" LyricContent="[ti:测试]
        [0,3000]词(0,500)语(500,500)
        [3000,2000]第二行(0,1000)接上(1000,1000)
        "/></LyricInfo></QrcInfos>
    """.trimIndent()

    @Test
    fun `encrypt then decrypt round trips`() {
        val encrypted = QrcCodec.encryptBytes(plainXml.toByteArray(Charsets.UTF_8))

        assertEquals(0, encrypted.size % 8)
        assertEquals(plainXml, String(QrcCodec.decryptBytes(encrypted), Charsets.UTF_8))
    }

    @Test
    fun `decrypt accepts hex payload`() {
        val encrypted = QrcCodec.encryptBytes(plainXml.toByteArray(Charsets.UTF_8))
        val hex = encrypted.joinToString("") { "%02x".format(it.toInt() and 0xFF) }

        assertEquals(plainXml, QrcCodec.decrypt(hex))
    }

    @Test
    fun `decrypt accepts base64 payload`() {
        val encrypted = QrcCodec.encryptBytes(plainXml.toByteArray(Charsets.UTF_8))
        val base64 = java.util.Base64.getEncoder().encodeToString(encrypted)

        assertEquals(plainXml, QrcCodec.decrypt(base64))
    }

    @Test
    fun `toLrc turns word timings into line timings`() {
        val lrc = QrcCodec.toLrc(plainXml)

        assertTrue(lrc.lines().any { it == "[00:00.00]词语" })
        assertTrue(lrc.lines().any { it == "[00:03.00]第二行接上" })
        assertTrue(lrc.lines().any { it == "[ti:测试]" })
    }

    @Test
    fun `toYrc keeps word timings`() {
        val yrc = QrcCodec.toYrc(plainXml)

        assertTrue(yrc.lines().any { it == "[0,3000](0,500,0)词(500,500,0)语" })
        assertTrue(yrc.lines().any { it == "[3000,2000](0,1000,0)第二行(1000,1000,0)接上" })
    }

    @Test
    fun `escaped xml entities are decoded`() {
        val escaped = """<Lyric_1 LyricContent="[0,1000]a&amp;b(0,500)"/></Lyric_1>"""

        assertEquals("[00:00.00]a&b", QrcCodec.toLrc(escaped))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects payload that is neither hex nor base64`() {
        QrcCodec.decrypt("not-a-qrc-payload")
    }
}
