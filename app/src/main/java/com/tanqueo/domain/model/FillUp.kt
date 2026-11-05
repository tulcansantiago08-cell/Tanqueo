package com.tanqueo.domain.model

import java.time.Instant

enum class FuelType { CORRIENTE, EXTRA, DIESEL }

/** Un tanqueo registrado. [odometerKm] es opcional, pero sin él no aporta a la estimación. */
data class FillUp(
    val id: Long = 0,
    val at: Instant,
    val gallons: Double,
    val paidCop: Long,
    val fuelType: FuelType,
    val isFull: Boolean,
    val odometerKm: Int?,
)

/** Kilometraje que el usuario registró a mano con "Actualizar km". */
data class OdometerUpdate(val km: Int, val at: Instant)

data class TankSettings(
    val tankCapacityGallons: Double = DEFAULT_CAPACITY_GALLONS,
    val reminderKmThreshold: Int = DEFAULT_KM_THRESHOLD,
    val reminderDaysThreshold: Int = DEFAULT_DAYS_THRESHOLD,
) {
    companion object {
        const val DEFAULT_CAPACITY_GALLONS = 12.0
        const val DEFAULT_KM_THRESHOLD = 50
        const val DEFAULT_DAYS_THRESHOLD = 1
    }
}
