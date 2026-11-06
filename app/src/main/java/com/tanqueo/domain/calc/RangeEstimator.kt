package com.tanqueo.domain.calc

import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.OdometerUpdate
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.math.floor

/** Resultado de la estimación de autonomía. Sin datos suficientes no se inventa ninguna cifra. */
sealed interface RangeEstimate {
    data object Insufficient : RangeEstimate

    data class Available(
        val kmRemaining: Double,
        /** Null si no hay promedio diario todavía (solo se conoce el km real). */
        val daysRemaining: Double?,
        val kmPerGallon: Double,
        val kmPerDay: Double?,
        val gallonsInTank: Double,
        val kmSinceLastFillUp: Double,
        /** True si los km recorridos parten de un "Actualizar km" y no solo del promedio diario. */
        val usesManualOdometer: Boolean,
        /** Parte del tanque que queda ahora, de 0 a 1. Es lo que llena el anillo. */
        val tankFraction: Double,
    ) : RangeEstimate
}

/** Cálculos de rendimiento y autonomía. Funciones puras; el orden de la lista de entrada no importa. */
object RangeEstimator {
    /** Número de tramos recientes que entran en cada promedio. */
    const val MAX_SEGMENTS = 5

    private const val MILLIS_PER_DAY = 86_400_000.0

    /** Menos de un día de historia daría promedios diarios absurdos: mejor no estimar. */
    private const val MIN_DAYS_FOR_DAILY_AVERAGE = 1.0

    /**
     * Rendimiento por el método de tanque lleno. Un tramo va de un lleno con km al siguiente lleno
     * con km; los galones son todos los cargados después del primero y hasta el segundo inclusive,
     * porque los parciales intermedios también se consumieron en ese recorrido.
     * Devuelve el promedio de los km/galón de los últimos [MAX_SEGMENTS] tramos.
     */
    /** Qué día se acabaría: los días completos que alcanza contados desde hoy. */
    fun refuelDate(today: LocalDate, daysRemaining: Double): LocalDate =
        today.plusDays(floor(daysRemaining).toLong())

    fun kmPerGallon(fillUps: List<FillUp>): Double? {
        val sorted = fillUps.chronological()
        val anchors = sorted.indices.filter { sorted[it].isFull && sorted[it].odometerKm != null }
        val segments = anchors.zipWithNext().mapNotNull { (from, to) ->
            val km = sorted[to].odometerKm!! - sorted[from].odometerKm!!
            val gallons = sorted.subList(from + 1, to + 1).sumOf { it.gallons }
            if (km > 0 && gallons > 0) km / gallons else null
        }
        return segments.takeLast(MAX_SEGMENTS).takeIf { it.isNotEmpty() }?.average()
    }

    /**
     * Km por día = km ÷ días entre tanqueos consecutivos con kilometraje (llenos o parciales),
     * acumulados en los últimos [MAX_SEGMENTS] tramos. Null con menos de un día de historia.
     */
    fun kmPerDay(fillUps: List<FillUp>): Double? {
        val segments = fillUps.chronological()
            .filter { it.odometerKm != null }
            .zipWithNext()
            .map { (from, to) -> (to.odometerKm!! - from.odometerKm!!) to daysBetween(from.at, to.at) }
            .filter { (km, days) -> km > 0 && days > 0 }
            .takeLast(MAX_SEGMENTS)
        val totalDays = segments.sumOf { it.second }
        if (totalDays < MIN_DAYS_FOR_DAILY_AVERAGE) return null
        return segments.sumOf { it.first } / totalDays
    }

