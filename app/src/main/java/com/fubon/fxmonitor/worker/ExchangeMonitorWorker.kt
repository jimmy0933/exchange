package com.fubon.fxmonitor.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.fubon.fxmonitor.data.local.UserPreferencesRepository
import com.fubon.fxmonitor.data.repository.ExchangeRateRepository
import com.fubon.fxmonitor.domain.usecase.ExchangeAlertEngine
import com.fubon.fxmonitor.notification.FubonNotificationManager
import com.fubon.fxmonitor.widget.WidgetUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class ExchangeMonitorWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ExchangeRateRepository,
    private val alertEngine: ExchangeAlertEngine,
    private val notificationManager: FubonNotificationManager,
    private val userPrefs: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "fubon_fx_monitor_periodic_work"

        fun schedule(context: Context, intervalMinutes: Long = 15) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ExchangeMonitorWorker>(
                intervalMinutes, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10,
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        val refreshResult = repository.refreshRates()
        return refreshResult.fold(
            onSuccess = {
                // Update Glance widgets (alert logic is now inside repository)
                try {
                    WidgetUpdater.updateAllWidgets(context)
                } catch (e: Exception) {
                    // Ignore widget update errors during background task
                }

                Result.success()
            },
            onFailure = {
                Result.retry()
            }
        )
    }
}
