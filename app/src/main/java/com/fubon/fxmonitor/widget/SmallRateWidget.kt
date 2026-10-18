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
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.ColorFilter
import androidx.glance.appwidget.action.actionRunCallback
import com.fubon.fxmonitor.R
import androidx.glance.color.ColorProvider as DayNightColorProvider
import com.fubon.fxmonitor.MainActivity
import com.fubon.fxmonitor.data.local.AppDatabase
import com.fubon.fxmonitor.data.local.LatestRateEntity
import com.fubon.fxmonitor.data.local.UserPreferencesRepository
import com.fubon.fxmonitor.data.local.WidgetConfig
import com.fubon.fxmonitor.data.local.WidgetDisplayMode
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

val DividerColorProvider: ColorProvider = DayNightColorProvider(
    day = Color(0xFFE2E8F0),
    night = Color(0xFF334155)
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
    val displayCodes = config.selectedCurrencies.take(2).ifEmpty { listOf(CurrencyCode.USD) }
    val isSingle = displayCodes.size == 1

    val isCash = config.rateType == RateType.CASH
    val bgColorProvider = getBgColorProvider(config.bgTransparency)

    val scale = config.fontSizeScale.coerceIn(80, 120) / 100f
    val titleFontSize = (11 * scale).sp
    val timeFontSize = (8.5 * scale).sp
    val labelFontSize = (9.5 * scale).sp
    val rateFontSize = (11.5 * scale).sp

    val mainIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("target_currency", displayCodes.first().name)
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(bgColorProvider)
            .padding(8.dp)
            .clickable(actionStartActivity(mainIntent)),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        if (isSingle) {
            val code = displayCodes.first()
            val rateEntity = allRates[code.name]
            val buyVal = if (isCash) rateEntity?.cashBuy else rateEntity?.spotBuy
            val sellVal = if (isCash) rateEntity?.cashSell else rateEntity?.spotSell

            val buyStr = formatWidgetRate(buyVal)
            val sellStr = formatWidgetRate(sellVal)

            val officialTime = rateEntity?.officialUpdateTime ?: "未更新"
            val formattedTime = extractTime(officialTime)

            val titleText = if (config.displayMode == WidgetDisplayMode.FLAG_ONLY) code.flagEmoji else "${code.flagEmoji} ${code.name}"

            // Line 1: Code + Flag on left, Time on right
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleText,
                    style = TextStyle(
                        color = CurrencyTextColorProvider,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Bold
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
                        fontSize = labelFontSize,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = buyStr,
                    style = TextStyle(
                        color = BuyRateColorProvider,
                        fontSize = rateFontSize,
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
                        fontSize = labelFontSize,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = sellStr,
                    style = TextStyle(
                        color = SellRateColorProvider,
                        fontSize = rateFontSize,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        } else {
            // Dual mode
            val timeRate = allRates[displayCodes.first().name]?.officialUpdateTime ?: "未更新"
            val formattedTime = extractTime(timeRate)
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

            Spacer(modifier = GlanceModifier.height(2.dp))

            displayCodes.forEachIndexed { index, code ->
                val rateEntity = allRates[code.name]
                val buyVal = if (isCash) rateEntity?.cashBuy else rateEntity?.spotBuy
                val sellVal = if (isCash) rateEntity?.cashSell else rateEntity?.spotSell

                val buyStr = formatWidgetRate(buyVal, isCompact = true)
                val sellStr = formatWidgetRate(sellVal, isCompact = true)

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = code.flagEmoji,
                        style = TextStyle(
                            color = CurrencyTextColorProvider,
                            fontSize = (11 * scale).sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "$buyStr",
                        style = TextStyle(
                            color = BuyRateColorProvider,
                            fontSize = (10 * scale).sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "/",
                        style = TextStyle(
                            color = TimeTextColorProvider,
                            fontSize = (10 * scale).sp
                        ),
                        modifier = GlanceModifier.padding(horizontal = 2.dp)
                    )
                    Text(
                        text = "$sellStr",
                        style = TextStyle(
                            color = SellRateColorProvider,
                            fontSize = (10 * scale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                if (index == 0) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
                }
            }
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