    /**
     * Galones en el tanque justo después del último tanqueo. Si fue lleno, la capacidad. Si fue
     * parcial, se parte del último lleno y se descuenta lo consumido en cada tramo y se suma lo
     * cargado. Null si nunca hubo un lleno o si falta con qué calcular un tramo.
     */
    fun gallonsAfterLastFillUp(
        fillUps: List<FillUp>,
        capacityGallons: Double,
        kmPerGallon: Double,
        kmPerDay: Double?,
    ): Double? {
        val sorted = fillUps.chronological()
        val lastFull = sorted.indexOfLast { it.isFull }
        if (lastFull < 0) return null

        var gallons = capacityGallons
        for (i in lastFull + 1 until sorted.size) {
            val km = kmBetween(sorted[i - 1], sorted[i], kmPerDay) ?: return null
            gallons = ((gallons - km / kmPerGallon).coerceAtLeast(0.0) + sorted[i].gallons)
                .coerceAtMost(capacityGallons)
        }
        return gallons
    }

    /**
     * Autonomía restante = galones tras el último tanqueo × rendimiento − km recorridos desde
     * entonces. Los km recorridos son los reales si hay un "Actualizar km" posterior al último
     * tanqueo (más el promedio diario solo por los días que pasaron desde esa lectura); si no, se
     * estiman con el promedio diario.
     */
    fun estimate(
        fillUps: List<FillUp>,
        capacityGallons: Double,
        odometerUpdate: OdometerUpdate?,
        now: Instant,
    ): RangeEstimate {
        val last = fillUps.chronological().lastOrNull() ?: return RangeEstimate.Insufficient
        val kmPerGallon = kmPerGallon(fillUps) ?: return RangeEstimate.Insufficient
        val kmPerDay = kmPerDay(fillUps)
        val gallons = gallonsAfterLastFillUp(fillUps, capacityGallons, kmPerGallon, kmPerDay)
            ?: return RangeEstimate.Insufficient

        val realKm = realKmSince(last, odometerUpdate)
        val kmSince = when {
            realKm != null -> realKm + (kmPerDay ?: 0.0) * daysBetween(odometerUpdate!!.at, now)
            kmPerDay != null -> kmPerDay * daysBetween(last.at, now)
            else -> return RangeEstimate.Insufficient
        }

        val kmRemaining = (gallons * kmPerGallon - kmSince).coerceAtLeast(0.0)
        return RangeEstimate.Available(
            kmRemaining = kmRemaining,
            daysRemaining = kmPerDay?.let { kmRemaining / it },
            kmPerGallon = kmPerGallon,
            kmPerDay = kmPerDay,
            gallonsInTank = gallons,
            kmSinceLastFillUp = kmSince,
            usesManualOdometer = realKm != null,
            tankFraction = (kmRemaining / (capacityGallons * kmPerGallon)).coerceIn(0.0, 1.0),
        )
    }

    /** La lectura de odómetro más reciente, de un tanqueo o de "Actualizar km". Nunca se estima. */
    fun latestOdometer(fillUps: List<FillUp>, odometerUpdate: OdometerUpdate?): Int? {
        val lastWithKm = fillUps.chronological().lastOrNull { it.odometerKm != null }
        return when {
            lastWithKm == null -> odometerUpdate?.km
            odometerUpdate != null && !odometerUpdate.at.isBefore(lastWithKm.at) -> odometerUpdate.km
            else -> lastWithKm.odometerKm
        }
    }

    /** Solo vale una lectura posterior al último tanqueo y que no retroceda frente a su odómetro. */
    private fun realKmSince(last: FillUp, update: OdometerUpdate?): Double? {
        val baseKm = last.odometerKm ?: return null
        if (update == null || update.at.isBefore(last.at) || update.km < baseKm) return null
        return (update.km - baseKm).toDouble()
    }

    private fun kmBetween(from: FillUp, to: FillUp, kmPerDay: Double?): Double? {
        val fromKm = from.odometerKm
        val toKm = to.odometerKm
        if (fromKm != null && toKm != null && toKm >= fromKm) return (toKm - fromKm).toDouble()
        return kmPerDay?.let { it * daysBetween(from.at, to.at) }
    }

    private fun daysBetween(from: Instant, to: Instant): Double =
        (Duration.between(from, to).toMillis() / MILLIS_PER_DAY).coerceAtLeast(0.0)

    private fun List<FillUp>.chronological() = sortedWith(compareBy({ it.at }, { it.id }))
}
