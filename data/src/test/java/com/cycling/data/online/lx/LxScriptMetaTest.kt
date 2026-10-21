package com.cycling.data.online.lx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LxScriptMetaTest {

    private val meta = LxScriptMeta()

    private val validScript = """
        /**
         * @name 测试音源
         * @description 支持 wy/tx
         * @version 1.0.0
         * @author tester
         * @homepage https://example.com
         * @x-pro-only true
         * @lxkey =
         */
        const { EVENT_NAMES } = lx
        lx.on(EVENT_NAMES.request, () => ({}))
    """.trimIndent()

    @Test
    fun `parses header metadata`() {
        val parsed = meta.parseMeta(validScript)

        assertEquals("测试音源", parsed.name)
        assertEquals("支持 wy/tx", parsed.description)
        assertEquals("1.0.0", parsed.version)
        assertEquals("tester", parsed.author)
        assertEquals("https://example.com", parsed.homepage)
    }

    @Test
    fun `parses pro only flag and config fields`() {
        assertTrue(meta.isProOnly(validScript))
        assertFalse(meta.isProOnly(validScript.replace("@x-pro-only true", "@x-pro-only false")))
        assertEquals(listOf("lxkey"), meta.parseEmptyConfigFields(validScript))
    }

    @Test
    fun `rejects empty oversized html and scripts without lx api`() {
        assertEquals(LxScriptMeta.Gate.EMPTY, meta.gateForImport("   "))
        assertEquals(LxScriptMeta.Gate.NOT_JS, meta.gateForImport("<html>404</html>"))
        assertEquals(LxScriptMeta.Gate.NO_LX_MARKER, meta.gateForImport("console.log('hi')"))
        assertEquals(
            LxScriptMeta.Gate.TOO_LARGE,
            meta.gateForImport("lx.on('request',()=>{})\n" + "a".repeat(4 * 1024 * 1024))
        )
        assertNull(meta.validateForImport(validScript))
    }

    @Test
    fun `accepts lx api reached through window or globalThis`() {
        assertEquals(LxScriptMeta.Gate.OK, meta.gateForImport("window.lx.on('request', () => ({}))"))
        assertEquals(LxScriptMeta.Gate.OK, meta.gateForImport("globalThis['lx'].send('inited', {})"))
        assertEquals(LxScriptMeta.Gate.OK, meta.gateForImport("lx.request('https://example.com')"))
    }

    @Test
    fun `md5 is stable and lowercase`() {
        assertEquals(meta.md5("abc"), meta.md5("abc"))
        assertEquals("900150983cd24fb0d6963f7d28e17f72", meta.md5("abc"))
    }
}
