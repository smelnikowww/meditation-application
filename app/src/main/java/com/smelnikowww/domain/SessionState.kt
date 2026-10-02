package com.smelnikowww.domain

/** Snapshot of a running session, published to UI and notifications. */
data class SessionState(
    val config: SessionConfig,
    val elapsedSeconds: Long,
    val remainingSeconds: Long,
    val isPaused: Boolean,
    val isFinished: Boolean,
    val nextChimeSeconds: Long?,
    val chimesPlayed: Int,
)

/** Discrete sound triggers produced by the timer. */
enum class ChimeKind { START, INTERVAL, END }

/** Durable representation of an active session. */
data class SessionRecord(
    val config: SessionConfig,
    val startedAtMillis: Long,
    val accumulatedPauseMillis: Long,
    val pauseStartedAtMillis: Long?,
)
