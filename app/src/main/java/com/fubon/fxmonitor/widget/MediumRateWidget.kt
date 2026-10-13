package com.fubon.fxmonitor.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.fubon.fxmonitor.MainActivity
import com.fubon.fxmonitor.data.local.AppDatabase
import com.fubon.fxmonitor.data.local.LatestRateEntity
import com.fubon.fxmonitor.domain.model.CurrencyCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MediumRateWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val rateDao = AppDatabase.getInstance(context).rateDao()
        val allRates = withContext(Dispatchers.IO) {
            rateDao.getAllLatestRates().first()
        }
        val favorites = allRates.filter { it.isFavorite }
        val others = allRates.filter { !it.isFavorite }
        val rates = (favorites + others).take(3)

        provideContent {
            MediumWidgetContent(rates)
        }
    }
}

@Composable
fun MediumWidgetContent(rates: List<LatestRateEntity>) {
    val context = LocalContext.current
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val isStale = rates.firstOrNull()?.let {
        System.currentTimeMillis() - it.localFetchTime > 30 * 60 * 1000
    } ?: false

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp)
            .clickable(actionStartActivity(intent))
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "台北富邦 匯率精選 ${if (isStale) "⚠️(過期)" else ""}",
                style = TextStyle(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        for (rate in rates) {
            val code = CurrencyCode.fromString(rate.currencyCode) ?: CurrencyCode.USD
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "${code.name}: ",
                    style = TextStyle(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "即賣 ${rate.spotSell ?: "N/A"} / 現賣 ${rate.cashSell ?: "N/A"}",
                    style = TextStyle(fontWeight = FontWeight.Normal)
                )
            }
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        val lastTime = rates.firstOrNull()?.officialUpdateTime ?: "尚未更新"
        Text(
            text = "牌告時間: $lastTime",
            style = TextStyle(fontWeight = FontWeight.Normal)
        )
    }
}

class MediumRateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumRateWidget()
}
