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
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val rateDao = entryPoint.rateDao()
        val latestRates = rateDao.getAllLatestRates()

        SmallRateWidget().updateAll(context)
        MediumRateWidget().updateAll(context)
        LargeRateWidget().updateAll(context)
    }

    private suspend fun GlanceAppWidget.updateAll(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(this::class.java)
        for (glanceId in glanceIds) {
            update(context, glanceId)
        }
    }
}
