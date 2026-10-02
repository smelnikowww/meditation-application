package com.smelnikowww.domain

/**
 * Immutable, drift-free session math on top of a monotonic clock.
 *
 * Every method takes the current monotonic time explicitly so the logic is
 * deterministic and trivially unit-testable. Elapsed time is always derived
 * from the start timestamp and accumulated pauses - never from accumulated
 * ticks.
 */
class SessionTimer(
    val config: SessionConfig,
    val startedAtMillis: Long,
    val accumulatedPauseMillis: Long = 0L,
    val pauseStartedAtMillis: Long? = null,
) {
    val durationMillis: Long = config.durationMinutes * 60_000L

    val isPaused: Boolean get() = pauseStartedAtMillis != null

    fun elapsedMillis(now: Long): Long {
        val reference = pauseStartedAtMillis ?: now
        return (reference - startedAtMillis - accumulatedPauseMillis).coerceAtLeast(0L)
    }

    fun remainingMillis(now: Long): Long =
        (durationMillis - elapsedMillis(now)).coerceAtLeast(0L)

    fun isFinished(now: Long): Boolean = !isPaused && remainingMillis(now) == 0L

    fun pause(now: Long): SessionTimer =
        if (isPaused) this else SessionTimer(config, startedAtMillis, accumulatedPauseMillis, now)

    fun resume(now: Long): SessionTimer {
        val pauseStart = pauseStartedAtMillis ?: return this
        return SessionTimer(
            config = config,
            startedAtMillis = startedAtMillis,
            accumulatedPauseMillis = accumulatedPauseMillis + (now - pauseStart).coerceAtLeast(0L),
            pauseStartedAtMillis = null,
        )
    }
}
