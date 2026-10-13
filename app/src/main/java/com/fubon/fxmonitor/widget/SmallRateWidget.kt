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
import kotlinx.coroutines.withContext

class SmallRateWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val rateDao = AppDatabase.getInstance(context).rateDao()
        val usdRate = withContext(Dispatchers.IO) {
            rateDao.getLatestRate("USD")
        }

        provideContent {
            SmallWidgetContent(usdRate)
        }
    }
}

@Composable
fun SmallWidgetContent(rate: LatestRateEntity?) {
    val context = LocalContext.current
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp)
            .clickable(actionStartActivity(intent)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "美金 (USD)",
            style = TextStyle(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "即期賣出: ${rate?.spotSell ?: "N/A"}",
            style = TextStyle(fontWeight = FontWeight.Medium)
        )
        Text(
            text = "現鈔賣出: ${rate?.cashSell ?: "N/A"}",
            style = TextStyle(fontWeight = FontWeight.Normal)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = rate?.officialUpdateTime ?: "未更新",
            style = TextStyle(fontWeight = FontWeight.Normal)
        )
    }
}

class SmallRateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallRateWidget()
}
