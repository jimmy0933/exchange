package com.fubon.fxmonitor

import com.fubon.fxmonitor.data.remote.FubonExchangeRateScraper
import com.fubon.fxmonitor.domain.model.BankSide
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.ExchangeRate
import com.fubon.fxmonitor.domain.model.RateType
import com.fubon.fxmonitor.domain.usecase.ConversionDirection
import com.fubon.fxmonitor.domain.usecase.ExchangeCalculatorUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class FubonFXMonitorUnitTest {

    @Test
    fun testScraperParsesPrimaryRealWebsite() = runBlocking {
        val scraper = FubonExchangeRateScraper()
        val result = scraper.fetchRates()

        assertTrue("Scraper should succeed fetching rates from Fubon", result.isSuccess)
        val rates = result.getOrNull()
        assertNotNull(rates)
        assertEquals("Should parse all 14 official currencies", 14, rates!!.size)

        // Check USD is present with buy/sell quotes
        val usd = rates.find { it.currency == CurrencyCode.USD }
        assertNotNull("USD should be present", usd)
        assertNotNull("USD spot buy should be non-null", usd!!.spotBuy)
        assertNotNull("USD spot sell should be non-null", usd.spotSell)
        assertTrue("Spot sell rate should be higher than buy rate", usd.spotSell!! > usd.spotBuy!!)

        // Check ZAR cash rates are N/A (null) as officially published
        val zar = rates.find { it.currency == CurrencyCode.ZAR }
        assertNotNull("ZAR should be present", zar)
        assertNull("ZAR cash buy should be null (N/A)", zar!!.cashBuy)
        assertNull("ZAR cash sell should be null (N/A)", zar.cashSell)
    }

    @Test
    fun testExchangeCalculatorBigDecimalPrecision() {
        val calculator = ExchangeCalculatorUseCase()

        val rate = ExchangeRate(
            currency = CurrencyCode.JPY,
            spotBuy = BigDecimal("0.2002"),
            spotSell = BigDecimal("0.2038"),
            cashBuy = BigDecimal("0.1975"),
            cashSell = BigDecimal("0.2065"),
            officialUpdateTime = "2026/10/13 09:23:31"
        )

        // TWD 10,000 -> Foreign (JPY cash sell rate 0.2065)
        // Expected: 10,000 / 0.2065 = 48426.1501...
        val result = calculator.convert(
            amount = BigDecimal("10000"),
            rate = rate,
            direction = ConversionDirection.TWD_TO_FOREIGN,
            rateType = RateType.CASH
        )

        assertNotNull(result)
        assertEquals(CurrencyCode.JPY.name, result!!.currencyCode)
        assertEquals(RateType.CASH, result.rateType)
        assertEquals(BankSide.SELL, result.bankSide)
        assertEquals(BigDecimal("0.2065"), result.rateUsed)
        assertEquals(BigDecimal("48426.1501"), result.outputAmount)
    }

    @Test
    fun testCurrencyCodeEnumMapping() {
        assertEquals(CurrencyCode.USD, CurrencyCode.fromString("USD"))
        assertEquals(CurrencyCode.JPY, CurrencyCode.fromString("jpy"))
        assertEquals(CurrencyCode.EUR, CurrencyCode.fromString("歐元"))
        assertNull(CurrencyCode.fromString("UNKNOWN"))
    }
}
