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
        val manager = GlanceAppWidgetManager(context)
        val ids = mutableListOf<String>()
        val smalls = manager.getGlanceIds(SmallRateWidget::class.java)
        val mediums = manager.getGlanceIds(MediumRateWidget::class.java)
        val larges = manager.getGlanceIds(LargeRateWidget::class.java)

        (smalls + mediums + larges).forEach { glanceId ->
            ids.add(glanceId.toString())
        }
        return ids
    }

    private suspend fun GlanceAppWidget.updateAll(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(this::class.java)
        for (glanceId in glanceIds) {
            update(context, glanceId)
        }
    }
}
