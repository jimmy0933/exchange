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
    val currencies = config.selectedCurrencies.take(2).ifEmpty { listOf(CurrencyCode.USD) }
    val isSingle = currencies.size == 1

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (isSingle) {
            putExtra("target_currency", currencies.first().name)
        }
    }

    val isCash = config.rateType == RateType.CASH

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(12.dp)
            .background(Color(0xFF17212F))
            .padding(6.dp)
            .clickable(actionStartActivity(intent)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start
    ) {
        if (isSingle) {
            val code = currencies.first()
            val rateEntity = allRates[code.name]
            val buyVal = if (isCash) rateEntity?.cashBuy else rateEntity?.spotBuy
            val sellVal = if (isCash) rateEntity?.cashSell else rateEntity?.spotSell

            val buyStr = buyVal ?: "未提供"
            val sellStr = sellVal ?: "未提供"

            val officialTime = rateEntity?.officialUpdateTime ?: "未更新"
            val formattedTime = extractTime(officialTime)

            // Row 1: Code + Time
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = code.name,
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFFF8FAFC)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = formattedTime,
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFF94A3B8)),
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(3.dp))

            // Row 2: Buy Rate
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "買 ",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFFCBD5E1)),
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = buyStr,
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFFF8FAFC)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(1.dp))

            // Row 3: Sell Rate
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "賣 ",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFFCBD5E1)),
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = sellStr,
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFF38BDF8)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        } else {
            // Dual Currency Mode
            var latestTime = "未更新"

            currencies.forEachIndexed { index, code ->
                val rateEntity = allRates[code.name]
                val buyVal = if (isCash) rateEntity?.cashBuy else rateEntity?.spotBuy
                val sellVal = if (isCash) rateEntity?.cashSell else rateEntity?.spotSell

                val buyStr = buyVal ?: "未提供"
                val sellStr = sellVal ?: "未提供"

                if (rateEntity?.officialUpdateTime != null) {
                    latestTime = extractTime(rateEntity.officialUpdateTime)
                }

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = code.name,
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(Color(0xFFF8FAFC)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = buyStr,
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(Color(0xFFF8FAFC)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = " / ",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(Color(0xFF64748B)),
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = sellStr,
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(Color(0xFF38BDF8)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                if (index < currencies.size - 1) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Row 3: Timestamp
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = latestTime,
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFF94A3B8)),
                        fontSize = 9.sp
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
