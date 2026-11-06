package com.tanqueo.domain.assistant

import kotlin.math.max
import kotlin.math.min

/** Recarga en el camino. [place] es null si no hay una ciudad conocida antes de quedarse sin gasolina. */
data class TripStop(val place: String?, val atKm: Int, val gallons: Double)

private const val MIN_GALLONS_TO_BUY = 0.05

data class TripPlan(
    val km: Int,
    /** Lo que gasta el viaje, con la reserva incluida. */
    val gallonsNeeded: Double,
    val gallonsInTank: Double,
    /** Lo que hay que echar antes de salir; cero si ya alcanza. */
    val gallonsBeforeLeaving: Double,
    val gallonsTotal: Double,
    val stops: List<TripStop>,
    /** Null si no se conoce ningún precio. */
    val pricePerGallon: Double?,
) {
    /** Lo que cuesta recorrer el viaje, sin importar cuánto haya ya en el tanque. */
    val copOfTrip: Double? get() = pricePerGallon?.let { it * gallonsNeeded }
    val copBeforeLeaving: Double? get() = pricePerGallon?.let { it * gallonsBeforeLeaving }
    val copTotal: Double? get() = pricePerGallon?.let { it * gallonsTotal }

    /** Menos de lo que se muestra con un decimal no es tanquear: se lee como que ya alcanza. */
    val alreadyEnough: Boolean get() = gallonsTotal < MIN_GALLONS_TO_BUY
}

/**
 * Rendimiento para quien todavía no tiene historial. No hay una cifra oficial actual en Colombia: lo
 * publicado para un carro particular va de 25 a 40 km/galón (SENA en Semana, UPME 2009, notas de prensa
 * sobre motores 1.4), así que se usa un valor intermedio y la app siempre dice que es de referencia.
 * Apenas hay dos llenos con km, manda el consumo real del vehículo.
 */
object ConsumptionReference {
    const val KM_PER_GALLON = 35.0
}

object TripPlanner {
    /** En carretera y cordillera se gasta distinto que a diario: mejor que sobre a que falte. */
    const val RESERVE_FRACTION = 0.10

    /** Evita un ciclo sin fin si llegaran datos absurdos. */
    private const val MAX_STOPS = 20

    fun plan(
        km: Int,
        waypoints: List<Waypoint>,
        kmPerGallon: Double,
        capacityGallons: Double,
        gallonsInTank: Double,
        pricePerGallon: Double?,
    ): TripPlan {
        val inTank = gallonsInTank.coerceIn(0.0, capacityGallons)
        val effectiveKmPerGallon = kmPerGallon / (1 + RESERVE_FRACTION)
        val needed = km / effectiveKmPerGallon
        val total = max(0.0, needed - inTank)
        val before = min(total, capacityGallons - inTank)

        val stops = mutableListOf<TripStop>()
        var fuel = inTank + before
        var position = 0.0
        while (position + fuel * effectiveKmPerGallon < km - 0.5 && stops.size < MAX_STOPS) {
            val reach = position + fuel * effectiveKmPerGallon
            val city = waypoints.lastOrNull { it.atKm > position && it.atKm <= reach }
            val stopAt = city?.atKm?.toDouble() ?: reach
            fuel -= (stopAt - position) / effectiveKmPerGallon
            position = stopAt
            val add = min(capacityGallons - fuel, (km - position) / effectiveKmPerGallon - fuel)
            stops += TripStop(city?.place, stopAt.toInt(), add)
            fuel += add
        }
        return TripPlan(km, needed, inTank, before, total, stops, pricePerGallon)
    }
}
