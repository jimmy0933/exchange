package com.fubon.fxmonitor.domain.usecase

import com.fubon.fxmonitor.data.local.AlertDao
import com.fubon.fxmonitor.data.local.ExchangeAlertEntity
import com.fubon.fxmonitor.data.local.RateDao
import com.fubon.fxmonitor.data.local.RateHistoryEntity
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.ExchangeRate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ExchangeAlertEngineTest {

    private lateinit var mockAlertDao: FakeAlertDao
    private lateinit var mockRateDao: FakeRateDao
    private lateinit var alertEngine: ExchangeAlertEngine

    @Before
    fun setUp() {
        mockAlertDao = FakeAlertDao()
        mockRateDao = FakeRateDao()
        alertEngine = ExchangeAlertEngine(mockAlertDao, mockRateDao)
    }

    @Test
    fun testGreaterEqualConditionTrigger() = runBlocking {
        val alert = ExchangeAlertEntity(
            id = 1,
            currencyCode = "USD",
            rateType = "SPOT",
            bankSide = "SELL",
            triggerCondition = "GREATER_EQUAL",
            targetValue = 32.00,
            isEnabled = true
        )
        mockAlertDao.alerts.add(alert)

        val rate = ExchangeRate(
            currency = CurrencyCode.USD,
            spotBuy = BigDecimal("31.90"),
            spotSell = BigDecimal("32.10"),
            cashBuy = null,
            cashSell = null,
            officialUpdateTime = "2026/10/13 09:00:00"
        )

        val results = alertEngine.evaluateAllAlerts(listOf(rate))
        assertEquals(1, results.size)
        assertTrue(results[0].isTriggered)
        assertEquals(32.10, results[0].currentRateValue, 0.0001)
    }

    @Test
    fun testDailyChangePctConditionTrigger() = runBlocking {
        val alert = ExchangeAlertEntity(
            id = 2,
            currencyCode = "USD",
            rateType = "SPOT",
            bankSide = "SELL",
            triggerCondition = "DAILY_CHANGE_PCT",
            targetValue = 1.0, // 1% change
            isEnabled = true
        )
        mockAlertDao.alerts.add(alert)

        // Previous trading day history
        mockRateDao.history.add(
            RateHistoryEntity(
                currencyCode = "USD",
                spotBuy = "31.50",
                spotSell = "31.50",
                cashBuy = null,
                cashSell = null,
                officialUpdateTime = "2026/10/12 16:00:00",
                timestamp = System.currentTimeMillis() - 86400000
            )
        )

        // Today rate: 32.00 (change from 31.50 is ~1.58%)
        val rate = ExchangeRate(
            currency = CurrencyCode.USD,
            spotBuy = BigDecimal("31.90"),
            spotSell = BigDecimal("32.00"),
            cashBuy = null,
            cashSell = null,
            officialUpdateTime = "2026/10/13 09:00:00"
        )

        val results = alertEngine.evaluateAllAlerts(listOf(rate))
        assertEquals(1, results.size)
        assertTrue(results[0].isTriggered)
    }

    // Simple Fakes for testing
    class FakeAlertDao : AlertDao {
        val alerts = mutableListOf<ExchangeAlertEntity>()

        override fun getAllAlerts() = TODO()
        override suspend fun getActiveAlerts() = alerts.filter { it.isEnabled }
        override suspend fun getAlertById(id: Long) = alerts.find { it.id == id }
        override suspend fun insertAlert(alert: ExchangeAlertEntity) = 1L
        override suspend fun updateAlert(alert: ExchangeAlertEntity) {
            val idx = alerts.indexOfFirst { it.id == alert.id }
            if (idx >= 0) alerts[idx] = alert
        }
        override suspend fun deleteAlert(id: Long) {}
        override fun getAlertHistory() = TODO()
        override suspend fun insertAlertHistory(log: com.fubon.fxmonitor.data.local.AlertHistoryEntity) {}
        override suspend fun clearAlertHistory() {}
    }

    class FakeRateDao : RateDao {
        val history = mutableListOf<RateHistoryEntity>()

        override fun getAllLatestRates() = TODO()
        override suspend fun getLatestRate(code: String) = null
        override suspend fun insertLatestRates(rates: List<com.fubon.fxmonitor.data.local.LatestRateEntity>) {}
        override suspend fun updateFavorite(code: String, isFavorite: Boolean) {}
        override suspend fun updateDisplayOrder(code: String, order: Int) {}
        override fun getHistoryForCurrency(code: String) = TODO()
        override suspend fun getHistoryByOfficialTime(code: String, officialTime: String) = null
        override suspend fun getLatestTwoHistory(code: String) = history.take(2)
        override suspend fun getAllHistoryListForCurrency(code: String) = history
        override suspend fun insertHistory(history: RateHistoryEntity) = 1L
        override suspend fun clearHistory() {}
    }
}
