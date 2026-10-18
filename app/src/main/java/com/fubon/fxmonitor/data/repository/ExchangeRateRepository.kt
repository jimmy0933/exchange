package com.fubon.fxmonitor.data.repository

import android.content.Context
import com.fubon.fxmonitor.data.local.*
import com.fubon.fxmonitor.data.remote.ExchangeRateProvider
import com.fubon.fxmonitor.domain.model.*
import com.fubon.fxmonitor.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import androidx.room.withTransaction
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.fubon.fxmonitor.domain.usecase.ExchangeAlertEngine
import com.fubon.fxmonitor.notification.FubonNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val rateProvider: ExchangeRateProvider,
    private val rateDao: RateDao,
    private val errorLogDao: ErrorLogDao,
    private val userPrefs: UserPreferencesRepository,
    private val alertEngine: ExchangeAlertEngine,
    private val notificationManager: FubonNotificationManager
) {

    val latestRates: Flow<List<ExchangeRate>> = rateDao.getAllLatestRates().map { entities ->
        entities.map { it.toDomain() }
    }

    private val updateMutex = Mutex()

    suspend fun refreshRates(): Result<List<ExchangeRate>> {
        return updateMutex.withLock {
            refreshRatesInternal()
        }
    }

    private suspend fun refreshRatesInternal(): Result<List<ExchangeRate>> {
        val result = rateProvider.fetchRates()
        return result.fold(
            onSuccess = { rates ->
                val now = System.currentTimeMillis()

                val existingEntities = rateDao.getAllLatestRates().first().associateBy { it.currencyCode }

                val entitiesToInsert = rates.mapIndexed { index, rate ->
                    val existing = existingEntities[rate.currency.name]

                    if (existing != null && existing.officialUpdateTime > rate.officialUpdateTime) {
                        return@mapIndexed existing
                    }

                    val isFav = existing?.isFavorite ?: CurrencyCode.DEFAULT_FAVORITES.contains(rate.currency)
                    val order = existing?.displayOrder ?: index

                    LatestRateEntity(
                        currencyCode = rate.currency.name,
                        spotBuy = rate.spotBuy?.toPlainString() ?: existing?.spotBuy,
                        spotSell = rate.spotSell?.toPlainString() ?: existing?.spotSell,
                        cashBuy = rate.cashBuy?.toPlainString() ?: existing?.cashBuy,
                        cashSell = rate.cashSell?.toPlainString() ?: existing?.cashSell,
                        officialUpdateTime = rate.officialUpdateTime,
                        localFetchTime = now,
                        source = rate.source,
                        isFavorite = isFav,
                        displayOrder = order
                    )
                }

                database.withTransaction {
                    rateDao.insertLatestRates(entitiesToInsert)

                    for (rate in rates) {
                        val existingHist = rateDao.getHistoryByOfficialTime(rate.currency.name, rate.officialUpdateTime)
                        if (existingHist == null) {
                            rateDao.insertHistory(
                                RateHistoryEntity(
                                    currencyCode = rate.currency.name,
                                    spotBuy = rate.spotBuy?.toPlainString(),
                                    spotSell = rate.spotSell?.toPlainString(),
                                    cashBuy = rate.cashBuy?.toPlainString(),
                                    cashSell = rate.cashSell?.toPlainString(),
                                    officialUpdateTime = rate.officialUpdateTime,
                                    timestamp = now,
                                    isOfficialDaily = false,
                                    source = rate.source
                                )
                            )
                        }
                    }
                }

                userPrefs.setLastSuccessTime(now)

                // Notify widgets to redraw with fresh data
                runCatching {
                    WidgetUpdater.updateAllWidgets(context)
                }

                // Evaluate alerts
                CoroutineScope(Dispatchers.IO).launch {
                    val alertResults = alertEngine.evaluateAllAlerts(rates)
                    val sound = userPrefs.soundEnabled.first()
                    val vibration = userPrefs.vibrationEnabled.first()
                    for (res in alertResults) {
                        if (res.isTriggered) {
                            notificationManager.sendAlertNotification(res, sound, vibration)
                        }
                    }
                }

                Result.success(rates)
            },
            onFailure = { error ->
                errorLogDao.insertErrorLog(
                    ErrorLogEntity(message = "Fetch failed: ${error.message ?: "Unknown error"}")
                )
                Result.failure(error)
            }
        )
    }

    suspend fun toggleFavorite(currency: CurrencyCode, isFavorite: Boolean) {
        rateDao.updateFavorite(currency.name, isFavorite)
    }

    suspend fun updateDisplayOrder(currency: CurrencyCode, order: Int) {
        rateDao.updateDisplayOrder(currency.name, order)
    }

    fun getHistoryForCurrency(code: String): Flow<List<RateHistoryEntity>> {
        return rateDao.getHistoryForCurrency(code)
    }

    suspend fun calculateRateChange(code: String, rateType: RateType, side: BankSide): RateChange {
        val historyList = rateDao.getLatestTwoHistory(code)
        if (historyList.size < 2) return RateChange(null, null)

        val latest = historyList[0].getRate(rateType, side)
        val previous = historyList[1].getRate(rateType, side)

        if (latest == null || previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return RateChange(null, null)
        }

        val diff = latest.subtract(previous)
        val pct = diff.divide(previous, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal("100"))
            .toDouble()

        return RateChange(diff, pct)
    }

    private fun LatestRateEntity.toDomain(): ExchangeRate {
        val code = CurrencyCode.fromString(currencyCode) ?: CurrencyCode.USD
        return ExchangeRate(
            currency = code,
            spotBuy = spotBuy?.toBigDecimalOrNull(),
            spotSell = spotSell?.toBigDecimalOrNull(),
            cashBuy = cashBuy?.toBigDecimalOrNull(),
            cashSell = cashSell?.toBigDecimalOrNull(),
            officialUpdateTime = officialUpdateTime,
            localFetchTime = localFetchTime,
            source = source
        )
    }

    private fun RateHistoryEntity.getRate(type: RateType, side: BankSide): BigDecimal? {
        val str = when (type) {
            RateType.SPOT -> if (side == BankSide.BUY) spotBuy else spotSell
            RateType.CASH -> if (side == BankSide.BUY) cashBuy else cashSell
        }
        return str?.toBigDecimalOrNull()
    }
}
