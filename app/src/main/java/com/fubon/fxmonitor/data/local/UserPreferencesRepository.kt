package com.fubon.fxmonitor.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val BACKGROUND_INTERVAL_MINS = intPreferencesKey("background_interval_mins")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val LAST_SUCCESS_TIME = longPreferencesKey("last_success_time")
    }

    val backgroundIntervalMins: Flow<Int> = context.dataStore.data.map {
        it[Keys.BACKGROUND_INTERVAL_MINS] ?: 15
    }

    val soundEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.SOUND_ENABLED] ?: true
    }

    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.VIBRATION_ENABLED] ?: true
    }

    val darkMode: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.DARK_MODE] ?: false
    }

    val lastSuccessTime: Flow<Long> = context.dataStore.data.map {
        it[Keys.LAST_SUCCESS_TIME] ?: 0L
    }

    suspend fun setBackgroundInterval(mins: Int) {
        context.dataStore.edit { it[Keys.BACKGROUND_INTERVAL_MINS] = mins }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setLastSuccessTime(time: Long) {
        context.dataStore.edit { it[Keys.LAST_SUCCESS_TIME] = time }
    }
}
