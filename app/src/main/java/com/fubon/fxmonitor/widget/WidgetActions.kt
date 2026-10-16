package com.fubon.fxmonitor.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fubon.fxmonitor.worker.ExchangeMonitorWorker

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val workRequest = OneTimeWorkRequestBuilder<ExchangeMonitorWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "widget_manual_update",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
