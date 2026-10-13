package com.fubon.fxmonitor.data.remote

import com.fubon.fxmonitor.domain.model.CurrencyCode
import org.junit.Assert.*
import org.junit.Test
import java.io.InputStreamReader

class FubonExchangeRateScraperTest {

    private fun loadFixture(filename: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream("fixtures/$filename")
            ?: throw IllegalArgumentException("Fixture $filename not found in test resources")
        return InputStreamReader(stream, Charsets.UTF_8).readText()
    }

    @Test
    fun testParseNormalHtmlFixture() {
        val scraper = FubonExchangeRateScraper()
        val html = loadFixture("fubon_rates_normal.html")
        val rates = scraper.parseHtml(html)

        assertEquals(14, rates.size)
        assertEquals("2026/10/13 09:23:31", rates.first().officialUpdateTime)

        val usd = rates.find { it.currency == CurrencyCode.USD }
        assertNotNull(usd)
        assertEquals("32.1740", usd?.spotBuy?.toPlainString())
        assertEquals("32.2740", usd?.spotSell?.toPlainString())
        assertEquals("31.9120", usd?.cashBuy?.toPlainString())
        assertEquals("32.4940", usd?.cashSell?.toPlainString())

        val zar = rates.find { it.currency == CurrencyCode.ZAR }
        assertNotNull(zar)
        assertNotNull(zar?.spotBuy)
        assertNull(zar?.cashBuy)
        assertNull(zar?.cashSell)
    }

    @Test
    fun testParseMissingCashFixture() {
        val scraper = FubonExchangeRateScraper()
        val html = loadFixture("fubon_rates_missing_cash.html")
        val rates = scraper.parseHtml(html)

        val usd = rates.find { it.currency == CurrencyCode.USD }
        assertNotNull(usd)
        assertNotNull(usd?.spotBuy)
        assertNull(usd?.cashBuy)
        assertNull(usd?.cashSell)
    }

    @Test
    fun testParseAlteredColumnsFixture() {
        val scraper = FubonExchangeRateScraper()
        val html = loadFixture("fubon_rates_altered_columns.html")
        val rates = scraper.parseHtml(html)

        val usd = rates.find { it.currency == CurrencyCode.USD }
        assertNotNull(usd)
        assertEquals("32.1500", usd?.spotBuy?.toPlainString())
        assertEquals("32.2500", usd?.spotSell?.toPlainString())
        assertEquals("31.9000", usd?.cashBuy?.toPlainString())
        assertEquals("32.5000", usd?.cashSell?.toPlainString())
    }

    @Test(expected = IllegalStateException::class)
    fun testParseInvalidHtmlFixture() {
        val scraper = FubonExchangeRateScraper()
        val html = loadFixture("fubon_rates_invalid.html")
        scraper.parseHtml(html)
    }
}
