package com.smelnikowww.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.smelnikowww.domain.SessionConfig
import com.smelnikowww.domain.SessionRecord
import kotlinx.coroutines.flow.first

private val Context.activeSessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "active_session",
)

/**
 * Durable snapshot of the running session so it can survive process death.
 * Timestamps are monotonic; after a device reboot they are meaningless, so a
 * record whose start lies in the future is discarded by the engine.
 */
class ActiveSessionStore(context: Context) {

    private val dataStore = context.applicationContext.activeSessionDataStore

    suspend fun load(): SessionRecord? {
        val prefs = dataStore.data.first()
        if (prefs[ACTIVE] != true) return null
        val duration = prefs[DURATION] ?: return null
        val startedAt = prefs[STARTED_AT] ?: return null
        return SessionRecord(
            config = SessionConfig(
                durationMinutes = duration,
                intervalEnabled = prefs[INTERVAL_ENABLED] ?: true,
                intervalMinutes = prefs[INTERVAL] ?: SessionConfig.Default.intervalMinutes,
            ).normalized(),
            startedAtMillis = startedAt,
            accumulatedPauseMillis = prefs[ACCUMULATED_PAUSE] ?: 0L,
            pauseStartedAtMillis = prefs[PAUSE_STARTED_AT]?.takeIf { it >= 0L },
        )
    }

    suspend fun save(record: SessionRecord) {
        dataStore.edit { prefs ->
            prefs[ACTIVE] = true
            prefs[DURATION] = record.config.durationMinutes
            prefs[INTERVAL_ENABLED] = record.config.intervalEnabled
            prefs[INTERVAL] = record.config.intervalMinutes
            prefs[STARTED_AT] = record.startedAtMillis
            prefs[ACCUMULATED_PAUSE] = record.accumulatedPauseMillis
            prefs[PAUSE_STARTED_AT] = record.pauseStartedAtMillis ?: -1L
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs -> prefs.clear() }
    }

    private companion object {
        val ACTIVE = booleanPreferencesKey("active")
        val DURATION = intPreferencesKey("duration_minutes")
        val INTERVAL_ENABLED = booleanPreferencesKey("interval_enabled")
        val INTERVAL = intPreferencesKey("interval_minutes")
        val STARTED_AT = longPreferencesKey("started_at")
        val ACCUMULATED_PAUSE = longPreferencesKey("accumulated_pause")
        val PAUSE_STARTED_AT = longPreferencesKey("pause_started_at")
    }
}
