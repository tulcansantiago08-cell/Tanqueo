package com.tanqueo.domain.stations

import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Un punto en el mapa, en grados. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/** Una gasolinera de OpenStreetMap. [name] o [brand] pueden faltar: no todas están bien etiquetadas. */
data class GasStation(
    val name: String?,
    val brand: String?,
    val location: GeoPoint,
    val distanceKm: Double,
    /** La calle, si el directorio la da: ayuda a reconocer una gasolinera sin nombre. */
    val street: String? = null,
)

/** Resultado de buscar gasolineras cerca. Cada caso tiene su mensaje: nunca se inventa una lista. */
sealed interface StationSearch {
    /**
     * [origin]: dónde está el reloj, para centrar el mapa. [savedAt]: sin internet, la fecha de la
     * última búsqueda guardada que se está mostrando; null si la lista es de ahora.
     */
    data class Found(val origin: GeoPoint, val stations: List<GasStation>, val savedAt: Instant? = null) : StationSearch
    data object NoneNearby : StationSearch
    /** El reloj no sabe dónde está: sin GPS, con la ubicación apagada o sin señal. */
    data object NoLocation : StationSearch
    /** Sin internet o el servicio no respondió. */
    data object Offline : StationSearch
}

/** Dónde está el reloj ahora. Null si no se puede saber. */
fun interface LocationSource {
    suspend fun current(): GeoPoint?
}

/** Busca gasolineras alrededor de un punto. Null si no hubo respuesta (sin internet, servicio caído). */
fun interface StationDirectory {
    suspend fun around(point: GeoPoint, radiusMeters: Int): List<StationPlace>?
}

/** La última búsqueda que funcionó, para mostrarla cuando no hay internet. */
data class SavedStations(val origin: GeoPoint, val places: List<StationPlace>, val at: Instant)

/** Guarda la última búsqueda en el reloj. Si falla leer o escribir, se comporta como si no hubiera nada. */
interface StationCache {
    suspend fun load(): SavedStations?
    suspend fun save(saved: SavedStations)

    /** Para pruebas y para quien no quiere guardar nada. */
    object None : StationCache {
        override suspend fun load(): SavedStations? = null
        override suspend fun save(saved: SavedStations) = Unit
    }
}

/** Una gasolinera tal como llega del directorio, todavía sin distancia. */
data class StationPlace(val name: String?, val brand: String?, val location: GeoPoint, val street: String? = null)

object GasStationRanking {
    const val RADIUS_METERS = 5_000
    const val MAX_RESULTS = 15
    private const val EARTH_RADIUS_KM = 6_371.0

    /** Distancia en línea recta (haversine). Por carretera siempre es algo más. */
    fun distanceKm(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /** Las más cercanas primero, sin repetir la misma gasolinera mapeada dos veces. */
    fun rank(origin: GeoPoint, places: List<StationPlace>): List<GasStation> =
        places
            .map { GasStation(it.name, it.brand, it.location, distanceKm(origin, it.location), it.street) }
            // Un directorio puede devolver algo apenas fuera del radio: no se muestra.
            .filter { it.distanceKm <= RADIUS_METERS / 1000.0 }
            .sortedBy { it.distanceKm }
            .distinctBy { Triple(it.name, it.brand, Math.round(it.distanceKm * 20)) }
            .take(MAX_RESULTS)
}

/**
 * Ubica el reloj y busca. Todo lo que puede fallar termina en un [StationSearch] con su mensaje.
 * Sin internet muestra la última búsqueda guardada, si fue cerca de aquí y no es muy vieja.
 */
class FindGasStations(
    private val location: LocationSource,
    private val directory: StationDirectory,
    private val cache: StationCache = StationCache.None,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend operator fun invoke(): StationSearch {
        val here = location.current() ?: return StationSearch.NoLocation
        val places = directory.around(here, GasStationRanking.RADIUS_METERS)
        if (places == null) return fromCache(here) ?: StationSearch.Offline
        if (places.isNotEmpty()) cache.save(SavedStations(here, places, clock.instant()))
        val ranked = GasStationRanking.rank(here, places)
        return if (ranked.isEmpty()) StationSearch.NoneNearby else StationSearch.Found(here, ranked)
    }

    /** Las distancias se calculan desde donde está el reloj ahora, no desde donde se buscó. */
    private suspend fun fromCache(here: GeoPoint): StationSearch.Found? {
        val saved = cache.load() ?: return null
        val age = Duration.between(saved.at, clock.instant())
        if (age.isNegative || age > MAX_CACHE_AGE) return null
        if (GasStationRanking.distanceKm(here, saved.origin) > MAX_CACHE_DISTANCE_KM) return null
        val ranked = GasStationRanking.rank(here, saved.places)
        return ranked.takeIf { it.isNotEmpty() }?.let { StationSearch.Found(here, it, savedAt = saved.at) }
    }

    companion object {
        /** Las gasolineras casi no cambian, pero tampoco se muestra algo de hace meses. */
        val MAX_CACHE_AGE: Duration = Duration.ofDays(30)

        /** Más lejos de donde se buscó, la lista guardada ya no sirve: sería de otra zona. */
        const val MAX_CACHE_DISTANCE_KM = 3.0
    }
}
