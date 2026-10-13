package com.fubon.fxmonitor.domain.model

import java.math.BigDecimal

enum class CurrencyCode(val displayName: String, val flagEmoji: String) {
    USD("美元", "🇺🇸"),
    JPY("日圓", "🇯🇵"),
    CNY("人民幣", "🇨🇳"),
    EUR("歐元", "🇪🇺"),
    HKD("港幣", "🇭🇰"),
    AUD("澳幣", "🇦🇺"),
    CAD("加拿大幣", "🇨🇦"),
    GBP("英鎊", "🇬🇧"),
    SGD("新加坡幣", "🇸🇬"),
    CHF("瑞士法郎", "🇨🇭"),
    NZD("紐西蘭幣", "🇳🇿"),
    ZAR("南非幣", "🇿🇦"),
    SEK("瑞典幣", "🇸🇪"),
    THB("泰國銖", "🇹🇭");

    companion object {
        val DEFAULT_FAVORITES = listOf(USD, JPY, CNY, EUR)

        fun fromString(code: String): CurrencyCode? {
            val trimmed = code.trim()
            return entries.find {
                it.name.equals(trimmed, ignoreCase = true) ||
                it.displayName.equals(trimmed, ignoreCase = true)
            }
        }
    }
}

enum class RateType {
    SPOT,
    CASH
}

enum class BankSide {
    BUY,
    SELL
}

data class ExchangeRate(
    val currency: CurrencyCode,
    val spotBuy: BigDecimal?,
    val spotSell: BigDecimal?,
    val cashBuy: BigDecimal?,
    val cashSell: BigDecimal?,
    val officialUpdateTime: String,
    val localFetchTime: Long = System.currentTimeMillis(),
    val source: String = "台北富邦銀行"
) {
    fun getRate(type: RateType, side: BankSide): BigDecimal? {
        return when (type) {
            RateType.SPOT -> if (side == BankSide.BUY) spotBuy else spotSell
            RateType.CASH -> if (side == BankSide.BUY) cashBuy else cashSell
        }
    }
}

data class RateChange(
    val amount: BigDecimal?,
    val percentage: Double?
)
