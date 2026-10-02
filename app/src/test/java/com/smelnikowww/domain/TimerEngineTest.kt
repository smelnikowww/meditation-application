package com.smelnikowww.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerEngineTest {

    private class TestClock(private val scheduler: TestCoroutineScheduler) : MonotonicClock {
        override fun elapsedRealtimeMillis(): Long = scheduler.currentTime
    }

    @Test
    fun `emits start interval and end chimes exactly once each`() = runTest {
        val engine = TimerEngine(TestClock(testScheduler), this, tickMillis = 1_000L)
        val events = mutableListOf<ChimeKind>()
        backgroundScope.launch { engine.chimes.collect { events.add(it) } }
        runCurrent()

        engine.start(SessionConfig(durationMinutes = 5, intervalEnabled = true, intervalMinutes = 1))
        runCurrent()
        advanceTimeBy(5 * 60_000L + 2_000L)
        runCurrent()

        assertEquals(1, events.count { it == ChimeKind.START })
        assertEquals(4, events.count { it == ChimeKind.INTERVAL })
        assertEquals(1, events.count { it == ChimeKind.END })
        assertTrue(engine.state.value?.isFinished == true)
    }

    @Test
    fun `elapsed stays drift free across a long jump`() = runTest {
        val engine = TimerEngine(TestClock(testScheduler), this, tickMillis = 250L)
        engine.start(SessionConfig(durationMinutes = 60, intervalEnabled = false, intervalMinutes = 5))
        runCurrent()

        advanceTimeBy(10 * 60_000L)
        runCurrent()
        assertEquals(600L, engine.state.value?.elapsedSeconds)
        assertEquals(3000L, engine.state.value?.remainingSeconds)
    }

    @Test
    fun `pause freezes remaining time`() = runTest {
        val engine = TimerEngine(TestClock(testScheduler), this, tickMillis = 250L)
        engine.start(SessionConfig(durationMinutes = 5, intervalEnabled = false, intervalMinutes = 1))
        runCurrent()
        advanceTimeBy(60_000L)
        runCurrent()
        engine.pause()

        advanceTimeBy(120_000L)
        runCurrent()
        assertTrue(engine.state.value?.isPaused == true)
        assertEquals(240L, engine.state.value?.remainingSeconds)

        engine.resume()
        runCurrent()
        assertFalse(engine.state.value?.isPaused == true)
        advanceTimeBy(60_000L)
        runCurrent()
        assertEquals(180L, engine.state.value?.remainingSeconds)
    }

    @Test
    fun `restore rejects records from a previous boot`() = runTest {
        val engine = TimerEngine(TestClock(testScheduler), this, tickMillis = 250L)
        val future = testScheduler.currentTime + 10_000_000L
        val record = SessionRecord(
            config = SessionConfig(10, true, 5),
            startedAtMillis = future,
            accumulatedPauseMillis = 0L,
            pauseStartedAtMillis = null,
        )
        assertFalse(engine.restore(record))
    }
}
