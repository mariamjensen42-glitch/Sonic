package com.cycling.domain.lyrics.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilsTest {

    @Test
    fun `parseAsTime with standard format`() {
        assertEquals(0, "00:00.00".parseAsTime())
        assertEquals(5000, "00:05.00".parseAsTime())
        assertEquals(65000, "01:05.00".parseAsTime())
    }

    @Test
    fun `parseAsTime with millisecond precision`() {
        assertEquals(500, "00:00.500".parseAsTime())
        assertEquals(1234, "00:01.234".parseAsTime())
        assertEquals(59999, "00:59.999".parseAsTime())
    }

    @Test
    fun `parseAsTime with short millisecond format`() {
        assertEquals(120, "00:00.12".parseAsTime())
        assertEquals(10, "00:00.1".parseAsTime())
    }

    @Test
    fun `parseAsTime with hours format`() {
        assertEquals(3600000, "01:00:00.000".parseAsTime())
        assertEquals(3661500, "01:01:01.500".parseAsTime())
        assertEquals(7325000, "02:02:05.000".parseAsTime())
    }

    @Test
    fun `parseAsTime with only seconds`() {
        assertEquals(1000, "1".parseAsTime())
        assertEquals(10000, "10".parseAsTime())
    }

    @Test
    fun `parseAsTime with invalid format returns 0`() {
        assertEquals(0, "".parseAsTime())
        assertEquals(0, "invalid".parseAsTime())
        assertEquals(0, ":::".parseAsTime())
    }

    @Test
    fun `toTimeFormattedString with zero`() {
        assertEquals("00:00:00.000", 0.toTimeFormattedString())
    }

    @Test
    fun `toTimeFormattedString with milliseconds`() {
        assertEquals("00:00:00.500", 500.toTimeFormattedString())
        assertEquals("00:00:00.999", 999.toTimeFormattedString())
    }

    @Test
    fun `toTimeFormattedString with seconds`() {
        assertEquals("00:00:01.000", 1000.toTimeFormattedString())
        assertEquals("00:00:30.000", 30000.toTimeFormattedString())
        assertEquals("00:00:59.000", 59000.toTimeFormattedString())
    }

    @Test
    fun `toTimeFormattedString with minutes`() {
        assertEquals("00:01:00.000", 60000.toTimeFormattedString())
        assertEquals("00:05:30.000", 330000.toTimeFormattedString())
    }

    @Test
    fun `toTimeFormattedString with hours`() {
        assertEquals("01:00:00.000", 3600000.toTimeFormattedString())
        assertEquals("02:30:45.500", 9045500.toTimeFormattedString())
    }

    @Test
    fun `toTimeFormattedString with negative value returns default`() {
        assertEquals("00:00:00.000", (-1).toTimeFormattedString())
        assertEquals("00:00:00.000", (-1000).toTimeFormattedString())
    }

    @Test
    fun `round trip conversion`() {
        val testValues = listOf(0, 1000, 5000, 60000, 3600000, 9045500)
        testValues.forEach { original ->
            val formatted = original.toTimeFormattedString()
            val reconstructed = formatted.parseAsTime()
            assertEquals("Failed for original=$original, formatted=$formatted", original, reconstructed)
        }
    }
}
