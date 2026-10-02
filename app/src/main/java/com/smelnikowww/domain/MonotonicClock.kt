package com.smelnikowww.domain

/**
 * Monotonic time source. Implementations must never jump backwards and are
 * unaffected by wall-clock changes.
 */
fun interface MonotonicClock {
    fun elapsedRealtimeMillis(): Long
}
