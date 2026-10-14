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
import androidx.glance.unit.ColorProvider
import androidx.glance.color.ColorProvider as DayNightColorProvider
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

fun getBgColorProvider(bgTransparency: Int): ColorProvider {
    val alpha = (100 - bgTransparency.coerceIn(0, 100)) / 100f
    return DayNightColorProvider(
        day = Color(0xFFFFFFFF).copy(alpha = alpha),
        night = Color(0xFF17212F).copy(alpha = alpha)
    )
}

val CurrencyTextColorProvider: ColorProvider = DayNightColorProvider(
    day = Color(0xFF17212F),
    night = Color(0xFFF8FAFC)
)

val BuyRateColorProvider: ColorProvider = DayNightColorProvider(
    day = Color(0xFF334155),
    night = Color(0xFFE2E8F0)
)

val SellRateColorProvider: ColorProvider = DayNightColorProvider(
    day = Color(0xFF0284C7),
    night = Color(0xFF38BDF8)
)

val TimeTextColorProvider: ColorProvider = DayNightColorProvider(
    day = Color(0xFF64748B),
    night = Color(0xFF94A3B8)
)

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
    val code = config.selectedCurrencies.firstOrNull() ?: CurrencyCode.USD

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("target_currency", code.name)
    }

    val isCash = config.rateType == RateType.CASH
    val bgColorProvider = getBgColorProvider(config.bgTransparency)

    val rateEntity = allRates[code.name]
    val buyVal = if (isCash) rateEntity?.cashBuy else rateEntity?.spotBuy
    val sellVal = if (isCash) rateEntity?.cashSell else rateEntity?.spotSell

    val buyStr = formatWidgetRate(buyVal)
    val sellStr = formatWidgetRate(sellVal)

    val officialTime = rateEntity?.officialUpdateTime ?: "未更新"
    val formattedTime = extractTime(officialTime)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(bgColorProvider)
            .padding(8.dp)
            .clickable(actionStartActivity(intent)),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Line 1: Code + Flag on left, Time on right
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${code.flagEmoji} ${code.name}",
                style = TextStyle(
                    color = CurrencyTextColorProvider,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = formattedTime,
                style = TextStyle(
                    color = TimeTextColorProvider,
                    fontSize = 10.sp
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Line 2: Buy Rate
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "買 ",
                style = TextStyle(
                    color = TimeTextColorProvider,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = buyStr,
                style = TextStyle(
                    color = BuyRateColorProvider,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(2.dp))

        // Line 3: Sell Rate
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "賣 ",
                style = TextStyle(
                    color = TimeTextColorProvider,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = sellStr,
                style = TextStyle(
                    color = SellRateColorProvider,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

/**
 * Helper to format rates cleanly in small widget space.
 * Prevents text overlapping and ensures clear spacing between values.
 */
fun formatWidgetRate(value: String?, isCompact: Boolean = false): String {
    if (value.isNullOrBlank()) return "未提供"
    val trimmed = value.trim()
    val num = trimmed.toDoubleOrNull() ?: return trimmed

    return if (isCompact && num >= 100) {
        String.format(java.util.Locale.US, "%.2f", num)
    } else {
        trimmed
    }
}

fun extractTime(timeStr: String): String {
    val match = Regex("""\d{2}:\d{2}""").find(timeStr)
    return match?.value ?: timeStr
}

class SmallRateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallRateWidget()
}
