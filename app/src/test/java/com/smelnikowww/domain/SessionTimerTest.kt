package com.smelnikowww.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTimerTest {

    private val config = SessionConfig(durationMinutes = 5, intervalEnabled = false, intervalMinutes = 1)
    private val start = 1_000_000L
    private val timer = SessionTimer(config = config, startedAtMillis = start)

    @Test
    fun `elapsed grows with monotonic time`() {
        assertEquals(60_000L, timer.elapsedMillis(start + 60_000L))
    }

    @Test
    fun `pause freezes elapsed`() {
        val paused = timer.pause(start + 120_000L)
        assertTrue(paused.isPaused)
        assertEquals(120_000L, paused.elapsedMillis(start + 900_000L))
    }

    @Test
    fun `resume accounts for paused time`() {
        val paused = timer.pause(start + 120_000L)
        val resumed = paused.resume(start + 300_000L)
        assertFalse(resumed.isPaused)
        assertEquals(180_000L, resumed.accumulatedPauseMillis)
        assertEquals(180_000L, resumed.elapsedMillis(start + 360_000L))
    }

    @Test
    fun `finished only when not paused and time is up`() {
        val paused = timer.pause(start + 120_000L)
        assertFalse(paused.isFinished(start + 10_000_000L))
        val resumed = paused.resume(start + 300_000L)
        assertTrue(resumed.isFinished(start + 300_000L + 180_000L))
        assertFalse(resumed.isFinished(start + 300_000L + 179_000L))
    }

    @Test
    fun `remaining never goes negative`() {
        assertEquals(0L, timer.remainingMillis(start + 10_000_000L))
    }
}
