package com.fubon.fxmonitor.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(bgColorProvider)
            .padding(12.dp)
            .clickable(actionStartActivity(intent)),
        verticalAlignment = Alignment.Top
    ) {
        displayCurrencies.forEachIndexed { index, code ->
            val entity = allRates[code.name]
            val buyVal = if (isCash) entity?.cashBuy else entity?.spotBuy
            val sellVal = if (isCash) entity?.cashSell else entity?.spotSell

            val buyStr = formatWidgetRate(buyVal)
            val sellStr = formatWidgetRate(sellVal)

            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Flag & Currency Code
                Text(
                    text = "${code.flagEmoji} ${code.name}",
                    style = TextStyle(
                        color = CurrencyTextColorProvider,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                // Column 2: Buy Rate
                Text(
                    text = buyStr,
                    style = TextStyle(
                        color = BuyRateColorProvider,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                // Column 3: Sell Rate
                Text(
                    text = sellStr,
                    style = TextStyle(
                        color = SellRateColorProvider,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                // Column 4: Update Time (only on first row)
                Text(
                    text = if (index == 0) formattedTime else "",
                    style = TextStyle(
                        color = TimeTextColorProvider,
                        fontSize = 10.sp
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

class LargeRateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LargeRateWidget()
}
