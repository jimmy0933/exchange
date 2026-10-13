package com.fubon.fxmonitor.domain.usecase

import com.fubon.fxmonitor.domain.model.BankSide
import com.fubon.fxmonitor.domain.model.ExchangeRate
import com.fubon.fxmonitor.domain.model.RateType
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

enum class ConversionDirection {
    TWD_TO_FOREIGN,
    FOREIGN_TO_TWD
}

data class ConversionResult(
    val inputAmount: BigDecimal,
    val outputAmount: BigDecimal,
    val rateUsed: BigDecimal,
    val currencyCode: String,
    val rateType: RateType,
    val bankSide: BankSide,
    val officialUpdateTime: String,
    val disclaimer: String = "註：實際成交價格可能受到優惠、手續費及銀行最終報價影響。"
)

@Singleton
class ExchangeCalculatorUseCase @Inject constructor() {

    fun convert(
        amount: BigDecimal,
        rate: ExchangeRate,
        direction: ConversionDirection,
        rateType: RateType
    ): ConversionResult? {
        // TWD -> Foreign Foreign buy rate applied when bank sells foreign currency to user
        // Bank sells to user = BankSide.SELL
        // User sells to bank = BankSide.BUY

        val bankSide = if (direction == ConversionDirection.TWD_TO_FOREIGN) {
            BankSide.SELL
        } else {
            BankSide.BUY
        }

        val rateVal = rate.getRate(rateType, bankSide) ?: return null
        if (rateVal.compareTo(BigDecimal.ZERO) <= 0) return null

        val resultAmount = if (direction == ConversionDirection.TWD_TO_FOREIGN) {
            amount.divide(rateVal, 4, RoundingMode.HALF_UP)
        } else {
            amount.multiply(rateVal).setScale(2, RoundingMode.HALF_UP)
        }

        return ConversionResult(
            inputAmount = amount,
            outputAmount = resultAmount,
            rateUsed = rateVal,
            currencyCode = rate.currency.name,
            rateType = rateType,
            bankSide = bankSide,
            officialUpdateTime = rate.officialUpdateTime
        )
    }
}
