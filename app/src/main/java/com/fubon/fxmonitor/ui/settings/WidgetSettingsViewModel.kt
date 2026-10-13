package com.fubon.fxmonitor.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fubon.fxmonitor.data.local.UserPreferencesRepository
import com.fubon.fxmonitor.data.local.WidgetConfig
import com.fubon.fxmonitor.data.repository.ExchangeRateRepository
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.ExchangeRate
import com.fubon.fxmonitor.domain.model.RateType
import com.fubon.fxmonitor.widget.WidgetUpdater
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WidgetSettingsUiState(
    val activeWidgetIds: List<String> = emptyList(),
    val selectedWidgetId: String = "default_widget",
    val rateType: RateType = RateType.SPOT,
    val selectedCurrencies: List<CurrencyCode> = listOf(CurrencyCode.USD),
    val availableRates: List<ExchangeRate> = emptyList(),
    val isSaved: Boolean = false
)

@HiltViewModel
class WidgetSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPrefs: UserPreferencesRepository,
    private val repository: ExchangeRateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WidgetSettingsUiState())
    val uiState: StateFlow<WidgetSettingsUiState> = _uiState.asStateFlow()

    init {
        loadActiveWidgets()
        observeRates()
    }

    private fun loadActiveWidgets() {
        viewModelScope.launch {
            val ids = WidgetUpdater.getPlacedWidgetIds(context)
            val widgetList = if (ids.isEmpty()) listOf("default_widget") else ids
            _uiState.update {
                it.copy(
                    activeWidgetIds = widgetList,
                    selectedWidgetId = widgetList.first()
                )
            }
            loadConfigForWidget(widgetList.first())
        }
    }

    fun selectWidget(widgetId: String) {
        _uiState.update { it.copy(selectedWidgetId = widgetId, isSaved = false) }
        loadConfigForWidget(widgetId)
    }

    private fun loadConfigForWidget(widgetId: String) {
        viewModelScope.launch {
            userPrefs.getWidgetConfig(widgetId).first().let { config ->
                val currencies = config.selectedCurrencies.take(2).ifEmpty { listOf(CurrencyCode.USD) }
                _uiState.update {
                    it.copy(
                        rateType = config.rateType,
                        selectedCurrencies = currencies
                    )
                }
            }
        }
    }

    private fun observeRates() {
        viewModelScope.launch {
            repository.latestRates.collect { rates ->
                _uiState.update { it.copy(availableRates = rates) }
            }
        }
    }

    fun setRateType(type: RateType) {
        _uiState.update { it.copy(rateType = type, isSaved = false) }
    }

    fun toggleCurrency(code: CurrencyCode) {
        _uiState.update { state ->
            val current = state.selectedCurrencies.toMutableList()
            if (current.contains(code)) {
                if (current.size > 1) current.remove(code)
            } else {
                if (current.size < 2) current.add(code)
            }
            state.copy(selectedCurrencies = current, isSaved = false)
        }
    }

    fun moveCurrencyUp(index: Int) {
        if (index <= 0) return
        _uiState.update { state ->
            val list = state.selectedCurrencies.toMutableList()
            val temp = list[index]
            list[index] = list[index - 1]
            list[index - 1] = temp
            state.copy(selectedCurrencies = list, isSaved = false)
        }
    }

    fun moveCurrencyDown(index: Int) {
        _uiState.update { state ->
            val list = state.selectedCurrencies.toMutableList()
            if (index >= list.size - 1) return@update state
            val temp = list[index]
            list[index] = list[index + 1]
            list[index + 1] = temp
            state.copy(selectedCurrencies = list, isSaved = false)
        }
    }

    fun saveConfig() {
        viewModelScope.launch {
            val state = _uiState.value
            val config = WidgetConfig(
                appWidgetId = state.selectedWidgetId,
                rateType = state.rateType,
                selectedCurrencies = state.selectedCurrencies.take(2)
            )
            userPrefs.saveWidgetConfig(config)
            WidgetUpdater.updateAllWidgets(context)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
