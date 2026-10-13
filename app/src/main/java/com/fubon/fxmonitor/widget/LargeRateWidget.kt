package com.fubon.fxmonitor.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

    var latestOfficialTime = "未更新"

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(Color(0xFF17212F))
            .padding(12.dp)
            .clickable(actionStartActivity(intent))
    ) {
        // Find latest official update time across displayed currencies
        for (code in displayCurrencies) {
            val entity = allRates[code.name]
            if (entity?.officialUpdateTime != null) {
                latestOfficialTime = entity.officialUpdateTime
            }
        }

        // Header with title and update time on the same line
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "台北富邦｜${if (isCash) "現鈔匯率" else "即期匯率"}",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFFF8FAFC)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            val formattedTime = extractTime(latestOfficialTime)
            Text(
                text = if (formattedTime == "未更新") "未更新" else "更新 $formattedTime",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFF94A3B8)),
                    fontSize = 10.sp
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        // Column Titles
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "幣別",
                style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFFCBD5E1)), fontSize = 12.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = "銀行買入",
                style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFFCBD5E1)), fontSize = 12.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = "銀行賣出",
                style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFFCBD5E1)), fontSize = 12.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.defaultWeight()
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Currency Rows
        for (code in displayCurrencies) {
            val entity = allRates[code.name]
            val buyVal = if (isCash) entity?.cashBuy else entity?.spotBuy
            val sellVal = if (isCash) entity?.cashSell else entity?.spotSell

            val buyStr = buyVal ?: "未提供"
            val sellStr = sellVal ?: "未提供"

            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${code.flagEmoji} ${code.name}",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFFF8FAFC)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = buyStr,
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFFF8FAFC)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = sellStr,
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFF38BDF8)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
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
