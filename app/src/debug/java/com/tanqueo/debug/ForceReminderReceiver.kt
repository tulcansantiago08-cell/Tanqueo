package com.tanqueo.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tanqueo.domain.repository.SettingsRepository
import com.tanqueo.reminder.RefuelReminderWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Corre ya el mismo worker del aviso diario. Con el extra `umbral_km` sube antes el umbral para
 * que la estimación quede por debajo:
 *
 * adb shell am broadcast -n com.tanqueo/.debug.ForceReminderReceiver --ei umbral_km 2000
 */
@AndroidEntryPoint
class ForceReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var settings: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val threshold = intent.getIntExtra(EXTRA_KM_THRESHOLD, -1)
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (threshold > 0) settings.setKmThreshold(threshold)
                WorkManager.getInstance(context)
                    .enqueue(OneTimeWorkRequestBuilder<RefuelReminderWorker>().build())
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val EXTRA_KM_THRESHOLD = "umbral_km"
    }
}
