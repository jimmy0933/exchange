package com.fubon.fxmonitor.data.remote

import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.ExchangeRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
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

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP error code: ${response.code}")
            val html = response.body?.string() ?: throw IllegalStateException("Empty body from primary source")
            parseHtml(html, sourceName = "台北富邦銀行")
        }
    }

    private fun fetchBackup(): List<ExchangeRate> {
        val request = Request.Builder()
            .url(backupUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP error code from backup: ${response.code}")
            val html = response.body?.string() ?: throw IllegalStateException("Empty body from backup source")
            parseHtml(html, sourceName = "台北富邦銀行(備援)")
        }
    }

    fun parseHtml(html: String, sourceName: String = "台北富邦銀行"): List<ExchangeRate> {
        val doc = Jsoup.parse(html)

        val officialTime = parseOfficialTime(doc)
            ?: throw IllegalStateException("Failed to parse official update time from HTML")

        val ratesMap = mutableMapOf<CurrencyCode, ExchangeRate>()
        val tables = doc.select("table")

        for (table in tables) {
            val headerCols = table.select("thead tr th, thead tr td, tr:first-child th, tr:first-child td")
            var currencyColIdx = -1
            var spotColIdx = -1
            var cashColIdx = -1

            headerCols.forEachIndexed { idx, cell ->
                val txt = cell.text()
                if (txt.contains("幣別")) currencyColIdx = idx
                if (txt.contains("即期")) spotColIdx = idx
                if (txt.contains("現鈔")) cashColIdx = idx
            }

            // Fallback column index defaults if header is standard Fubon format (1: currency, 3: spot, 4: cash)
            if (currencyColIdx == -1) currencyColIdx = 1
            if (spotColIdx == -1) spotColIdx = 3
            if (cashColIdx == -1) cashColIdx = 4

            val rows = table.select("tbody tr, tr")

            for (row in rows) {
                val cols = row.select("td, th")
                if (cols.size <= currencyColIdx) continue

                val currencyText = cols[currencyColIdx].text()
                val code = parseCurrencyCode(currencyText) ?: continue

                if (ratesMap.containsKey(code)) continue

                val spotText = if (cols.size > spotColIdx) cols[spotColIdx].text() else ""
                val cashText = if (cols.size > cashColIdx) cols[cashColIdx].text() else ""

                val (spotBuy, spotSell) = parsePair(spotText)
                val (cashBuy, cashSell) = parsePair(cashText)

                // Must have at least valid spot quotes or cash quotes
                if (spotBuy == null && spotSell == null && cashBuy == null && cashSell == null) {
                    continue
                }

                ratesMap[code] = ExchangeRate(
                    currency = code,
                    spotBuy = spotBuy,
                    spotSell = spotSell,
                    cashBuy = cashBuy,
                    cashSell = cashSell,
                    officialUpdateTime = officialTime,
                    source = sourceName
                )
            }

            if (ratesMap.isNotEmpty()) break
        }

        if (ratesMap.isEmpty()) {
            throw IllegalStateException("Failed to parse rates from HTML")
        }

        return ratesMap.values.toList()
    }

    private fun parseOfficialTime(doc: Document): String? {
        val fullText = doc.text()
        val regex = Regex("""\d{4}/\d{2}/\d{2}\s+\d{2}:\d{2}(:\d{2})?""")
        val match = regex.find(fullText)
        return match?.value
    }

    private fun parseCurrencyCode(text: String): CurrencyCode? {
        val codeMatch = Regex("""\(([A-Z]{3})\)""").find(text)
        if (codeMatch != null) {
            return CurrencyCode.fromString(codeMatch.groupValues[1])
        }
        return CurrencyCode.entries.find {
            text.contains(it.name, ignoreCase = true) || text.contains(it.displayName)
        }
    }

    private fun parsePair(text: String): Pair<BigDecimal?, BigDecimal?> {
        if (text.contains("---") || text.contains("N/A") || text.isBlank()) {
            return Pair(null, null)
        }
        val numbers = Regex("""\d+\.\d+""").findAll(text).map { it.value }.toList()
        if (numbers.size >= 2) {
            val buy = numbers[0].toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
            val sell = numbers[1].toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
            return Pair(buy, sell)
        }
        return Pair(null, null)
    }
}
