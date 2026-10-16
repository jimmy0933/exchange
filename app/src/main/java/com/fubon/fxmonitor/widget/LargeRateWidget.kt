package com.fubon.fxmonitor.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.ColorFilter
import androidx.glance.appwidget.action.actionRunCallback
import com.fubon.fxmonitor.R
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.fubon.fxmonitor.MainActivity
import com.fubon.fxmonitor.data.local.AppDatabase
import com.fubon.fxmonitor.data.local.LatestRateEntity
import com.fubon.fxmonitor.data.local.UserPreferencesRepository
import com.fubon.fxmonitor.data.local.WidgetConfig
import com.fubon.fxmonitor.data.local.WidgetDisplayMode
import com.fubon.fxmonitor.domain.model.RateType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class LargeRateWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val userPrefs = UserPreferencesRepository(context)
        val rateDao = AppDatabase.getInstance(context).rateDao()

        val config = withContext(Dispatchers.IO) {
            userPrefs.getWidgetConfig(id.toString()).first()
        }
        val allRates = withContext(Dispatchers.IO) {
            rateDao.getAllLatestRates().first().associateBy { it.currencyCode }
        }

        provideContent {
            LargeWidgetContent(config, allRates)
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        super.onDelete(context, glanceId)
        val userPrefs = UserPreferencesRepository(context)
        userPrefs.removeWidgetConfig(glanceId.toString())
    }
}

@Composable
fun LargeWidgetContent(
    config: WidgetConfig,
    allRates: Map<String, LatestRateEntity>
) {
    val context = LocalContext.current
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val isCash = config.rateType == RateType.CASH
    val displayCurrencies = config.selectedCurrencies.take(8)
    val bgColorProvider = getBgColorProvider(config.bgTransparency)

    val firstCode = displayCurrencies.firstOrNull()
    val firstEntity = if (firstCode != null) allRates[firstCode.name] else null
    val firstOfficialTime = firstEntity?.officialUpdateTime
    val formattedTime = if (firstOfficialTime != null) extractTime(firstOfficialTime) else "未更新"

    val scale = config.fontSizeScale.coerceIn(80, 120) / 100f
    val currencyFontSize = (12 * scale).sp
    val rateFontSize = (12 * scale).sp
    val timeFontSize = (9.5 * scale).sp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(bgColorProvider)
            .padding(12.dp)
            .clickable(actionStartActivity(intent)),
        verticalAlignment = Alignment.Top
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isCash) "現鈔匯率" else "即期匯率",
                style = TextStyle(
                    color = TimeTextColorProvider,
                    fontSize = timeFontSize,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = formattedTime,
                style = TextStyle(
                    color = TimeTextColorProvider,
                    fontSize = timeFontSize
                ),
                modifier = GlanceModifier.padding(end = 4.dp)
            )
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = "Refresh",
                colorFilter = ColorFilter.tint(TimeTextColorProvider),
                modifier = GlanceModifier.size(12.dp).clickable(actionRunCallback<RefreshAction>())
            )
        }

        displayCurrencies.forEachIndexed { index, code ->
            val entity = allRates[code.name]
            val buyVal = if (isCash) entity?.cashBuy else entity?.spotBuy
            val sellVal = if (isCash) entity?.cashSell else entity?.spotSell

            val buyStr = formatWidgetRate(buyVal)
            val sellStr = formatWidgetRate(sellVal)

            val currencyText = if (config.displayMode == WidgetDisplayMode.FLAG_ONLY) {
                code.flagEmoji
            } else {
                "${code.flagEmoji} ${code.name}"
            }

            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currencyText,
                    style = TextStyle(
                        color = CurrencyTextColorProvider,
                        fontSize = currencyFontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = buyStr,
                    style = TextStyle(
                        color = BuyRateColorProvider,
                        fontSize = rateFontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = sellStr,
                    style = TextStyle(
                        color = SellRateColorProvider,
                        fontSize = rateFontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
            }
            if (index < displayCurrencies.size - 1) {
                Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(DividerColorProvider)) {}
            }
        }
    }
}

class LargeRateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LargeRateWidget()

    override fun onUpdate(
        context: android.content.Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.fubon.fxmonitor.worker.ExchangeMonitorWorker>().build()
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "widget_auto_update",
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
