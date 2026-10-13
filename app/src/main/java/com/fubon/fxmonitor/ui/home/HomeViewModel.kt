package com.fubon.fxmonitor.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fubon.fxmonitor.data.local.RateDao
import com.fubon.fxmonitor.data.local.UserPreferencesRepository
import com.fubon.fxmonitor.data.repository.ExchangeRateRepository
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.ExchangeRate
import com.fubon.fxmonitor.domain.model.RateType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class CurrencyCardUiState(
    val rate: ExchangeRate,
    val isFavorite: Boolean,
    val displayOrder: Int,
    val spotChangePct: Double? = null,
    val cashChangePct: Double? = null
)

sealed interface RefreshStatus {
    object Idle : RefreshStatus
    data class Success(val message: String) : RefreshStatus
    data class NoChange(val message: String) : RefreshStatus
    data class Error(val message: String) : RefreshStatus
}

data class HomeUiState(
    val rates: List<CurrencyCardUiState> = emptyList(),
    val rateType: RateType = RateType.SPOT,
    val isRefreshing: Boolean = false,
    val isStale: Boolean = false,
    val lastUpdateTime: Long = 0L,
    val refreshStatus: RefreshStatus = RefreshStatus.Idle
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
                // Stale if last success is more than 30 mins ago
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

                    CurrencyCardUiState(
                        rate = domainRate,
                        isFavorite = entity.isFavorite,
                        displayOrder = entity.displayOrder
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
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            val previousOfficialTime = _uiState.value.rates.firstOrNull()?.rate?.officialUpdateTime
            _uiState.update { it.copy(isRefreshing = true, refreshStatus = RefreshStatus.Idle) }

            val result = repository.refreshRates()

            result.fold(
                onSuccess = { newRates ->
                    val newOfficialTime = newRates.firstOrNull()?.officialUpdateTime
                    val status = if (previousOfficialTime != null && previousOfficialTime == newOfficialTime) {
                        RefreshStatus.NoChange("查詢成功｜目前已是最新牌告")
                    } else {
                        val displayTime = extractTimeFromOfficial(newOfficialTime)
                        RefreshStatus.Success("更新成功｜最新牌告：$displayTime")
                    }

                    _uiState.update { currentState ->
                        currentState.copy(
                            isRefreshing = false,
                            refreshStatus = status
                        )
                    }
                },
                onFailure = {
                    _uiState.update { currentState ->
                        currentState.copy(
                            isRefreshing = false,
                            refreshStatus = RefreshStatus.Error("更新失敗｜目前顯示上次成功取得的資料")
                        )
                    }
                }
            )
        }
    }

    fun clearRefreshStatus() {
        _uiState.update { it.copy(refreshStatus = RefreshStatus.Idle) }
    }

    fun toggleRateType(type: RateType) {
        _uiState.update { it.copy(rateType = type) }
    }

    fun toggleFavorite(code: CurrencyCode, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(code, isFav)
        }
    }

    private fun extractTimeFromOfficial(officialTimeStr: String?): String {
        if (officialTimeStr.isNullOrBlank()) {
            return SimpleDateFormat("HH:mm", Locale.TAIWAN).format(Date())
        }
        val match = Regex("""\d{2}:\d{2}""").find(officialTimeStr)
        return match?.value ?: SimpleDateFormat("HH:mm", Locale.TAIWAN).format(Date())
    }
}
