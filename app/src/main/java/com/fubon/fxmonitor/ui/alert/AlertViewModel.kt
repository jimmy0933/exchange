package com.fubon.fxmonitor.ui.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fubon.fxmonitor.data.local.AlertDao
import com.fubon.fxmonitor.data.local.AlertHistoryEntity
import com.fubon.fxmonitor.data.local.ExchangeAlertEntity
import com.fubon.fxmonitor.data.local.RateDao
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
    val targetValueInput: String = "",
    val errorMessage: String? = null,
    val isQuoteAvailable: Boolean = true
)

@HiltViewModel
class AlertViewModel @Inject constructor(
    private val alertDao: AlertDao,
    private val rateDao: RateDao
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
                targetValueInput = "",
                errorMessage = null
            )
        }
        checkQuoteAvailability()
    }

    fun closeAddDialog() {
        _uiState.update { it.copy(showAddDialog = false, errorMessage = null) }
    }

    fun updateSelectedCurrency(code: CurrencyCode) {
        _uiState.update { it.copy(selectedCurrency = code, errorMessage = null) }
        checkQuoteAvailability()
    }

    fun updateSelectedRateType(type: RateType) {
        _uiState.update { it.copy(selectedRateType = type, errorMessage = null) }
        checkQuoteAvailability()
    }

    fun updateSelectedBankSide(side: BankSide) {
        _uiState.update { it.copy(selectedBankSide = side, errorMessage = null) }
        checkQuoteAvailability()
    }

    fun updateSelectedCondition(condition: TriggerCondition) {
        _uiState.update { it.copy(selectedCondition = condition, errorMessage = null) }
    }

    fun updateTargetValue(value: String) {
        _uiState.update { it.copy(targetValueInput = value, errorMessage = null) }
    }

    private fun checkQuoteAvailability() {
        viewModelScope.launch {
            val state = _uiState.value
            val rate = rateDao.getLatestRate(state.selectedCurrency.name)
            var available = true

            if (rate != null) {
                val str = when (state.selectedRateType) {
                    RateType.SPOT -> if (state.selectedBankSide == BankSide.BUY) rate.spotBuy else rate.spotSell
                    RateType.CASH -> if (state.selectedBankSide == BankSide.BUY) rate.cashBuy else rate.cashSell
                }
                if (str == null || str.toDoubleOrNull() == null) {
                    available = false
                }
            }

            _uiState.update { it.copy(isQuoteAvailable = available) }
        }
    }

    fun saveAlert() {
        val state = _uiState.value

        if (!state.isQuoteAvailable) {
            _uiState.update { it.copy(errorMessage = "該幣別目前無此類型的有效報價，無法建立提醒。") }
            return
        }

        val valDouble = state.targetValueInput.toDoubleOrNull()
        if (valDouble == null || valDouble.isNaN() || valDouble.isInfinite() || valDouble <= 0) {
            _uiState.update { it.copy(errorMessage = "請輸入合法的正數數值") }
            return
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
