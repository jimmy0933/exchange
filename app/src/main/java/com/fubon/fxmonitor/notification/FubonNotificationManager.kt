package com.fubon.fxmonitor.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fubon.fxmonitor.MainActivity
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.RateType
import com.fubon.fxmonitor.domain.usecase.AlertEvaluationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FubonNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "fubon_fx_alerts"
        const val CHANNEL_NAME = "匯率到價通知"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "當台北富邦銀行匯率達到您設定的條件時發送提醒"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendAlertNotification(result: AlertEvaluationResult, soundEnabled: Boolean, vibrationEnabled: Boolean) {
        val currencyCode = CurrencyCode.fromString(result.alert.currencyCode) ?: CurrencyCode.USD
        val rateTypeLabel = if (RateType.valueOf(result.alert.rateType) == RateType.SPOT) "即期" else "現鈔"
        val bankSideLabel = if (result.alert.bankSide == "BUY") "銀行買入" else "銀行賣出"

        val title = "【我的匯率到價通知】"
        val text = "${currencyCode.displayName} ${currencyCode.name}\n" +
                "$rateTypeLabel$bankSideLabel：${result.currentRateValue}\n" +
                "條件：${result.conditionDescription}\n" +
                "已達到設定條件。"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_currency", currencyCode.name)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            result.alert.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("${currencyCode.displayName} $rateTypeLabel$bankSideLabel：${result.currentRateValue}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (!vibrationEnabled) {
            builder.setVibrate(longArrayOf(0L))
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(result.alert.id.toInt(), builder.build())
    }
}
