package com.fubon.fxmonitor.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "latest_rates")
data class LatestRateEntity(
    @PrimaryKey val currencyCode: String,
    val spotBuy: String?,
    val spotSell: String?,
    val cashBuy: String?,
    val cashSell: String?,
    val officialUpdateTime: String,
    val localFetchTime: Long,
    val source: String,
    val isFavorite: Boolean = false,
    val displayOrder: Int = 0
)

@Entity(
    tableName = "rate_history",
    indices = [
        Index(value = ["currencyCode", "timestamp"]),
        Index(value = ["currencyCode", "officialUpdateTime"], unique = true)
    ]
)
data class RateHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val currencyCode: String,
    val spotBuy: String?,
    val spotSell: String?,
    val cashBuy: String?,
    val cashSell: String?,
    val officialUpdateTime: String,
    val timestamp: Long,
    val isOfficialDaily: Boolean = false,
    val source: String = "台北富邦銀行"
)

@Entity(tableName = "exchange_alerts")
data class ExchangeAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val currencyCode: String,
    val rateType: String, // SPOT, CASH
    val bankSide: String, // BUY, SELL
    val triggerCondition: String, // GREATER_EQUAL, LESS_EQUAL, SINGLE_CHANGE_PCT, DAILY_CHANGE_PCT
    val targetValue: Double,
    val isEnabled: Boolean = true,
    val createdTime: Long = System.currentTimeMillis(),
    val lastTriggeredTime: Long? = null,
    val isTriggeredAndArmed: Boolean = false // Used for re-arming logic
)

@Entity(tableName = "alert_history")
data class AlertHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alertId: Long,
    val currencyCode: String,
    val rateType: String,
    val bankSide: String,
    val triggeredRate: Double,
    val targetValue: Double,
    val conditionDescription: String,
    val triggeredTime: Long
)

@Entity(tableName = "error_logs")
data class ErrorLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
