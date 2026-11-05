package com.tanqueo.domain.usecase

import com.tanqueo.domain.calc.MonthSummary
import com.tanqueo.domain.calc.MonthSummaryCalculator
import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.calc.RangeEstimator
import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.OdometerUpdate
import com.tanqueo.domain.model.TankSettings
import com.tanqueo.domain.repository.FillUpRepository
import com.tanqueo.domain.repository.SettingsRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/** Todo lo que muestran Inicio, Resumen y la tile, y lo que revisa el aviso diario. */
data class Dashboard(
    val estimate: RangeEstimate,
    val month: MonthSummary,
    val kmPerGallon: Double?,
    val settings: TankSettings,
)

class ObserveDashboard @Inject constructor(
    private val fillUps: FillUpRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    /** Se recalcula cuando cambian los datos y cada minuto, porque la estimación depende de la hora. */
    fun observe(tickMillis: Long = 60_000): Flow<Dashboard> = combine(
        fillUps.observeAll(),
        settings.settings,
        settings.odometerUpdate,
        flow {
            while (true) {
                emit(Unit)
                delay(tickMillis)
            }
        },
    ) { list, config, update, _ -> build(list, config, update) }

    suspend fun current(): Dashboard =
        build(fillUps.observeAll().first(), settings.settings.first(), settings.odometerUpdate.first())

    private fun build(list: List<FillUp>, config: TankSettings, update: OdometerUpdate?): Dashboard {
        val now = clock.instant()
        return Dashboard(
            estimate = RangeEstimator.estimate(list, config.tankCapacityGallons, update, now),
            month = MonthSummaryCalculator.calculate(list, now, clock.zone),
            kmPerGallon = RangeEstimator.kmPerGallon(list),
            settings = config,
        )
    }
}
