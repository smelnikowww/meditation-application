package com.smelnikowww.platform

import android.os.SystemClock
import com.smelnikowww.domain.MonotonicClock

/** Production clock: unaffected by user or network time changes. */
object AndroidMonotonicClock : MonotonicClock {
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
}
