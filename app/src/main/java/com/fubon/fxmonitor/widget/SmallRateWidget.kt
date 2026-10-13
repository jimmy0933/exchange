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
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.RateType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SmallRateWidget : GlanceAppWidget() {
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
            SmallWidgetContent(config, allRates)
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        super.onDelete(context, glanceId)
        val userPrefs = UserPreferencesRepository(context)
        userPrefs.removeWidgetConfig(glanceId.toString())
    }
}

@Composable
fun SmallWidgetContent(
    config: WidgetConfig,
    allRates: Map<String, LatestRateEntity>
) {
    val context = LocalContext.current
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val code = config.selectedCurrencies.firstOrNull() ?: CurrencyCode.USD
    val rateEntity = allRates[code.name]

    val isCash = config.rateType == RateType.CASH
    val buyVal = if (isCash) rateEntity?.cashBuy else rateEntity?.spotBuy
    val sellVal = if (isCash) rateEntity?.cashSell else rateEntity?.spotSell

    val buyStr = buyVal ?: "未提供"
    val sellStr = sellVal ?: "未提供"

    val officialTime = rateEntity?.officialUpdateTime ?: "未更新"

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(Color(0xFF17212F))
            .padding(10.dp)
            .clickable(actionStartActivity(intent)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val formattedTime = extractTime(officialTime)
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "台北富邦｜${if (isCash) "現鈔" else "即期"}",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFFCBD5E1)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = if (formattedTime == "未更新") "未更新" else "更新 $formattedTime",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFF94A3B8)),
                    fontSize = 9.sp
                )
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "${code.flagEmoji} ${code.name}",
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(Color(0xFFF8FAFC)),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(6.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "買入",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFFCBD5E1)),
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = buyStr,
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFFF8FAFC)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = GlanceModifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "賣出",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFFCBD5E1)),
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = sellStr,
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFF38BDF8)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

fun extractTime(timeStr: String): String {
    val match = Regex("""\d{2}:\d{2}""").find(timeStr)
    return match?.value ?: timeStr
}

class SmallRateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallRateWidget()
}
