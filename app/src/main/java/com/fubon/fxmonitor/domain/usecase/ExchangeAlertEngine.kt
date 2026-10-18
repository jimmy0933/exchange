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

enum class TriggerCondition {
    GREATER_EQUAL,
    LESS_EQUAL,
    SINGLE_CHANGE_PCT,
    DAILY_CHANGE_PCT;

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

            var isConditionMet = false
            var desc = ""

            when (condition) {
                TriggerCondition.GREATER_EQUAL -> {
                    isConditionMet = currentRateDouble >= alert.targetValue
                    desc = "匯率 ≥ ${alert.targetValue}"
                }
                TriggerCondition.LESS_EQUAL -> {
                    isConditionMet = currentRateDouble <= alert.targetValue
                    desc = "匯率 ≤ ${alert.targetValue}"
                }
                TriggerCondition.SINGLE_CHANGE_PCT -> {
                    val history = rateDao.getLatestTwoHistory(alert.currencyCode)
                    if (history.size >= 2) {
                        val prevValStr = when (type) {
                            RateType.SPOT -> if (side == BankSide.BUY) history[1].spotBuy else history[1].spotSell
                            RateType.CASH -> if (side == BankSide.BUY) history[1].cashBuy else history[1].cashSell
                        }
                        val prevVal = prevValStr?.toBigDecimalOrNull()
                        if (prevVal != null && prevVal.compareTo(BigDecimal.ZERO) != 0) {
                            val pct = currentRateVal.subtract(prevVal)
                                .divide(prevVal, 6, RoundingMode.HALF_UP)
                                .multiply(BigDecimal("100")).toDouble()
                            // For single change we trigger if current change percentage is >= targetValue or <= -targetValue (depending on how the user thinks).
                            // Wait, the prompt says "單次漲跌幅百分比" -> user expects it to trigger when it drops OR rises.
                            isConditionMet = Math.abs(pct) >= alert.targetValue
                            desc = "單次漲跌幅達 ${alert.targetValue}%"
                        }
                    }
                }
                TriggerCondition.DAILY_CHANGE_PCT -> {
                    val prevDayHistory = rateDao.getPreviousDayHistory(alert.currencyCode, rate.officialUpdateTime)
                    if (prevDayHistory != null) {
                        val prevValStr = when (type) {
                            RateType.SPOT -> if (side == BankSide.BUY) prevDayHistory.spotBuy else prevDayHistory.spotSell
                            RateType.CASH -> if (side == BankSide.BUY) prevDayHistory.cashBuy else prevDayHistory.cashSell
                        }
                        val prevVal = prevValStr?.toBigDecimalOrNull()
                        if (prevVal != null && prevVal.compareTo(BigDecimal.ZERO) != 0) {
                            val pct = currentRateVal.subtract(prevVal)
                                .divide(prevVal, 6, RoundingMode.HALF_UP)
                                .multiply(BigDecimal("100")).toDouble()
                            isConditionMet = Math.abs(pct) >= alert.targetValue
                            desc = "每日漲跌幅達 ${alert.targetValue}%"
                        }
                    }
                }
            }

            // Re-arming logic:
            // If previously triggered and armed, condition must first NOT be met before re-firing.
            if (isConditionMet) {
                if (!alert.isTriggeredAndArmed) {
                    // Fire alert!
                    results.add(
                        AlertEvaluationResult(
                            alert = alert,
                            isTriggered = true,
                            currentRateValue = currentRateDouble,
                            conditionDescription = desc
                        )
                    )
                    // Update state to armed
                    alertDao.updateAlert(
                        alert.copy(
                            lastTriggeredTime = System.currentTimeMillis(),
                            isTriggeredAndArmed = true
                        )
                    )
                    // Log alert
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
                // Rate left the threshold zone; re-arm for next time
                if (alert.isTriggeredAndArmed) {
                    alertDao.updateAlert(alert.copy(isTriggeredAndArmed = false))
                }
            }
        }

        return results
    }
}
