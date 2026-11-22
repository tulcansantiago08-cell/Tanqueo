package com.tanqueo.domain.usecase

import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.calc.ReminderPolicy
import com.tanqueo.domain.model.VehicleType
import javax.inject.Inject

/** Revisa la estimación y devuelve lo que hay que avisar, o null si no toca avisar. */
class CheckRefuelReminder @Inject constructor(private val observeDashboard: ObserveDashboard) {
    /** Solo [vehicle], o el activo si es null, con sus propios datos y umbrales. */
    suspend operator fun invoke(vehicle: VehicleType? = null): RangeEstimate.Available? {
        val dashboard = observeDashboard.current(vehicle)
        return (dashboard.estimate as? RangeEstimate.Available)
            ?.takeIf { ReminderPolicy.shouldNotify(it, dashboard.settings) }
    }

    /** Cada vehículo por separado: la moto puede necesitar gasolina aunque el carro no. */
    suspend fun allVehicles(): Map<VehicleType, RangeEstimate.Available?> =
        VehicleType.entries.associateWith { invoke(it) }
}
