package com.tanqueo.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.calc.RangeEstimator
import com.tanqueo.domain.model.OdometerUpdate
import com.tanqueo.domain.repository.FillUpRepository
import com.tanqueo.domain.repository.SettingsRepository
import com.tanqueo.domain.repository.SurfaceRefresher
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Lleva la app a un estado para mostrarlo en una demo:
 *
 * adb shell am broadcast -n com.tanqueo/.debug.DemoReceiver --ei km_restantes 40   # alerta ámbar
 * adb shell am broadcast -n com.tanqueo/.debug.DemoReceiver --ez vaciar true       # estado vacío
 * adb shell pm clear com.tanqueo                                                   # datos de ejemplo
 *
 * Para los km restantes registra una lectura de odómetro, igual que "Actualizar km".
 */
@AndroidEntryPoint
class DemoReceiver : BroadcastReceiver() {
    @Inject lateinit var fillUps: FillUpRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var surfaces: SurfaceRefresher
    @Inject lateinit var clock: Clock

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val targetKm = intent.getIntExtra(EXTRA_KM_LEFT, -1)
        val clear = intent.getBooleanExtra(EXTRA_CLEAR, false)
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (clear) fillUps.observeAll().first().forEach { fillUps.delete(it.id) }
                if (targetKm >= 0) setKmLeft(targetKm)
                surfaces.refresh()
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun setKmLeft(targetKm: Int) {
        val list = fillUps.observeAll().first()
        val baseKm = list.firstOrNull()?.odometerKm ?: return
        val capacity = settings.settings.first().tankCapacityGallons
        val now = clock.instant()
        val estimate = RangeEstimator.estimate(list, capacity, null, now) as? RangeEstimate.Available ?: return
        val fullRange = estimate.gallonsInTank * estimate.kmPerGallon
        settings.setOdometerUpdate(OdometerUpdate(baseKm + (fullRange - targetKm).roundToInt(), now))
    }

    private companion object {
        const val EXTRA_KM_LEFT = "km_restantes"
        const val EXTRA_CLEAR = "vaciar"
    }
}

