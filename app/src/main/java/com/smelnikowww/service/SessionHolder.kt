package com.smelnikowww.service

import com.smelnikowww.domain.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-process bridge between the foreground service and the UI. The service is
 * the single writer; screens only observe.
 */
object SessionHolder {
    private val _state = MutableStateFlow<SessionState?>(null)
    val state: StateFlow<SessionState?> = _state.asStateFlow()

    fun set(state: SessionState?) {
        _state.value = state
    }
}
