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

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IllegalStateException("HTTP error code: ${response.code}")

        val html = response.body?.string() ?: throw IllegalStateException("Empty body from primary source")
        val doc = Jsoup.parse(html)

        // Parse official update time e.g., 2026/10/13 09:23:31
        val timeElement = doc.selectFirst(".ico-date")
        val fullTimeText = timeElement?.text() ?: ""
        val officialTime = parseOfficialTime(fullTimeText)

        val rates = mutableListOf<ExchangeRate>()
        val rows = doc.select("table.rate-table tbody tr")

        for (row in rows) {
            val cols = row.select("td")
            if (cols.size >= 5) {
                val currencyText = cols[1].text()
                val code = parseCurrencyCode(currencyText) ?: continue

                val spotText = cols[3].text()
                val cashText = cols[4].text()

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

        if (rates.isEmpty()) {
            throw IllegalStateException("Failed to parse rates from primary source")
        }

        return rates
    }

    private fun fetchBackup(): List<ExchangeRate> {
        val request = Request.Builder()
            .url(backupUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IllegalStateException("HTTP error code from backup: ${response.code}")

        val html = response.body?.string() ?: throw IllegalStateException("Empty body from backup source")
        val doc = Jsoup.parse(html)

        // Fallback parser implementation for backup official page
        val rates = mutableListOf<ExchangeRate>()
        val timeText = doc.select("span, div").find { it.text().contains("時間") || it.text().contains("日期") }?.text() ?: ""
        val officialTime = parseOfficialTime(timeText).ifEmpty { "最新牌價" }

        val rows = doc.select("table tr")
        for (row in rows) {
            val text = row.text()
            for (code in CurrencyCode.entries) {
                if (text.contains(code.name) || text.contains(code.displayName)) {
                    val numbers = Regex("""\d+\.\d+""").findAll(text).map { it.value }.toList()
                    if (numbers.size >= 4) {
                        rates.add(
                            ExchangeRate(
                                currency = code,
                                spotBuy = numbers[0].toBigDecimalOrNull(),
                                spotSell = numbers[1].toBigDecimalOrNull(),
                                cashBuy = numbers[2].toBigDecimalOrNull(),
                                cashSell = numbers[3].toBigDecimalOrNull(),
                                officialUpdateTime = officialTime,
                                source = "台北富邦銀行(無障礙版)"
                            )
                        )
                    } else if (numbers.size >= 2) {
                        rates.add(
                            ExchangeRate(
                                currency = code,
                                spotBuy = numbers[0].toBigDecimalOrNull(),
                                spotSell = numbers[1].toBigDecimalOrNull(),
                                cashBuy = null,
                                cashSell = null,
                                officialUpdateTime = officialTime,
                                source = "台北富邦銀行(無障礙版)"
                            )
                        )
                    }
                    break
                }
            }
        }

        if (rates.isEmpty()) {
            throw IllegalStateException("Failed to parse rates from backup source")
        }

        return rates
    }

    private fun parseOfficialTime(text: String): String {
        val regex = Regex("""\d{4}/\d{2}/\d{2}\s+\d{2}:\d{2}:\d{2}""")
        return regex.find(text)?.value ?: text.replace("更新", "").trim()
    }

    private fun parseCurrencyCode(text: String): CurrencyCode? {
        val codeMatch = Regex("""\(([A-Z]{3})\)""").find(text)
        if (codeMatch != null) {
            return CurrencyCode.fromString(codeMatch.groupValues[1])
        }
        return CurrencyCode.entries.find { text.contains(it.name) || text.contains(it.displayName) }
    }

    private fun parsePair(text: String): Pair<BigDecimal?, BigDecimal?> {
        if (text.contains("---") || text.isBlank()) {
            return Pair(null, null)
        }
        val parts = text.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (parts.size >= 2) {
            val buy = parts[0].toBigDecimalOrNull()
            val sell = parts[1].toBigDecimalOrNull()
            return Pair(buy, sell)
        }
        return Pair(null, null)
    }
}
