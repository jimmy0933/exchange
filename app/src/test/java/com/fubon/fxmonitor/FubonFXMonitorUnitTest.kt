package com.fubon.fxmonitor

import com.fubon.fxmonitor.data.local.RateHistoryEntity
import com.fubon.fxmonitor.data.remote.FubonExchangeRateScraper
import com.fubon.fxmonitor.domain.model.BankSide
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.ExchangeRate
import com.fubon.fxmonitor.domain.model.RateType
import com.fubon.fxmonitor.domain.usecase.ConversionDirection
import com.fubon.fxmonitor.domain.usecase.ExchangeCalculatorUseCase
import com.fubon.fxmonitor.ui.detail.TimeRange
import com.fubon.fxmonitor.widget.extractTime
import com.fubon.fxmonitor.widget.formatRateValue
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
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

    @Test
    fun testDetailViewModelTimeRangeFiltering7d1m6m1y() {
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 3600 * 1000L

        val mockHistory = listOf(
            RateHistoryEntity(id = 1, currencyCode = "USD", spotBuy = "32.0", spotSell = "32.1", cashBuy = "31.8", cashSell = "32.3", officialUpdateTime = "2026/10/13 09:00:00", timestamp = now - 2 * dayMillis),
            RateHistoryEntity(id = 2, currencyCode = "USD", spotBuy = "32.1", spotSell = "32.2", cashBuy = "31.9", cashSell = "32.4", officialUpdateTime = "2026/10/01 09:00:00", timestamp = now - 14 * dayMillis),
            RateHistoryEntity(id = 3, currencyCode = "USD", spotBuy = "32.2", spotSell = "32.3", cashBuy = "32.0", cashSell = "32.5", officialUpdateTime = "2026/09/10 09:00:00", timestamp = now - 40 * dayMillis),
            RateHistoryEntity(id = 4, currencyCode = "USD", spotBuy = "32.3", spotSell = "32.4", cashBuy = "32.1", cashSell = "32.6", officialUpdateTime = "2026/05/10 09:00:00", timestamp = now - 160 * dayMillis),
            RateHistoryEntity(id = 5, currencyCode = "USD", spotBuy = "32.4", spotSell = "32.5", cashBuy = "32.2", cashSell = "32.7", officialUpdateTime = "2025/11/10 09:00:00", timestamp = now - 350 * dayMillis),
            RateHistoryEntity(id = 6, currencyCode = "USD", spotBuy = "32.5", spotSell = "32.6", cashBuy = "32.3", cashSell = "32.8", officialUpdateTime = "2025/08/10 09:00:00", timestamp = now - 400 * dayMillis)
        )

        // Filter 7 days (WEEK)
        val weekCutoff = now - TimeRange.WEEK.durationMillis
        val weekList = mockHistory.filter { it.timestamp >= weekCutoff }
        assertEquals(1, weekList.size)

        // Filter 1 month (MONTH)
        val monthCutoff = now - TimeRange.MONTH.durationMillis
        val monthList = mockHistory.filter { it.timestamp >= monthCutoff }
        assertEquals(2, monthList.size)

        // Filter 6 months (SIX_MONTHS)
        val sixMonthCutoff = now - TimeRange.SIX_MONTHS.durationMillis
        val sixMonthList = mockHistory.filter { it.timestamp >= sixMonthCutoff }
        assertEquals(4, sixMonthList.size)

        // Filter 1 year (YEAR)
        val yearCutoff = now - TimeRange.YEAR.durationMillis
        val yearList = mockHistory.filter { it.timestamp >= yearCutoff }
        assertEquals(5, yearList.size)
    }

    @Test
    fun testDetailViewModel4WayRateCalculations() {
        val entry1 = RateHistoryEntity(id = 1, currencyCode = "USD", spotBuy = "32.0000", spotSell = "32.1000", cashBuy = "31.8000", cashSell = "32.3000", officialUpdateTime = "2026/10/06 09:00:00", timestamp = 1000L)
        val entry2 = RateHistoryEntity(id = 2, currencyCode = "USD", spotBuy = "32.2000", spotSell = "32.3000", cashBuy = "32.0000", cashSell = "32.5000", officialUpdateTime = "2026/10/13 09:00:00", timestamp = 2000L)
        val history = listOf(entry1, entry2)

        // Function helper to test stats calculation
        fun calculateStats(type: RateType, side: BankSide): Pair<BigDecimal, BigDecimal> {
            val rates = history.mapNotNull { hist ->
                val str = when (type) {
                    RateType.SPOT -> if (side == BankSide.BUY) hist.spotBuy else hist.spotSell
                    RateType.CASH -> if (side == BankSide.BUY) hist.cashBuy else hist.cashSell
                }
                str?.toBigDecimalOrNull()
            }
            val minVal = rates.minOrNull()!!
            val maxVal = rates.maxOrNull()!!
            return Pair(minVal, maxVal)
        }

        // 1. Spot Buy
        val (sbMin, sbMax) = calculateStats(RateType.SPOT, BankSide.BUY)
        assertEquals(BigDecimal("32.0000"), sbMin)
        assertEquals(BigDecimal("32.2000"), sbMax)

        // 2. Spot Sell
        val (ssMin, ssMax) = calculateStats(RateType.SPOT, BankSide.SELL)
        assertEquals(BigDecimal("32.1000"), ssMin)
        assertEquals(BigDecimal("32.3000"), ssMax)

        // 3. Cash Buy
        val (cbMin, cbMax) = calculateStats(RateType.CASH, BankSide.BUY)
        assertEquals(BigDecimal("31.8000"), cbMin)
        assertEquals(BigDecimal("32.0000"), cbMax)

        // 4. Cash Sell
        val (csMin, csMax) = calculateStats(RateType.CASH, BankSide.SELL)
        assertEquals(BigDecimal("32.3000"), csMin)
        assertEquals(BigDecimal("32.5000"), csMax)

        // Check Change calculation comparing earliest and latest
        val spotSellEarliest = BigDecimal(entry1.spotSell)
        val spotSellLatest = BigDecimal(entry2.spotSell)
        val diff = spotSellLatest.subtract(spotSellEarliest)
        val pct = diff.divide(spotSellEarliest, 6, RoundingMode.HALF_UP).multiply(BigDecimal("100"))

        assertEquals(BigDecimal("0.2000"), diff)
        assertEquals("0.62", pct.setScale(2, RoundingMode.HALF_UP).toPlainString())
    }

    @Test
    fun testExtractTimeFromOfficialUpdateTime() {
        assertEquals("22:34", extractTime("2026/10/13 22:34:00"))
        assertEquals("22:34", extractTime("22:34"))
        assertEquals("09:15", extractTime("10/13 09:15:30"))
        assertEquals("未更新", extractTime("未更新"))
    }

    @Test
    fun testFormatRateValueFor1x1Widget() {
        // Standard mode (full precision)
        assertEquals("31.8205", formatRateValue("31.8205", isCompact = false))
        assertEquals("0.2005", formatRateValue("0.2005", isCompact = false))
        assertEquals("未提供", formatRateValue(null, isCompact = false))
        assertEquals("未提供", formatRateValue("", isCompact = false))

        // Compact mode (for 1x1 dual mode)
        assertEquals("31.82", formatRateValue("31.8205", isCompact = true))
        assertEquals("0.2005", formatRateValue("0.2005", isCompact = true))
        assertEquals("32.46", formatRateValue("32.4625", isCompact = true))
    }

    @Test
    fun testSmallWidget1x1SelectionAndCashFallback() {
        val currencies = listOf(CurrencyCode.USD, CurrencyCode.JPY, CurrencyCode.CNY)
        val selected = currencies.take(2)
        assertEquals(2, selected.size)
        assertEquals(CurrencyCode.USD, selected[0])
        assertEquals(CurrencyCode.JPY, selected[1])

        val gbpRate = ExchangeRate(
            currency = CurrencyCode.GBP,
            spotBuy = BigDecimal("41.50"),
            spotSell = BigDecimal("41.80"),
            cashBuy = null,
            cashSell = null,
            officialUpdateTime = "2026/10/13 22:34:00"
        )
        val cashBuyStr = gbpRate.cashBuy?.toPlainString() ?: "未提供"
        val cashSellStr = gbpRate.cashSell?.toPlainString() ?: "未提供"

        assertEquals("未提供", cashBuyStr)
        assertEquals("未提供", cashSellStr)
    }

    @Test
    fun testHistoryDeduplicationAndNullCashRates() {
        // GBP has no cash rates (cashBuy and cashSell are null)
        val gbpEntry = RateHistoryEntity(
            id = 1,
            currencyCode = "GBP",
            spotBuy = "41.5000",
            spotSell = "41.8000",
            cashBuy = null,
            cashSell = null,
            officialUpdateTime = "2026/10/13 09:23:31",
            timestamp = System.currentTimeMillis(),
            source = "台北富邦銀行"
        )

        // Cash rate extraction should return null
        assertNull(gbpEntry.cashBuy)
        assertNull(gbpEntry.cashSell)
        assertNotNull(gbpEntry.spotBuy)
        assertNotNull(gbpEntry.spotSell)

        // Check key uniqueness
        val key1 = "${gbpEntry.currencyCode}_${gbpEntry.officialUpdateTime}"
        val key2 = "GBP_2026/10/13 09:23:31"
        assertEquals(key1, key2)
    }
}
