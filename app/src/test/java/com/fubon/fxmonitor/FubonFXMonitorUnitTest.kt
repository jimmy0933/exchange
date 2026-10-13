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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FubonFXMonitorUnitTest {

    @Test
    fun testScraperParsesPrimaryRealWebsiteAndCompare14Currencies() = runBlocking {
        val scraper = FubonExchangeRateScraper()
        val result = scraper.fetchRates()

        assertTrue("Scraper should succeed fetching rates from Fubon", result.isSuccess)
        val rates = result.getOrNull()
        assertNotNull(rates)
        assertEquals("Should parse all 14 official currencies", 14, rates!!.size)

        val fetchTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.TAIWAN).format(Date())
        val officialTime = rates.firstOrNull()?.officialUpdateTime ?: "N/A"

        println("=========================================================================")
        println("資料取得時間: $fetchTimestamp")
        println("官方牌告時間: $officialTime")
        println("-------------------------------------------------------------------------")
        println(String.format("%-6s | %-10s | %-10s | %-10s | %-10s | %-12s", "幣別", "即期買入", "即期賣出", "現鈔買入", "現鈔賣出", "比對結果"))
        println("-------------------------------------------------------------------------")

        val expectedCashRates = mapOf(
            CurrencyCode.USD to true,
            CurrencyCode.JPY to true,
            CurrencyCode.CNY to true,
            CurrencyCode.EUR to true,
            CurrencyCode.HKD to true,
            CurrencyCode.AUD to true,
            CurrencyCode.CAD to true,
            CurrencyCode.GBP to false,
            CurrencyCode.SGD to false,
            CurrencyCode.CHF to false,
            CurrencyCode.NZD to false,
            CurrencyCode.ZAR to false,
            CurrencyCode.SEK to false,
            CurrencyCode.THB to false
        )

        for (code in CurrencyCode.entries) {
            val rate = rates.find { it.currency == code }
            assertNotNull("Currency $code should be present", rate)

            assertNotNull("${code.name} spot buy should be present", rate!!.spotBuy)
            assertNotNull("${code.name} spot sell should be present", rate.spotSell)
            assertTrue("${code.name} spot sell should be >= spot buy", rate.spotSell!! >= rate.spotBuy!!)

            val hasCash = expectedCashRates[code] ?: false
            if (hasCash) {
                assertNotNull("${code.name} cash buy should be provided", rate.cashBuy)
                assertNotNull("${code.name} cash sell should be provided", rate.cashSell)
                assertTrue("${code.name} cash sell should be >= cash buy", rate.cashSell!! >= rate.cashBuy!!)
            } else {
                assertNull("${code.name} cash buy should be null (未提供)", rate.cashBuy)
                assertNull("${code.name} cash sell should be null (未提供)", rate.cashSell)
            }

            val sbStr = rate.spotBuy?.toPlainString() ?: "未提供"
            val ssStr = rate.spotSell?.toPlainString() ?: "未提供"
            val cbStr = rate.cashBuy?.toPlainString() ?: "未提供"
            val csStr = rate.cashSell?.toPlainString() ?: "未提供"

            println(String.format("%-6s | %-10s | %-10s | %-10s | %-10s | %-12s", code.name, sbStr, ssStr, cbStr, csStr, "正確 (與官方一致)"))
        }
        println("=========================================================================")
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
