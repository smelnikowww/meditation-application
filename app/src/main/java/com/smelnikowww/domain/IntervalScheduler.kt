package com.smelnikowww.domain

/**
 * Pure interval math. Chimes happen at 1x, 2x, 3x ... the interval, but never
 * at the exact end of the session because the closing bell covers that.
 */
object IntervalScheduler {

    private fun stepMillis(config: SessionConfig): Long =
        config.intervalMinutes * 60_000L

    private fun endExclusiveMillis(config: SessionConfig): Long =
        config.durationMinutes * 60_000L

    /**
     * Chimes whose moment lies in ([fromExclusiveMillis], [toInclusiveMillis]).
     */
    fun chimesIn(
        config: SessionConfig,
        fromExclusiveMillis: Long,
        toInclusiveMillis: Long,
    ): List<Long> {
        if (!config.intervalEnabled) return emptyList()
        val step = stepMillis(config)
        if (step <= 0L) return emptyList()
        val end = endExclusiveMillis(config)
        val result = mutableListOf<Long>()
        var k = fromExclusiveMillis / step + 1
        while (true) {
            val moment = k * step
            if (moment > toInclusiveMillis) break
            if (moment < end) result.add(moment)
            k++
        }
        return result
    }

    /** Next chime moment strictly after [elapsedMillis], or null if none. */
    fun nextChimeMillis(config: SessionConfig, elapsedMillis: Long): Long? {
        if (!config.intervalEnabled) return null
        val step = stepMillis(config)
        if (step <= 0L) return null
        val end = endExclusiveMillis(config)
        val moment = (elapsedMillis / step + 1) * step
        return if (moment < end) moment else null
    }
}
