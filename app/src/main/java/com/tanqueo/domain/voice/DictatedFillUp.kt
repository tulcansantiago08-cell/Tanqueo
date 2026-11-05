package com.tanqueo.domain.voice

import com.tanqueo.domain.calc.FillUpInput
import com.tanqueo.domain.model.FuelType

/** Lo que se entendió de una frase dictada. Lo que no se dijo queda en null: nunca se inventa. */
data class DictatedFillUp(
    val gallons: Double? = null,
    val paidCop: Long? = null,
    val fuelType: FuelType? = null,
    val isFull: Boolean? = null,
    val odometerKm: Int? = null,
) {
    val isEmpty: Boolean
        get() = gallons == null && paidCop == null && fuelType == null && isFull == null && odometerKm == null

    /** Completa lo que falte con [other]; lo que ya se entendió no se pisa. */
    fun orElse(other: DictatedFillUp) = DictatedFillUp(
        gallons = gallons ?: other.gallons,
        paidCop = paidCop ?: other.paidCop,
        fuelType = fuelType ?: other.fuelType,
        isFull = isFull ?: other.isFull,
        odometerKm = odometerKm ?: other.odometerKm,
    )

    /**
     * Descarta valores imposibles venga de donde venga (también de la IA): quedan en null en vez
     * de corregirlos, para que el usuario los ponga. El odómetro no puede bajar de [minOdometerKm].
     */
    fun sanitized(minOdometerKm: Int) = DictatedFillUp(
        gallons = gallons?.takeIf { it >= FillUpInput.MIN_GALLONS && it <= FillUpInput.MAX_GALLONS }
            ?.let { FillUpInput.adjustGallons(it, 0) },
        paidCop = paidCop?.takeIf { it in FillUpInput.COP_STEP..FillUpInput.MAX_COP },
        fuelType = fuelType,
        isFull = isFull,
        odometerKm = odometerKm?.takeIf { it in minOdometerKm..FillUpInput.MAX_ODOMETER_KM && it > 0 },
    )
}
