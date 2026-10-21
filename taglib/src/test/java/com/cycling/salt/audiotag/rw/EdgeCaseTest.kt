@file:Suppress("unused")

package com.cycling.salt.audiotag.rw

import com.cycling.salt.audiotag.UnstableAudioTagApi
import com.cycling.salt.audiotag.rw.data.Metadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(UnstableAudioTagApi::class)
class EdgeCaseTest {

    @Test
    fun testMetadataValid() {
        assert(Metadata("TITLE", "Test").isValid())
        assert(!Metadata("", "Test").isValid())
        assert(!Metadata("TITLE", "").isValid())
        assert(!Metadata("", "").isValid())
    }

    @Test
    fun testMetadataFormat() {
        val formatted = Metadata(" TITLE ", "  Test Song  ").format()
        assertNotNull(formatted)
        assertEquals("TITLE", formatted.key)
        assertEquals("Test Song", formatted.value)

        val invalid = Metadata("", "").format()
        assertNull(invalid)
    }

    @Test
    fun testMetadataToFlacUserComment() {
        val meta = Metadata("TITLE", "Test Song")
        assertEquals("TITLE=Test Song", meta.toFlacUserComment())
    }

    @Test
    fun testMetadataRoundTrip() {
        val meta = Metadata("  ARTIST  ", "  Some Artist  ")
        val formatted = meta.format()
        assertNotNull(formatted)
        assertEquals("ARTIST", formatted.key)
        assertEquals("Some Artist", formatted.value)
    }
}
