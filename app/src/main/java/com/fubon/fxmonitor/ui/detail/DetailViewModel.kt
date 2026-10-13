package com.fubon.fxmonitor.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fubon.fxmonitor.data.local.RateHistoryEntity
import com.fubon.fxmonitor.data.repository.ExchangeRateRepository
import com.fubon.fxmonitor.domain.model.BankSide
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.ExchangeRate
import com.fubon.fxmonitor.domain.model.RateType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

enum class TimeRange(val label: String, val durationMillis: Long) {
    WEEK("7日", 7L * 24 * 3600 * 1000),
    MONTH("1個月", 30L * 24 * 3600 * 1000),
    SIX_MONTHS("半年", 180L * 24 * 3600 * 1000),
    YEAR("1年", 365L * 24 * 3600 * 1000)
}

data class DetailUiState(
    val currencyCode: CurrencyCode = CurrencyCode.USD,
    val latestRate: ExchangeRate? = null,
    val history: List<RateHistoryEntity> = emptyList(), // Filtered history sorted DESC (newest first)
    val chartHistory: List<RateHistoryEntity> = emptyList(), // Filtered history sorted ASC (oldest first for chart)
    val selectedTimeRange: TimeRange = TimeRange.WEEK,
    val selectedRateType: RateType = RateType.SPOT,
    val selectedBankSide: BankSide = BankSide.SELL,
    val currentPrice: String = "N/A",
    val highPrice: String = "N/A",
    val lowPrice: String = "N/A",
    val avgPrice: String = "N/A",
    val changeAmount: String = "N/A",
    val changePct: String = "N/A",
    val comparisonDateRange: String = "",
    val dataCoverageNote: String = ""
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: ExchangeRateRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val codeString: String = savedStateHandle.get<String>("currency") ?: "USD"
    val currency: CurrencyCode = CurrencyCode.fromString(codeString) ?: CurrencyCode.USD

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
                _uiState.map { it.selectedRateType }.distinctUntilChanged(),
                _uiState.map { it.selectedBankSide }.distinctUntilChanged()
            ) { latestList, historyList, range, type, side ->
                val latest = latestList.find { it.currency == currency }
                val now = System.currentTimeMillis()
                val cutoff = now - range.durationMillis

                // Filter by time range
                val filtered = historyList.filter { it.timestamp >= cutoff }
                    .sortedBy { it.timestamp }

                val descList = filtered.sortedByDescending { it.timestamp }
                val ascList = filtered

                // Calculate current price from latest rate
                val currentVal = latest?.getRate(type, side)?.toPlainString() ?: "N/A"

                // Extract valid rates for high, low, avg, change calculation
                val validEntries = ascList.mapNotNull { hist ->
                    val rate = hist.getRate(type, side)
                    if (rate != null) hist to rate else null
                }

                var high = "N/A"
                var low = "N/A"
                var avg = "N/A"
                var changeAmtStr = "N/A"
                var pctStr = "N/A"
                var dateRangeStr = ""

                if (validEntries.isNotEmpty()) {
                    val ratesOnly = validEntries.map { it.second }
                    val maxVal = ratesOnly.maxOrNull()
                    val minVal = ratesOnly.minOrNull()
                    val sumVal = ratesOnly.reduce { acc, b -> acc.add(b) }
                    val avgVal = sumVal.divide(BigDecimal(ratesOnly.size), 4, RoundingMode.HALF_UP)

                    high = maxVal?.toPlainString() ?: "N/A"
                    low = minVal?.toPlainString() ?: "N/A"
                    avg = avgVal.stripTrailingZeros().toPlainString()

                    val earliest = validEntries.first()
                    val latestEntry = validEntries.last()

                    dateRangeStr = "比較日期：${earliest.first.officialUpdateTime} ~ ${latestEntry.first.officialUpdateTime}"

                    if (validEntries.size >= 2) {
                        val firstRate = earliest.second
                        val lastRate = latestEntry.second
                        if (firstRate.compareTo(BigDecimal.ZERO) != 0) {
                            val diff = lastRate.subtract(firstRate)
                            val pct = diff.divide(firstRate, 6, RoundingMode.HALF_UP).multiply(BigDecimal("100"))

                            val sign = if (diff >= BigDecimal.ZERO) "+" else ""
                            changeAmtStr = "$sign${diff.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()}"
                            pctStr = "$sign${pct.setScale(2, RoundingMode.HALF_UP).toPlainString()}%"
                        }
                    } else {
                        changeAmtStr = "+0.0000"
                        pctStr = "+0.00%"
                    }
                } else {
                    dateRangeStr = "該區間無有效 ${type.toLabel()}${side.toLabel()} 報價"
                }

                val coverageNote = if (historyList.isEmpty()) {
                    "目前尚未累積歷史匯率資料。"
                } else {
                    "台北富邦銀行未公開歷史 API，本畫面顯示 App 累積之官方牌告紀錄（共 ${filtered.size} 筆）。"
                }

                _uiState.update {
                    it.copy(
                        latestRate = latest,
                        history = descList,
                        chartHistory = ascList,
                        currentPrice = currentVal,
                        highPrice = high,
                        lowPrice = low,
                        avgPrice = avg,
                        changeAmount = changeAmtStr,
                        changePct = pctStr,
                        comparisonDateRange = dateRangeStr,
                        dataCoverageNote = coverageNote
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

    fun setBankSide(side: BankSide) {
        _uiState.update { it.copy(selectedBankSide = side) }
    }

    private fun RateHistoryEntity.getRate(type: RateType, side: BankSide): BigDecimal? {
        val str = when (type) {
            RateType.SPOT -> if (side == BankSide.BUY) spotBuy else spotSell
            RateType.CASH -> if (side == BankSide.BUY) cashBuy else cashSell
        }
        return str?.toBigDecimalOrNull()
    }

    private fun RateType.toLabel(): String = if (this == RateType.SPOT) "即期" else "現鈔"
    private fun BankSide.toLabel(): String = if (this == BankSide.BUY) "買入" else "賣出"
}
