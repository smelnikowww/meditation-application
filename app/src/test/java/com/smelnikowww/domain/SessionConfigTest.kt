package com.smelnikowww.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionConfigTest {

    @Test
    fun `normalized clamps duration below minimum`() {
        val config = SessionConfig(durationMinutes = 1, intervalEnabled = true, intervalMinutes = 5)
        assertEquals(SessionConfig.MIN_DURATION, config.normalized().durationMinutes)
    }

    @Test
    fun `normalized clamps duration above maximum`() {
        val config = SessionConfig(durationMinutes = 10_000, intervalEnabled = true, intervalMinutes = 5)
        assertEquals(SessionConfig.MAX_DURATION, config.normalized().durationMinutes)
    }

    @Test
    fun `normalized clamps interval into range`() {
        val tooSmall = SessionConfig(30, true, 0).normalized()
        val tooBig = SessionConfig(30, true, 10_000).normalized()
        assertEquals(SessionConfig.MIN_INTERVAL, tooSmall.intervalMinutes)
        assertEquals(SessionConfig.MAX_INTERVAL, tooBig.intervalMinutes)
    }

    @Test
    fun `interval count excludes the closing bell`() {
        val config = SessionConfig(durationMinutes = 10, intervalEnabled = true, intervalMinutes = 5)
        assertEquals(1, config.intervalChimeCount)
    }

    @Test
    fun `interval count for long session`() {
        val config = SessionConfig(durationMinutes = 60, intervalEnabled = true, intervalMinutes = 10)
        assertEquals(5, config.intervalChimeCount)
    }

    @Test
    fun `interval larger than duration yields no interval chimes`() {
        val config = SessionConfig(durationMinutes = 10, intervalEnabled = true, intervalMinutes = 240)
        assertEquals(0, config.intervalChimeCount)
    }

    @Test
    fun `disabled interval yields no chimes`() {
        val config = SessionConfig(durationMinutes = 60, intervalEnabled = false, intervalMinutes = 10)
        assertEquals(0, config.intervalChimeCount)
    }

    @Test
    fun `validity respects boundaries`() {
        assertTrue(SessionConfig(5, true, 1).isValid)
        assertTrue(SessionConfig(480, false, 240).isValid)
        assertFalse(SessionConfig(4, true, 5).isValid)
        assertFalse(SessionConfig(30, true, 0).isValid)
    }

    @Test
    fun `format minutes label`() {
        assertEquals("45 мин", formatMinutesLabel(45))
        assertEquals("1 ч", formatMinutesLabel(60))
        assertEquals("1 ч 30 мин", formatMinutesLabel(90))
        assertEquals("8 ч", formatMinutesLabel(480))
    }

    @Test
    fun `format clock switches to hours past one hour`() {
        assertEquals("12:34", formatClock(754))
        assertEquals("1:30:00", formatClock(5400))
        assertEquals("00:00", formatClock(-5))
    }
}
