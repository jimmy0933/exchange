package com.fubon.fxmonitor.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fubon.fxmonitor.data.local.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var userPrefs: UserPreferencesRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val mins = userPrefs.backgroundIntervalMins.first()
                    ExchangeMonitorWorker.schedule(context, mins.toLong())
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
