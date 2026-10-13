package com.fubon.fxmonitor.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fubon.fxmonitor.data.local.RateDao
import com.fubon.fxmonitor.data.local.UserPreferencesRepository
import com.fubon.fxmonitor.data.repository.ExchangeRateRepository
import com.fubon.fxmonitor.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class CurrencyCardUiState(
    val rate: ExchangeRate,
    val isFavorite: Boolean,
    val displayOrder: Int,
    val spotChangeAmount: BigDecimal? = null,
    val spotChangePct: Double? = null,
    val cashChangeAmount: BigDecimal? = null,
    val cashChangePct: Double? = null
)

data class HomeUiState(
    val rates: List<CurrencyCardUiState> = emptyList(),
    val rateType: RateType = RateType.SPOT,
    val isRefreshing: Boolean = false,
    val isStale: Boolean = false,
    val lastUpdateTime: Long = 0L,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ExchangeRateRepository,
    private val rateDao: RateDao,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeRates()
        refreshData()
    }

    private fun observeRates() {
        viewModelScope.launch {
            combine(
                rateDao.getAllLatestRates(),
                userPrefs.lastSuccessTime,
                _uiState.map { it.rateType }.distinctUntilChanged()
            ) { entities, lastSuccess, rateType ->
                val now = System.currentTimeMillis()
                val isStale = lastSuccess > 0 && (now - lastSuccess > 30 * 60 * 1000)

                val cardStates = entities.map { entity ->
                    val code = CurrencyCode.fromString(entity.currencyCode) ?: CurrencyCode.USD
                    val domainRate = ExchangeRate(
                        currency = code,
                        spotBuy = entity.spotBuy?.toBigDecimalOrNull(),
                        spotSell = entity.spotSell?.toBigDecimalOrNull(),
                        cashBuy = entity.cashBuy?.toBigDecimalOrNull(),
                        cashSell = entity.cashSell?.toBigDecimalOrNull(),
                        officialUpdateTime = entity.officialUpdateTime,
                        localFetchTime = entity.localFetchTime,
                        source = entity.source
                    )

                    val spotChange = repository.calculateRateChange(code.name, RateType.SPOT, BankSide.SELL)
                    val cashChange = repository.calculateRateChange(code.name, RateType.CASH, BankSide.SELL)

                    CurrencyCardUiState(
                        rate = domainRate,
                        isFavorite = entity.isFavorite,
                        displayOrder = entity.displayOrder,
                        spotChangeAmount = spotChange.amount,
                        spotChangePct = spotChange.percentage,
                        cashChangeAmount = cashChange.amount,
                        cashChangePct = cashChange.percentage
                    )
                }

                _uiState.update {
                    it.copy(
                        rates = cardStates,
                        isStale = isStale,
                        lastUpdateTime = lastSuccess
                    )
                }
            }.collect()
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            val result = repository.refreshRates()
            _uiState.update { currentState ->
                currentState.copy(
                    isRefreshing = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun toggleRateType(type: RateType) {
        _uiState.update { it.copy(rateType = type) }
    }

    fun toggleFavorite(code: CurrencyCode, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(code, isFav)
        }
    }
}
