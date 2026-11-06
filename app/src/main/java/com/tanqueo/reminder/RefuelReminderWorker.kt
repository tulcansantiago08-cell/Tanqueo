package com.tanqueo.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.tanqueo.domain.repository.SurfaceRefresher
import com.tanqueo.domain.usecase.CheckRefuelReminder
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Revisa la estimación dos veces al día, avisa si toca (o quita un aviso viejo) y deja la tile al día. */
@HiltWorker
class RefuelReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val check: CheckRefuelReminder,
    private val notifier: RefuelNotifier,
    private val surfaces: SurfaceRefresher,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val estimate = check()
        if (inputData.getBoolean(KEY_ONLY_CLEAR, false)) {
            if (estimate == null) notifier.cancel()
            return Result.success()
        }
        if (estimate != null) notifier.notify(estimate) else notifier.cancel()
        surfaces.refresh()
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "aviso_tanqueo_diario"
        private const val RECHECK_NAME = "aviso_tanqueo_revisar"
        private const val KEY_ONLY_CLEAR = "solo_quitar"

        /** Tras un tanqueo o un km nuevo: quita el aviso si ya no aplica, sin lanzar uno nuevo. */
        fun clearIfNoLongerNeeded(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                RECHECK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<RefuelReminderWorker>()
                    .setInputData(workDataOf(KEY_ONLY_CLEAR to true))
                    .build(),
            )
        }

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                // Con una sola revisión al día, el aviso de "menos de 1 día" podría llegar casi sin gasolina.
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<RefuelReminderWorker>(12, TimeUnit.HOURS).build(),
            )
        }
    }
}
