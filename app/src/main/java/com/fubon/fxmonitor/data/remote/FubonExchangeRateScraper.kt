package com.fubon.fxmonitor.data.remote

import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.ExchangeRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FubonExchangeRateScraper @Inject constructor() : ExchangeRateProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val primaryUrl = "https://www.fubon.com/Fubon_Portal/banking/Personal/deposit/exchange_rate/exchange_rate1.jsp"
    private val backupUrl = "https://accessible.taipeifubon.com.tw/BFS/cdfqu/cdfqu006/CDFQU006_Home.faces"

    override suspend fun fetchRates(): Result<List<ExchangeRate>> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                fetchPrimary()
            } catch (e: Exception) {
                // Attempt fallback if primary fails
                fetchBackup()
            }
        }
    }

    private fun fetchPrimary(): List<ExchangeRate> {
        val request = Request.Builder()
            .url(primaryUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()

        val html = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP error code: ${response.code}")
            response.body?.string() ?: throw IllegalStateException("Empty body from primary source")
        }
        return parsePrimaryHtml(html)
    }

    fun parsePrimaryHtml(html: String): List<ExchangeRate> {
        val doc = Jsoup.parse(html)
        // Parse official update time e.g., 2026/10/13 09:23:31
        val timeElement = doc.selectFirst(".ico-date")
        val fullTimeText = timeElement?.text() ?: ""
        val officialTime = parseOfficialTime(fullTimeText)

        val rates = mutableListOf<ExchangeRate>()
        val rows = doc.select("table.rate-table tbody tr")

        // Find column indices from headers
        val headers = doc.select("table.rate-table thead tr th, table.rate-table thead tr td")
        var spotIndex = -1
        var cashIndex = -1
        var currencyIndex = -1
        headers.forEachIndexed { index, element ->
            val text = element.text()
            if (text.contains("幣別")) {
                currencyIndex = index
            } else if (text.contains("即期")) {
                spotIndex = index
            } else if (text.contains("現鈔")) {
                cashIndex = index
            }
        }

        if (currencyIndex == -1 || spotIndex == -1 || cashIndex == -1) {
            // Fallback to defaults
            currencyIndex = 1
            spotIndex = 3
            cashIndex = 4
        }

        val uniqueCodes = mutableSetOf<CurrencyCode>()

        for (row in rows) {
            val cols = row.select("td")
            if (cols.size > maxOf(currencyIndex, spotIndex, cashIndex)) {
                val currencyText = cols[currencyIndex].text()
                val code = parseCurrencyCode(currencyText) ?: continue
                if (!uniqueCodes.add(code)) continue // Ensure uniqueness

                val spotText = cols[spotIndex].text()
                val cashText = cols[cashIndex].text()

                val (spotBuy, spotSell) = parsePair(spotText)
                val (cashBuy, cashSell) = parsePair(cashText)

                rates.add(
                    ExchangeRate(
                        currency = code,
                        spotBuy = spotBuy,
                        spotSell = spotSell,
                        cashBuy = cashBuy,
                        cashSell = cashSell,
                        officialUpdateTime = officialTime,
                        source = "台北富邦銀行"
                    )
                )
            }
        }

        if (rates.size < 5) {
            throw IllegalStateException("Failed to parse rates from primary source")
        }

        return rates
    }

    private fun fetchBackup(): List<ExchangeRate> {
        val request = Request.Builder()
            .url(backupUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()

        val html = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP error code from backup: ${response.code}")
            response.body?.string() ?: throw IllegalStateException("Empty body from backup source")
        }
        return parseBackupHtml(html)
    }

    fun parseBackupHtml(html: String): List<ExchangeRate> {
        val doc = Jsoup.parse(html)
        val timeText = doc.select("tr, td, span, div").find { it.text().contains("更新時間") || it.text().contains("時間") }?.text() ?: ""
        val officialTime = parseOfficialTime(timeText).ifEmpty { "最新牌價" }

        val rates = mutableListOf<ExchangeRate>()
        val rows = doc.select("table tr")
        val uniqueCodes = mutableSetOf<CurrencyCode>()
        for (row in rows) {
            val cols = row.select("td")
            if (cols.size == 5) {
                val currencyText = cols[0].text()
                val code = parseCurrencyCode(currencyText) ?: continue
                if (!uniqueCodes.add(code)) continue

                val spotBuy = parseSingleRate(cols[1].text())
                val spotSell = parseSingleRate(cols[2].text())
                val cashBuy = parseSingleRate(cols[3].text())
                val cashSell = parseSingleRate(cols[4].text())

                rates.add(
                    ExchangeRate(
                        currency = code,
                        spotBuy = spotBuy,
                        spotSell = spotSell,
                        cashBuy = cashBuy,
                        cashSell = cashSell,
                        officialUpdateTime = officialTime,
                        source = "台北富邦銀行(無障礙版)"
                    )
                )
            }
        }

        if (rates.size < 5) {
            throw IllegalStateException("Failed to parse rates from backup source")
        }

        return rates
    }

    private fun parseOfficialTime(text: String): String {
        val regex = Regex("""\d{4}/\d{2}/\d{2}\s+\d{2}:\d{2}:\d{2}""")
        val match = regex.find(text)?.value
        if (match != null) return match
        val fallbackRegex = Regex("""\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}""")
        val matchFallback = fallbackRegex.find(text)?.value
        if (matchFallback != null) return matchFallback
        return text.replace("更新", "").replace("時間：", "").trim()
    }

    private fun parseCurrencyCode(text: String): CurrencyCode? {
        val codeMatch = Regex("""\(([A-Z]{3})\)""").find(text)
        if (codeMatch != null) {
            return CurrencyCode.fromString(codeMatch.groupValues[1])
        }
        return CurrencyCode.entries.find { text.contains(it.name) || text.contains(it.displayName) }
    }

    private fun parseSingleRate(text: String): BigDecimal? {
        val trimmed = text.trim()
        if (trimmed.contains("---") || trimmed.isEmpty()) {
            return null
        }
        return trimmed.toBigDecimalOrNull()
    }

    private fun parsePair(text: String): Pair<BigDecimal?, BigDecimal?> {
        val trimmed = text.trim()
        val parts = trimmed.split(Regex("""\s+""")).filter { it.isNotBlank() }
        val buy = parts.getOrNull(0)?.let { if (it.contains("---")) null else it.toBigDecimalOrNull() }
        val sell = parts.getOrNull(1)?.let { if (it.contains("---")) null else it.toBigDecimalOrNull() }
        return Pair(buy, sell)
    }
}
