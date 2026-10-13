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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MediumRateWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val rateDao = AppDatabase.getInstance(context).rateDao()
        val rates = withContext(Dispatchers.IO) {
            rateDao.getAllLatestRates().first().take(3)
        }

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

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp)
            .clickable(actionStartActivity(intent))
    ) {
        Text(
            text = "台北富邦銀行 匯率監控",
            style = TextStyle(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        for (rate in rates) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "${rate.currencyCode}: ",
                    style = TextStyle(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "即買 ${rate.spotBuy ?: "N/A"} / 即賣 ${rate.spotSell ?: "N/A"}",
                    style = TextStyle(fontWeight = FontWeight.Normal)
                )
            }
        }
    }
}

class MediumRateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumRateWidget()
}
