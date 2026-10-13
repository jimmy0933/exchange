package com.fubon.fxmonitor.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fubon.fxmonitor.data.local.RateDao
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.ExchangeRate
import com.fubon.fxmonitor.domain.model.RateType
import com.fubon.fxmonitor.domain.usecase.ConversionDirection
import com.fubon.fxmonitor.domain.usecase.ConversionResult
import com.fubon.fxmonitor.domain.usecase.ExchangeCalculatorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class CalculatorUiState(
    val rates: List<ExchangeRate> = emptyList(),
    val selectedCurrency: CurrencyCode = CurrencyCode.JPY,
    val selectedRateType: RateType = RateType.CASH,
    val direction: ConversionDirection = ConversionDirection.TWD_TO_FOREIGN,
    val inputAmountString: String = "10000",
    val conversionResult: ConversionResult? = null
)

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val rateDao: RateDao,
    private val calculatorUseCase: ExchangeCalculatorUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    init {
        observeRates()
    }

    private fun observeRates() {
        viewModelScope.launch {
            rateDao.getAllLatestRates().collect { entities ->
                val rates = entities.map { entity ->
                    val code = CurrencyCode.fromString(entity.currencyCode) ?: CurrencyCode.USD
                    ExchangeRate(
                        currency = code,
                        spotBuy = entity.spotBuy?.toBigDecimalOrNull(),
                        spotSell = entity.spotSell?.toBigDecimalOrNull(),
                        cashBuy = entity.cashBuy?.toBigDecimalOrNull(),
                        cashSell = entity.cashSell?.toBigDecimalOrNull(),
                        officialUpdateTime = entity.officialUpdateTime,
                        localFetchTime = entity.localFetchTime,
                        source = entity.source
                    )
                }
                _uiState.update { it.copy(rates = rates) }
                recalculate()
            }
        }
    }

    fun setCurrency(code: CurrencyCode) {
        _uiState.update { it.copy(selectedCurrency = code) }
        recalculate()
    }

    fun setRateType(type: RateType) {
        _uiState.update { it.copy(selectedRateType = type) }
        recalculate()
    }

    fun setDirection(direction: ConversionDirection) {
        _uiState.update { it.copy(direction = direction) }
        recalculate()
    }

    fun setInputAmount(amount: String) {
        _uiState.update { it.copy(inputAmountString = amount) }
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        val amount = state.inputAmountString.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val rate = state.rates.find { it.currency == state.selectedCurrency }

        if (rate != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            val result = calculatorUseCase.convert(
                amount = amount,
                rate = rate,
                direction = state.direction,
                rateType = state.selectedRateType
            )
            _uiState.update { it.copy(conversionResult = result) }
        } else {
            _uiState.update { it.copy(conversionResult = null) }
        }
    }
}
