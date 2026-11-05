package com.tanqueo.domain.calc

import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.FuelType
import kotlin.math.round
import kotlin.math.roundToLong

/** Reglas de los valores que se ajustan con la corona: pasos, límites y sugerencias iniciales. */
object FillUpInput {
    const val MIN_GALLONS = 0.1
    const val MAX_GALLONS = 99.9
    const val DEFAULT_GALLONS = 10.0
    const val COP_STEP = 100L
    const val MAX_COP = 9_999_900L
    const val MAX_ODOMETER_KM = 2_999_999
    const val MIN_CAPACITY_GALLONS = 1.0
    const val MAX_CAPACITY_GALLONS = 60.0
    const val MAX_KM_THRESHOLD = 2_000
    const val MIN_KM_THRESHOLD = 10
    const val MAX_DAYS_THRESHOLD = 30

    /** Sube o baja galones en pasos de 0,1 sin arrastrar el error de los decimales. */
    fun adjustGallons(
        gallons: Double,
        steps: Int,
        min: Double = MIN_GALLONS,
        max: Double = MAX_GALLONS,
    ): Double = (round(gallons * 10 + steps) / 10).coerceIn(min, max)

    fun adjustCop(cop: Long, steps: Int): Long =
        (cop + steps * COP_STEP).coerceIn(COP_STEP, MAX_COP)

    fun adjustOdometer(km: Int, steps: Int, minKm: Int): Int =
        (km.toLong() + steps).coerceIn(minKm.toLong(), MAX_ODOMETER_KM.toLong()).toInt()

    fun adjustKmThreshold(km: Int, steps: Int): Int =
        (km + steps * MIN_KM_THRESHOLD).coerceIn(MIN_KM_THRESHOLD, MAX_KM_THRESHOLD)

    fun adjustDaysThreshold(days: Int, steps: Int): Int =
        (days + steps).coerceIn(1, MAX_DAYS_THRESHOLD)

    /** Precio por galón del último tanqueo de ese combustible; si no hay, del último en general. */
    fun lastPricePerGallon(fillUps: List<FillUp>, fuelType: FuelType): Double? {
        val byDate = fillUps.sortedByDescending { it.at }
        val reference = byDate.firstOrNull { it.fuelType == fuelType } ?: byDate.firstOrNull()
        return reference?.takeIf { it.gallons > 0 }?.let { it.paidCop / it.gallons }
    }

    /** Valor sugerido al cambiar los galones, redondeado al paso de $100. */
    fun suggestedPaid(gallons: Double, pricePerGallon: Double): Long =
        ((gallons * pricePerGallon / COP_STEP).roundToLong() * COP_STEP).coerceIn(COP_STEP, MAX_COP)

    /** El odómetro nunca retrocede: el mínimo es el mayor kilometraje ya registrado. */
    fun minOdometer(fillUps: List<FillUp>, extraKm: Int? = null): Int =
        maxOf(fillUps.mapNotNull { it.odometerKm }.maxOrNull() ?: 0, extraKm ?: 0)
}
