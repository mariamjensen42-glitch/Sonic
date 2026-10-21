package com.cycling.domain.lyrics.utils

import com.cycling.domain.lyrics.model.Attributes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LrcMetadataHelperTest {

    @Test
    fun `parse reads the LRC header tags`() {
        val attributes = LrcMetadataHelper.parse(
            listOf(
                "[ti:海阔天空]",
                "[ar:Beyond]",
                "[al:乐与怒]",
                "[by:某位网友]",
                "[offset:500]",
                "[length:326000]",
                "[00:01.00]今天我"
            )
        )

        assertEquals("海阔天空", attributes.title)
        assertEquals("Beyond", attributes.artist)
        assertEquals("乐与怒", attributes.album)
        assertEquals("某位网友", attributes.by)
        assertEquals(500, attributes.offset)
        assertEquals(326000, attributes.duration)
    }

    @Test
    fun `parse leaves unknown tags out of the attributes`() {
        val attributes = LrcMetadataHelper.parse("[re:SomePlayer]\n[00:01.00]line")

        assertNull(attributes.title)
        assertNull(attributes.artist)
        assertNull(attributes.offset)
    }

    @Test
    fun `applyAttributes replaces existing values in place`() {
        val content = "[ti:old]\n[ar:old artist]\n[00:01.00]line"

        val result = LrcMetadataHelper.applyAttributes(
            content,
            Attributes(title = "new", artist = "new artist")
        )

        assertEquals("[ti:new]\n[ar:new artist]\n[00:01.00]line", result)
    }

    @Test
    fun `applyAttributes prepends tags the document did not have`() {
        val result = LrcMetadataHelper.applyAttributes(
            "[00:01.00]line",
            Attributes(title = "song", artist = "artist", album = "album")
        )

        assertEquals("[ti:song]\n[ar:artist]\n[al:album]\n[00:01.00]line", result)
    }

    @Test
    fun `applyAttributes removes a tag whose value is blank`() {
        val result = LrcMetadataHelper.applyAttributes(
            "[ti:song]\n[by:someone]\n[00:01.00]line",
            Attributes(title = "song", by = null)
        )

        assertEquals("[ti:song]\n[00:01.00]line", result)
    }

    @Test
    fun `applyAttributes keeps unknown tags and timings`() {
        val content = "[re:SomePlayer]\n[tool:editor]\n[offset:100]\n[00:01.00]line"

        val result = LrcMetadataHelper.applyAttributes(content, Attributes(offset = 200))

        assertEquals("[re:SomePlayer]\n[tool:editor]\n[offset:200]\n[00:01.00]line", result)
    }

    @Test
    fun `applyAttributes collapses duplicate tags into one`() {
        val result = LrcMetadataHelper.applyAttributes(
            "[ti:first]\n[ti:second]\n[00:01.00]line",
            Attributes(title = "only one")
        )

        assertEquals("[ti:only one]\n[00:01.00]line", result)
    }

    @Test
    fun `applyAttributes is idempotent`() {
        val attributes = Attributes(title = "song", artist = "artist", offset = -250, by = "someone")
        val once = LrcMetadataHelper.applyAttributes("[00:01.00]line", attributes)
        val twice = LrcMetadataHelper.applyAttributes(once, attributes)

        assertEquals(once, twice)
    }

    @Test
    fun `applyAttributes leaves blank content alone`() {
        assertEquals("", LrcMetadataHelper.applyAttributes("", Attributes(title = "song")))
        assertEquals("  ", LrcMetadataHelper.applyAttributes("  ", Attributes(title = "song")))
    }
}
