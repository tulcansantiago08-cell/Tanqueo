package com.tanqueo.debug

import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.FuelType
import com.tanqueo.domain.model.VehicleType
import java.time.Duration
import java.time.Instant

/** Tanqueos de ejemplo de las últimas semanas con kilometraje, relativos a la fecha actual. */
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

    /** Una moto de tanque de 3 galones que rinde unos 100 km/galón y anda unos 35 km al día. */
    fun motoFillUps(now: Instant): List<FillUp> {
        fun daysAgo(days: Long) = now.minus(Duration.ofDays(days))
        fun moto(days: Long, gallons: Double, paid: Long, full: Boolean, km: Int) =
            FillUp(at = daysAgo(days), gallons = gallons, paidCop = paid, fuelType = FuelType.CORRIENTE, isFull = full, odometerKm = km, vehicle = VehicleType.MOTO)
        return listOf(
            moto(20, 2.8, 44_500, full = true, km = 12_000),
            moto(13, 2.6, 41_300, full = true, km = 12_260),
            moto(8, 1.0, 15_900, full = false, km = 12_430),
            moto(4, 2.0, 31_800, full = true, km = 12_560),
            moto(1, 1.1, 17_500, full = true, km = 12_665),
        )
    }
}
