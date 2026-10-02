package com.smelnikowww.domain

/**
 * User-chosen parameters of a meditation session.
 *
 * Duration is limited to 5 minutes - 8 hours, the sound interval to
 * 1 minute - 4 hours. The interval may be disabled entirely.
 */
data class SessionConfig(
    val durationMinutes: Int,
    val intervalEnabled: Boolean,
    val intervalMinutes: Int,
) {
    /**
     * Number of chimes played strictly before the end of the session.
     * The end of the session always plays its own closing bell.
     */
    val intervalChimeCount: Int
        get() = if (!intervalEnabled || intervalMinutes <= 0) {
            0
        } else {
            (durationMinutes - 1) / intervalMinutes
        }

    val isValid: Boolean
        get() = durationMinutes in MIN_DURATION..MAX_DURATION &&
            intervalMinutes in MIN_INTERVAL..MAX_INTERVAL

    /** Clamps every field into the supported range. */
    fun normalized(): SessionConfig = copy(
        durationMinutes = durationMinutes.coerceIn(MIN_DURATION, MAX_DURATION),
        intervalMinutes = intervalMinutes.coerceIn(MIN_INTERVAL, MAX_INTERVAL),
    )

    companion object {
        const val MIN_DURATION = 5
        const val MAX_DURATION = 480
        const val MIN_INTERVAL = 1
        const val MAX_INTERVAL = 240

        const val DURATION_STEP = 5
        const val INTERVAL_STEP = 1

        val Default = SessionConfig(
            durationMinutes = 10,
            intervalEnabled = true,
            intervalMinutes = 5,
        )

        val DurationPresets = listOf(5, 10, 15, 20, 30, 45, 60, 90, 120, 240, 480)
        val IntervalPresets = listOf(1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120, 240)
    }
}
