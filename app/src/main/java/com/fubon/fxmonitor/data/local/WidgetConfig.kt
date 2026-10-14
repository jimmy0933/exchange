package com.fubon.fxmonitor.data.local

import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.RateType

data class WidgetConfig(
    val appWidgetId: String,
    val rateType: RateType = RateType.SPOT,
    val selectedCurrencies: List<CurrencyCode> = listOf(
        CurrencyCode.USD,
        CurrencyCode.JPY,
        CurrencyCode.CNY,
        CurrencyCode.EUR
    ),
    val bgTransparency: Int = 0
)
