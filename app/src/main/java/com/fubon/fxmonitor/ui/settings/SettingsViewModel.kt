package com.fubon.fxmonitor.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fubon.fxmonitor.data.local.ErrorLogDao
import com.fubon.fxmonitor.data.local.ErrorLogEntity
import com.fubon.fxmonitor.data.local.RateDao
import com.fubon.fxmonitor.data.local.UserPreferencesRepository
import com.fubon.fxmonitor.data.repository.ExchangeRateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val repository: ExchangeRateRepository
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
                userPrefs.lastSuccessTime
            ) { interval, sound, vibration, dark, lastSuccess ->
                Tuple5(interval, sound, vibration, dark, lastSuccess)
            }.combine(errorLogDao.getErrorLogs()) { tuple, logs ->
                SettingsUiState(
                    intervalMins = tuple.v1,
                    soundEnabled = tuple.v2,
                    vibrationEnabled = tuple.v3,
                    darkMode = tuple.v4,
                    lastSuccessTime = tuple.v5,
                    errorLogs = logs
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private data class Tuple5<A, B, C, D, E>(
        val v1: A,
        val v2: B,
        val v3: C,
        val v4: D,
        val v5: E
    )

    fun setInterval(mins: Int) {
        viewModelScope.launch {
            userPrefs.setBackgroundInterval(mins)
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
