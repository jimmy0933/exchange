package com.fubon.fxmonitor

import com.fubon.fxmonitor.data.remote.FubonExchangeRateScraper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class FubonExchangeRateScraperTest {

    @Test
    fun testParse() {
        val scraper = FubonExchangeRateScraper()
        val html = File("src/test/resources/fubon.html").readText()
        val rates = scraper.parsePrimaryHtml(html)

        rates.forEach {
            println("Currency: ${it.currency.name}, Spot: ${it.spotBuy}/${it.spotSell}, Cash: ${it.cashBuy}/${it.cashSell}")
        }
    }
}
