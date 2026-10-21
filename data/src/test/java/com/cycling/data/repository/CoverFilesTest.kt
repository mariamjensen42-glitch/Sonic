package com.cycling.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stored cover files must get a fresh name on every update — Coil keys its cache by URI, so a
 * reused name keeps showing the previous cover. These tests pin the naming rules down.
 */
class CoverFilesTest {

    @Test
    fun `every update produces a different file name`() {
        val first = coverFileName(songId = 1L, stamp = 1_000L)
        val second = coverFileName(songId = 1L, stamp = 1_001L)

        assertEquals("1-1000.jpg", first)
        assertEquals("1-1001.jpg", second)
        assertTrue(first != second)
    }

    @Test
    fun `stamped and legacy names are recognised as this song's cover`() {
        assertTrue(isCoverFileForSong("1-1000.jpg", songId = 1L))
        // A cover stored by the build before the stamped names.
        assertTrue(isCoverFileForSong("1.jpg", songId = 1L))
    }

    @Test
    fun `another song's file is never matched by a longer or shorter id`() {
        assertFalse(isCoverFileForSong("12-1000.jpg", songId = 1L))
        assertFalse(isCoverFileForSong("12.jpg", songId = 1L))
        assertFalse(isCoverFileForSong("1-1000.jpg", songId = 12L))
        assertFalse(isCoverFileForSong("notes.txt", songId = 1L))
    }
}
