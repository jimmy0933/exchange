package com.fubon.fxmonitor.domain.usecase

import com.fubon.fxmonitor.data.local.AlertDao
import com.fubon.fxmonitor.data.local.AlertHistoryEntity
import com.fubon.fxmonitor.data.local.ExchangeAlertEntity
import com.fubon.fxmonitor.data.local.RateDao
import com.fubon.fxmonitor.domain.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

enum class TriggerCondition(val label: String, val unit: String) {
    GREATER_EQUAL("≥ 大於等於", "TWD"),
    LESS_EQUAL("≤ 小於等於", "TWD"),
    SINGLE_CHANGE_PCT("單次漲跌 %", "%"),
    DAILY_CHANGE_PCT("每日漲跌 %", "%");

    companion object {
        fun fromString(str: String): TriggerCondition = entries.find { it.name == str } ?: GREATER_EQUAL
    }
}

data class AlertEvaluationResult(
    val alert: ExchangeAlertEntity,
    val isTriggered: Boolean,
    val currentRateValue: Double,
    val conditionDescription: String
)

@Singleton
class ExchangeAlertEngine @Inject constructor(
    private val alertDao: AlertDao,
    private val rateDao: RateDao
) {

    suspend fun evaluateAllAlerts(latestRates: List<ExchangeRate>): List<AlertEvaluationResult> {
        val activeAlerts = alertDao.getActiveAlerts()
        val results = mutableListOf<AlertEvaluationResult>()
        val rateMap = latestRates.associateBy { it.currency.name }

        for (alert in activeAlerts) {
            val rate = rateMap[alert.currencyCode] ?: continue
            val type = RateType.valueOf(alert.rateType)
            val side = BankSide.valueOf(alert.bankSide)
            val currentRateVal = rate.getRate(type, side) ?: continue

            val currentRateDouble = currentRateVal.toDouble()
            val condition = TriggerCondition.fromString(alert.triggerCondition)
            val targetValBD = alert.targetValue.toBigDecimal()

            var isConditionMet = false
            var desc = ""

            when (condition) {
                TriggerCondition.GREATER_EQUAL -> {
                    isConditionMet = currentRateVal >= targetValBD
                    desc = "匯率 ${currentRateVal.toPlainString()} ≥ ${alert.targetValue} TWD"
                }
                TriggerCondition.LESS_EQUAL -> {
                    isConditionMet = currentRateVal <= targetValBD
                    desc = "匯率 ${currentRateVal.toPlainString()} ≤ ${alert.targetValue} TWD"
                }
                TriggerCondition.SINGLE_CHANGE_PCT -> {
                    val history = rateDao.getLatestTwoHistory(alert.currencyCode)
                    if (history.size >= 2) {
                        val prevValStr = when (type) {
                            RateType.SPOT -> if (side == BankSide.BUY) history[1].spotBuy else history[1].spotSell
                            RateType.CASH -> if (side == BankSide.BUY) history[1].cashBuy else history[1].cashSell
                        }
                        val prevVal = prevValStr?.toBigDecimalOrNull()
                        if (prevVal != null && prevVal.compareTo(BigDecimal.ZERO) > 0) {
                            val diff = currentRateVal.subtract(prevVal)
                            val pctBD = diff.divide(prevVal, 6, RoundingMode.HALF_UP).multiply(BigDecimal("100")).abs()
                            isConditionMet = pctBD >= targetValBD
                            desc = "單次變動 ${pctBD.setScale(2, RoundingMode.HALF_UP).toPlainString()}% ≥ ${alert.targetValue}%"
                        }
                    }
                }
                TriggerCondition.DAILY_CHANGE_PCT -> {
                    val allHistory = rateDao.getAllHistoryListForCurrency(alert.currencyCode)
                    val currentDateStr = extractDateString(rate.officialUpdateTime)

                    val prevDayEntry = allHistory.find { hist ->
                        val histDateStr = extractDateString(hist.officialUpdateTime)
                        histDateStr.isNotBlank() && histDateStr != currentDateStr
                    }

                    if (prevDayEntry != null) {
                        val prevValStr = when (type) {
                            RateType.SPOT -> if (side == BankSide.BUY) prevDayEntry.spotBuy else prevDayEntry.spotSell
                            RateType.CASH -> if (side == BankSide.BUY) prevDayEntry.cashBuy else prevDayEntry.cashSell
                        }
                        val prevVal = prevValStr?.toBigDecimalOrNull()
                        if (prevVal != null && prevVal.compareTo(BigDecimal.ZERO) > 0) {
                            val diff = currentRateVal.subtract(prevVal)
                            val pctBD = diff.divide(prevVal, 6, RoundingMode.HALF_UP).multiply(BigDecimal("100")).abs()
                            isConditionMet = pctBD >= targetValBD
                            desc = "每日變動 ${pctBD.setScale(2, RoundingMode.HALF_UP).toPlainString()}% ≥ ${alert.targetValue}%"
                        }
                    }
                }
            }

            if (isConditionMet) {
                if (!alert.isTriggeredAndArmed) {
                    results.add(
                        AlertEvaluationResult(
                            alert = alert,
                            isTriggered = true,
                            currentRateValue = currentRateDouble,
                            conditionDescription = desc
                        )
                    )
                    alertDao.updateAlert(
                        alert.copy(
                            lastTriggeredTime = System.currentTimeMillis(),
                            isTriggeredAndArmed = true
                        )
                    )
                    alertDao.insertAlertHistory(
                        AlertHistoryEntity(
                            alertId = alert.id,
                            currencyCode = alert.currencyCode,
                            rateType = alert.rateType,
                            bankSide = alert.bankSide,
                            triggeredRate = currentRateDouble,
                            targetValue = alert.targetValue,
                            conditionDescription = desc,
                            triggeredTime = System.currentTimeMillis()
                        )
                    )
                }
            } else {
                if (alert.isTriggeredAndArmed) {
                    alertDao.updateAlert(alert.copy(isTriggeredAndArmed = false))
                }
            }
        }

        return results
    }

    private fun extractDateString(officialTimeStr: String): String {
        val regex = Regex("""\d{4}/\d{2}/\d{2}""")
        return regex.find(officialTimeStr)?.value ?: ""
    }
}
