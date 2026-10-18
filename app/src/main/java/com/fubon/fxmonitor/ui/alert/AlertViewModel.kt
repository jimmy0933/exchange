package com.fubon.fxmonitor.ui.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fubon.fxmonitor.data.local.AlertDao
import com.fubon.fxmonitor.data.local.AlertHistoryEntity
import com.fubon.fxmonitor.data.local.ExchangeAlertEntity
import com.fubon.fxmonitor.domain.model.BankSide
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.RateType
import com.fubon.fxmonitor.domain.usecase.TriggerCondition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertUiState(
    val alerts: List<ExchangeAlertEntity> = emptyList(),
    val historyLogs: List<AlertHistoryEntity> = emptyList(),
    val showAddDialog: Boolean = false,
    val selectedCurrency: CurrencyCode = CurrencyCode.USD,
    val selectedRateType: RateType = RateType.SPOT,
    val selectedBankSide: BankSide = BankSide.SELL,
    val selectedCondition: TriggerCondition = TriggerCondition.LESS_EQUAL,
    val targetValueInput: String = ""
)

@HiltViewModel
class AlertViewModel @Inject constructor(
    private val alertDao: AlertDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertUiState())
    val uiState: StateFlow<AlertUiState> = _uiState.asStateFlow()

    init {
        observeAlerts()
    }

    private fun observeAlerts() {
        viewModelScope.launch {
            combine(
                alertDao.getAllAlerts(),
                alertDao.getAlertHistory()
            ) { alerts, logs ->
                _uiState.update { it.copy(alerts = alerts, historyLogs = logs) }
            }.collect()
        }
    }

    fun openAddDialog(currencyCode: String? = null) {
        val currency = CurrencyCode.fromString(currencyCode ?: "") ?: CurrencyCode.USD
        _uiState.update {
            it.copy(
                showAddDialog = true,
                selectedCurrency = currency,
                targetValueInput = ""
            )
        }
    }

    fun closeAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun updateSelectedCurrency(code: CurrencyCode) {
        _uiState.update { it.copy(selectedCurrency = code) }
    }

    fun updateSelectedRateType(type: RateType) {
        _uiState.update { it.copy(selectedRateType = type) }
    }

    fun updateSelectedBankSide(side: BankSide) {
        _uiState.update { it.copy(selectedBankSide = side) }
    }

    fun updateSelectedCondition(condition: TriggerCondition) {
        _uiState.update { it.copy(selectedCondition = condition) }
    }

    fun updateTargetValue(value: String) {
        _uiState.update { it.copy(targetValueInput = value) }
    }

    fun saveAlert() {
        val state = _uiState.value
        val valDouble = state.targetValueInput.toDoubleOrNull() ?: return

        if (valDouble.isNaN() || valDouble.isInfinite() || valDouble <= 0.0) {
            return // Invalid input
        }

        viewModelScope.launch {
            alertDao.insertAlert(
                ExchangeAlertEntity(
                    currencyCode = state.selectedCurrency.name,
                    rateType = state.selectedRateType.name,
                    bankSide = state.selectedBankSide.name,
                    triggerCondition = state.selectedCondition.name,
                    targetValue = valDouble,
                    isEnabled = true
                )
            )
            closeAddDialog()
        }
    }

    fun toggleAlertEnabled(alert: ExchangeAlertEntity, isEnabled: Boolean) {
        viewModelScope.launch {
            alertDao.updateAlert(alert.copy(isEnabled = isEnabled))
        }
    }

    fun deleteAlert(alertId: Long) {
        viewModelScope.launch {
            alertDao.deleteAlert(alertId)
        }
    }

    fun clearAlertHistory() {
        viewModelScope.launch {
            alertDao.clearAlertHistory()
        }
    }
}
