package com.fubon.fxmonitor.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.fubon.fxmonitor.data.local.RateDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun rateDao(): RateDao
}

object WidgetUpdater {
    suspend fun updateAllWidgets(context: Context) {
        SmallRateWidget().updateAll(context)
        MediumRateWidget().updateAll(context)
        LargeRateWidget().updateAll(context)
    }

    suspend fun getPlacedWidgetIds(context: Context): List<String> {
        return getPlacedWidgetTypes(context).keys.toList()
    }

    suspend fun getPlacedWidgetTypes(context: Context): Map<String, String> {
        val manager = GlanceAppWidgetManager(context)
        val typeMap = mutableMapOf<String, String>()
        manager.getGlanceIds(SmallRateWidget::class.java).forEach {
            typeMap[it.toString()] = "Small"
        }
        manager.getGlanceIds(MediumRateWidget::class.java).forEach {
            typeMap[it.toString()] = "Medium"
        }
        manager.getGlanceIds(LargeRateWidget::class.java).forEach {
            typeMap[it.toString()] = "Large"
        }
        return typeMap
    }

    private suspend fun GlanceAppWidget.updateAll(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(this::class.java)
        for (glanceId in glanceIds) {
            update(context, glanceId)
        }
    }
}
