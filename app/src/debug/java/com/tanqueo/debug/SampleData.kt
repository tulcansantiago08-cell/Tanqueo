package com.tanqueo.debug

import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.FuelType
import java.time.Duration
import java.time.Instant

/** Seis tanqueos de las últimas semanas con kilometraje, relativos a la fecha actual. */
object SampleData {
    fun fillUps(now: Instant): List<FillUp> {
        fun daysAgo(days: Long) = now.minus(Duration.ofDays(days))
        return listOf(
            FillUp(at = daysAgo(31), gallons = 10.2, paidCop = 164_700, fuelType = FuelType.CORRIENTE, isFull = true, odometerKm = 40_120),
            FillUp(at = daysAgo(22), gallons = 10.5, paidCop = 169_600, fuelType = FuelType.CORRIENTE, isFull = true, odometerKm = 40_560),
            FillUp(at = daysAgo(17), gallons = 5.0, paidCop = 80_800, fuelType = FuelType.CORRIENTE, isFull = false, odometerKm = 40_810),
            FillUp(at = daysAgo(12), gallons = 6.7, paidCop = 108_200, fuelType = FuelType.CORRIENTE, isFull = true, odometerKm = 41_050),
            FillUp(at = daysAgo(3), gallons = 10.2, paidCop = 202_000, fuelType = FuelType.EXTRA, isFull = true, odometerKm = 41_480),
            FillUp(at = daysAgo(1), gallons = 1.5, paidCop = 24_300, fuelType = FuelType.CORRIENTE, isFull = false, odometerKm = 41_580),
        )
    }
}
