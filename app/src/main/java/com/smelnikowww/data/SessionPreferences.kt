package com.smelnikowww.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.smelnikowww.domain.SessionConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "session_preferences",
)

/** Persists the last used session configuration. */
class SessionPreferences(context: Context) {

    private val dataStore = context.applicationContext.sessionDataStore

    val config: Flow<SessionConfig> = dataStore.data.map { prefs ->
        SessionConfig(
            durationMinutes = prefs[DURATION] ?: SessionConfig.Default.durationMinutes,
            intervalEnabled = prefs[INTERVAL_ENABLED] ?: SessionConfig.Default.intervalEnabled,
            intervalMinutes = prefs[INTERVAL] ?: SessionConfig.Default.intervalMinutes,
        ).normalized()
    }

    suspend fun save(config: SessionConfig) {
        val safe = config.normalized()
        dataStore.edit { prefs ->
            prefs[DURATION] = safe.durationMinutes
            prefs[INTERVAL_ENABLED] = safe.intervalEnabled
            prefs[INTERVAL] = safe.intervalMinutes
        }
    }

    private companion object {
        val DURATION = intPreferencesKey("duration_minutes")
        val INTERVAL_ENABLED = booleanPreferencesKey("interval_enabled")
        val INTERVAL = intPreferencesKey("interval_minutes")
    }
}
