package com.fubon.fxmonitor.data.local

import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.RateType

enum class WidgetDisplayMode(val displayName: String) {
    FLAG_AND_CODE("國旗 + 幣別"),
    FLAG_ONLY("僅顯示國旗");

    companion object {
        fun fromString(value: String?): WidgetDisplayMode {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: FLAG_ONLY
        }
    }
}

data class WidgetConfig(
    val appWidgetId: String,
    val rateType: RateType = RateType.SPOT,
    val selectedCurrencies: List<CurrencyCode> = listOf(
        CurrencyCode.USD,
        CurrencyCode.JPY,
        CurrencyCode.CNY,
        CurrencyCode.EUR
    ),
    val bgTransparency: Int = 0,
    val fontSizeScale: Int = 100,
    val displayMode: WidgetDisplayMode = WidgetDisplayMode.FLAG_ONLY
)
