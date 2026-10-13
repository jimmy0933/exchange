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

class SmallRateWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val rateDao = AppDatabase.getInstance(context).rateDao()
        val rates = withContext(Dispatchers.IO) {
            rateDao.getAllLatestRates().first()
        }
        val topRate = rates.firstOrNull { it.isFavorite } ?: rates.firstOrNull()

        provideContent {
            SmallWidgetContent(topRate)
        }
    }
}

@Composable
fun SmallWidgetContent(rate: LatestRateEntity?) {
    val context = LocalContext.current
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (rate != null) putExtra("target_currency", rate.currencyCode)
    }

    val currency = rate?.let { CurrencyCode.fromString(it.currencyCode) } ?: CurrencyCode.USD
    val isStale = rate != null && (System.currentTimeMillis() - rate.localFetchTime > 30 * 60 * 1000)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp)
            .clickable(actionStartActivity(intent)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${currency.displayName} (${currency.name})",
            style = TextStyle(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "即賣: ${rate?.spotSell ?: "N/A"}",
            style = TextStyle(fontWeight = FontWeight.Medium)
        )
        Text(
            text = "現賣: ${rate?.cashSell ?: "N/A"}",
            style = TextStyle(fontWeight = FontWeight.Normal)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = rate?.officialUpdateTime ?: "未更新",
            style = TextStyle(fontWeight = FontWeight.Normal)
        )
        if (isStale) {
            Text(
                text = "⚠️ 資料過期",
                style = TextStyle(fontWeight = FontWeight.Bold)
            )
        }
    }
}

class SmallRateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallRateWidget()
}
