package com.smelnikowww.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives a [SessionTimer] on a periodic tick and publishes UI state plus
 * discrete chime events.
 *
 * The tick exists only to refresh presentation and detect chime boundaries.
 * All time values are derived from the monotonic clock, so a delayed or
 * missed tick can never cause drift.
 */
class TimerEngine(
    private val clock: MonotonicClock,
    private val scope: CoroutineScope,
    private val tickMillis: Long = 250L,
) {
    private val _state = MutableStateFlow<SessionState?>(null)
    val state: StateFlow<SessionState?> = _state.asStateFlow()

    private val _chimes = MutableSharedFlow<ChimeKind>(extraBufferCapacity = 16)
    val chimes: SharedFlow<ChimeKind> = _chimes.asSharedFlow()

    private var timer: SessionTimer? = null
    private var lastElapsedMillis = 0L
    private var chimesPlayed = 0
    private var tickJob: Job? = null

    val hasSession: Boolean get() = timer != null

    fun start(config: SessionConfig) {
        val now = clock.elapsedRealtimeMillis()
        begin(SessionTimer(config = config, startedAtMillis = now), now)
        _chimes.tryEmit(ChimeKind.START)
    }

    /**
     * Restores a persisted session. Returns false when the record cannot be
     * trusted (device rebooted, so the monotonic base changed).
     */
    fun restore(record: SessionRecord): Boolean {
        val now = clock.elapsedRealtimeMillis()
        if (record.startedAtMillis > now) return false
        val restored = SessionTimer(
            config = record.config,
            startedAtMillis = record.startedAtMillis,
            accumulatedPauseMillis = record.accumulatedPauseMillis,
            pauseStartedAtMillis = record.pauseStartedAtMillis,
        )
        begin(restored, now)
        return true
    }

    fun pause() {
        timer = timer?.pause(clock.elapsedRealtimeMillis())
        publish()
    }

    fun resume() {
        timer = timer?.resume(clock.elapsedRealtimeMillis())
        publish()
    }

    fun stop() {
        tickJob?.cancel()
        tickJob = null
        timer = null
        _state.value = null
    }

    fun record(): SessionRecord? = timer?.let {
        SessionRecord(
            config = it.config,
            startedAtMillis = it.startedAtMillis,
            accumulatedPauseMillis = it.accumulatedPauseMillis,
            pauseStartedAtMillis = it.pauseStartedAtMillis,
        )
    }

    private fun begin(newTimer: SessionTimer, now: Long) {
        tickJob?.cancel()
        tickJob = null
        timer = newTimer
        lastElapsedMillis = newTimer.elapsedMillis(now)
        chimesPlayed = IntervalScheduler
            .chimesIn(newTimer.config, -1L, lastElapsedMillis)
            .size
        publish(now)
        if (newTimer.isFinished(now)) {
            finish()
            return
        }
        tickJob = scope.launch { tickLoop() }
    }

    private suspend fun tickLoop() {
        while (true) {
            delay(tickMillis)
            val current = timer ?: return
            val now = clock.elapsedRealtimeMillis()
            val elapsed = current.elapsedMillis(now)
            if (elapsed > lastElapsedMillis) {
                val due = IntervalScheduler.chimesIn(current.config, lastElapsedMillis, elapsed)
                due.forEach {
                    chimesPlayed++
                    _chimes.tryEmit(ChimeKind.INTERVAL)
                }
                lastElapsedMillis = elapsed
            }
            publish(now)
            if (current.isFinished(now)) {
                finish()
                return
            }
        }
    }

    private fun finish() {
        tickJob?.cancel()
        tickJob = null
        _chimes.tryEmit(ChimeKind.END)
        publish(clock.elapsedRealtimeMillis())
    }

    private fun publish(now: Long = clock.elapsedRealtimeMillis()) {
        val current = timer ?: return
        val elapsed = current.elapsedMillis(now)
        val remaining = current.remainingMillis(now)
        val nextChime = IntervalScheduler.nextChimeMillis(current.config, elapsed)
        _state.value = SessionState(
            config = current.config,
            elapsedSeconds = elapsed / 1000,
            remainingSeconds = (remaining + 999) / 1000,
            isPaused = current.isPaused,
            isFinished = current.isFinished(now),
            nextChimeSeconds = nextChime?.let { (it - elapsed + 999) / 1000 },
            chimesPlayed = chimesPlayed,
        )
    }
}
