package com.smelnikowww.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IntervalSchedulerTest {

    private val minute = 60_000L

    @Test
    fun `chimes exclude the closing bell`() {
        val config = SessionConfig(durationMinutes = 60, intervalEnabled = true, intervalMinutes = 10)
        val chimes = IntervalScheduler.chimesIn(config, 0L, 60 * minute)
        assertEquals(
            listOf(10 * minute, 20 * minute, 30 * minute, 40 * minute, 50 * minute),
            chimes,
        )
    }

    @Test
    fun `window is exclusive on the left and inclusive on the right`() {
        val config = SessionConfig(durationMinutes = 60, intervalEnabled = true, intervalMinutes = 10)
        assertEquals(listOf(20 * minute), IntervalScheduler.chimesIn(config, 10 * minute, 20 * minute))
        assertEquals(
            listOf(20 * minute, 30 * minute),
            IntervalScheduler.chimesIn(config, 10 * minute, 30 * minute),
        )
    }

    @Test
    fun `interval longer than session yields nothing`() {
        val config = SessionConfig(durationMinutes = 10, intervalEnabled = true, intervalMinutes = 240)
        assertEquals(emptyList<Long>(), IntervalScheduler.chimesIn(config, 0L, 10 * minute))
        assertNull(IntervalScheduler.nextChimeMillis(config, 0L))
    }

    @Test
    fun `disabled interval yields nothing`() {
        val config = SessionConfig(durationMinutes = 60, intervalEnabled = false, intervalMinutes = 5)
        assertEquals(emptyList<Long>(), IntervalScheduler.chimesIn(config, 0L, 60 * minute))
        assertNull(IntervalScheduler.nextChimeMillis(config, 0L))
    }

    @Test
    fun `next chime is strictly after elapsed`() {
        val config = SessionConfig(durationMinutes = 60, intervalEnabled = true, intervalMinutes = 10)
        assertEquals(10 * minute, IntervalScheduler.nextChimeMillis(config, 0L))
        assertEquals(20 * minute, IntervalScheduler.nextChimeMillis(config, 10 * minute))
        assertEquals(20 * minute, IntervalScheduler.nextChimeMillis(config, 10 * minute + 1))
    }

    @Test
    fun `chimes are not duplicated across adjacent windows`() {
        val config = SessionConfig(durationMinutes = 10, intervalEnabled = true, intervalMinutes = 1)
        val first = IntervalScheduler.chimesIn(config, 0L, 3 * minute)
        val second = IntervalScheduler.chimesIn(config, 3 * minute, 6 * minute)
        assertEquals(listOf(1 * minute, 2 * minute, 3 * minute), first)
        assertEquals(listOf(4 * minute, 5 * minute, 6 * minute), second)
    }
}
