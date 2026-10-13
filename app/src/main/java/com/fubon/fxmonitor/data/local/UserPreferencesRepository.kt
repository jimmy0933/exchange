package com.fubon.fxmonitor.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.RateType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val BACKGROUND_INTERVAL_MINS = intPreferencesKey("background_interval_mins")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val LAST_SUCCESS_TIME = longPreferencesKey("last_success_time")

        fun widgetRateTypeKey(appWidgetId: String) = stringPreferencesKey("widget_${appWidgetId}_rate_type")
        fun widgetCurrenciesKey(appWidgetId: String) = stringPreferencesKey("widget_${appWidgetId}_currencies")
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

    fun getWidgetConfig(appWidgetId: String): Flow<WidgetConfig> {
        return context.dataStore.data.map { prefs ->
            val rateTypeStr = prefs[Keys.widgetRateTypeKey(appWidgetId)]
            val rateType = if (rateTypeStr == RateType.CASH.name) RateType.CASH else RateType.SPOT

            val currenciesStr = prefs[Keys.widgetCurrenciesKey(appWidgetId)]
            val currencies = if (!currenciesStr.isNullOrBlank()) {
                currenciesStr.split(",")
                    .mapNotNull { CurrencyCode.fromString(it) }
            } else {
                CurrencyCode.DEFAULT_FAVORITES
            }

            WidgetConfig(
                appWidgetId = appWidgetId,
                rateType = rateType,
                selectedCurrencies = if (currencies.isEmpty()) CurrencyCode.DEFAULT_FAVORITES else currencies
            )
        }
    }

    suspend fun saveWidgetConfig(config: WidgetConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.widgetRateTypeKey(config.appWidgetId)] = config.rateType.name
            prefs[Keys.widgetCurrenciesKey(config.appWidgetId)] = config.selectedCurrencies.joinToString(",") { it.name }
        }
    }

    suspend fun removeWidgetConfig(appWidgetId: String) {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.widgetRateTypeKey(appWidgetId))
            prefs.remove(Keys.widgetCurrenciesKey(appWidgetId))
        }
    }
}
