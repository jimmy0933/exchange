package com.fubon.fxmonitor.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fubon.fxmonitor.data.local.ErrorLogDao
import com.fubon.fxmonitor.data.local.ErrorLogEntity
import com.fubon.fxmonitor.data.local.RateDao
import com.fubon.fxmonitor.data.local.UserPreferencesRepository
import com.fubon.fxmonitor.data.repository.ExchangeRateRepository
import com.fubon.fxmonitor.worker.ExchangeMonitorWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val intervalMins: Int = 15,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val darkMode: Boolean = false,
    val lastSuccessTime: Long = 0L,
    val errorLogs: List<ErrorLogEntity> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPreferencesRepository,
    private val rateDao: RateDao,
    private val errorLogDao: ErrorLogDao,
    private val repository: ExchangeRateRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                userPrefs.backgroundIntervalMins,
                userPrefs.soundEnabled,
                userPrefs.vibrationEnabled,
                userPrefs.darkMode,
                userPrefs.lastSuccessTime,
                errorLogDao.getErrorLogs()
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                SettingsUiState(
                    intervalMins = args[0] as Int,
                    soundEnabled = args[1] as Boolean,
                    vibrationEnabled = args[2] as Boolean,
                    darkMode = args[3] as Boolean,
                    lastSuccessTime = args[4] as Long,
                    errorLogs = args[5] as List<ErrorLogEntity>
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun setInterval(mins: Int) {
        viewModelScope.launch {
            userPrefs.setBackgroundInterval(mins)
            ExchangeMonitorWorker.schedule(context, mins.toLong())
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setSoundEnabled(enabled)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setVibrationEnabled(enabled)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setDarkMode(enabled)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            rateDao.clearHistory()
            errorLogDao.clearLogs()
        }
    }

    fun refreshRatesNow() {
        viewModelScope.launch {
            repository.refreshRates()
        }
    }
}
