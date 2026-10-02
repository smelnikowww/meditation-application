package com.smelnikowww.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smelnikowww.data.SessionPreferences
import com.smelnikowww.domain.SessionConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SetupViewModel(
    private val preferences: SessionPreferences,
) : ViewModel() {

    private val _config = MutableStateFlow(SessionConfig.Default)
    val config: StateFlow<SessionConfig> = _config.asStateFlow()

    private var userEdited = false

    init {
        viewModelScope.launch {
            val stored = preferences.config.first()
            if (!userEdited) _config.value = stored
        }
    }

    fun setDuration(minutes: Int) = update { it.copy(durationMinutes = minutes) }

    fun setIntervalEnabled(enabled: Boolean) = update { it.copy(intervalEnabled = enabled) }

    fun setInterval(minutes: Int) = update { it.copy(intervalMinutes = minutes) }

    private fun update(transform: (SessionConfig) -> SessionConfig) {
        val next = transform(_config.value).normalized()
        userEdited = true
        _config.value = next
        viewModelScope.launch { preferences.save(next) }
    }

    companion object {
        fun factory(preferences: SessionPreferences): ViewModelProvider.Factory = viewModelFactory {
            initializer { SetupViewModel(preferences) }
        }
    }
}
