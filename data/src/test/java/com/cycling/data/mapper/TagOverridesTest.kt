package com.cycling.data.mapper

import com.cycling.data.local.entity.SongMetadataEntity
import com.cycling.domain.model.TagUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The override table is the only place a tag edit survives when the file cannot be written, so the
 * null/blank distinction in [mergeWith] is worth pinning down: `null` means "untouched", `""`
 * means "cleared".
 */
class TagOverridesTest {

    @Test
    fun `fields the update leaves null keep their stored override`() {
        val stored = TagUpdate(genre = "Rock", year = "1999")

        val merged = stored.mergeWith(TagUpdate(composer = "Someone"))

        assertEquals("Rock", merged.genre)
        assertEquals("1999", merged.year)
        assertEquals("Someone", merged.composer)
    }

    @Test
    fun `a blank update value clears the stored override`() {
        val stored = TagUpdate(genre = "Rock", year = "1999")

        val merged = stored.mergeWith(TagUpdate(genre = ""))

        assertNull(merged.genre)
        assertEquals("1999", merged.year)
    }

    @Test
    fun `a non-blank update replaces the stored override`() {
        val merged = TagUpdate(genre = "Rock").mergeWith(TagUpdate(genre = "Jazz"))

        assertEquals("Jazz", merged.genre)
    }

    @Test
    fun `entity round trip keeps every overridden field`() {
        val update = TagUpdate(
            albumArtist = "Various",
            year = "2001",
            genre = "Pop",
            trackNumber = "3/12",
            discNumber = "1/2",
            composer = "Composer",
            publisher = "Label",
            copyright = "(c)",
            comment = "note"
        )

        val entity: SongMetadataEntity = update.toSongMetadataEntity(songId = 7L, updatedAt = 1L)
        val roundTripped = entity.toTagUpdate()

        assertEquals(update, roundTripped)
        assertEquals(7L, entity.songId)
    }
}
