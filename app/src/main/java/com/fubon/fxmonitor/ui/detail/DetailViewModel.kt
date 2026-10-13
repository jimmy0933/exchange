package com.fubon.fxmonitor.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fubon.fxmonitor.data.local.RateHistoryEntity
import com.fubon.fxmonitor.data.repository.ExchangeRateRepository
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.ExchangeRate
import com.fubon.fxmonitor.domain.model.RateType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

enum class TimeRange(val label: String) {
    TODAY("今天"),
    WEEK("7天"),
    MONTH("30天"),
    THREE_MONTHS("3個月"),
    SIX_MONTHS("6個月")
}

data class DetailUiState(
    val currencyCode: CurrencyCode = CurrencyCode.USD,
    val latestRate: ExchangeRate? = null,
    val history: List<RateHistoryEntity> = emptyList(),
    val selectedTimeRange: TimeRange = TimeRange.WEEK,
    val selectedRateType: RateType = RateType.SPOT,
    val highPrice: String = "N/A",
    val lowPrice: String = "N/A",
    val avgPrice: String = "N/A",
    val changeRangePct: String = "N/A"
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: ExchangeRateRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val codeString: String = savedStateHandle.get<String>("currency") ?: "USD"
    private val currency = CurrencyCode.fromString(codeString) ?: CurrencyCode.USD

    private val _uiState = MutableStateFlow(DetailUiState(currencyCode = currency))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.latestRates,
                repository.getHistoryForCurrency(currency.name),
                _uiState.map { it.selectedTimeRange }.distinctUntilChanged(),
                _uiState.map { it.selectedRateType }.distinctUntilChanged()
            ) { latestList, historyList, range, type ->
                val latest = latestList.find { it.currency == currency }

                // Calculate stats
                val rates = historyList.mapNotNull { hist ->
                    val str = if (type == RateType.SPOT) hist.spotSell else hist.cashSell
                    str?.toBigDecimalOrNull()
                }

                var high = "N/A"
                var low = "N/A"
                var avg = "N/A"
                var pctStr = "N/A"

                if (rates.isNotEmpty()) {
                    val maxVal = rates.maxOrNull()
                    val minVal = rates.minOrNull()
                    val sumVal = rates.reduce { acc, b -> acc.add(b) }
                    val avgVal = sumVal.divide(BigDecimal(rates.size), 4, RoundingMode.HALF_UP)

                    high = maxVal?.toPlainString() ?: "N/A"
                    low = minVal?.toPlainString() ?: "N/A"
                    avg = avgVal.toPlainString()

                    if (rates.size >= 2 && minVal != null && minVal.compareTo(BigDecimal.ZERO) != 0) {
                        val first = rates.last()
                        val last = rates.first()
                        val diff = last.subtract(first)
                        val pct = diff.divide(first, 6, RoundingMode.HALF_UP).multiply(BigDecimal("100"))
                        pctStr = "${if (pct >= BigDecimal.ZERO) "+" else ""}${pct.setScale(2, RoundingMode.HALF_UP)}%"
                    }
                }

                _uiState.update {
                    it.copy(
                        latestRate = latest,
                        history = historyList,
                        highPrice = high,
                        lowPrice = low,
                        avgPrice = avg,
                        changeRangePct = pctStr
                    )
                }
            }.collect()
        }
    }

    fun setTimeRange(range: TimeRange) {
        _uiState.update { it.copy(selectedTimeRange = range) }
    }

    fun setRateType(type: RateType) {
        _uiState.update { it.copy(selectedRateType = type) }
    }
}
