package com.tanqueo.data.stations

import com.tanqueo.domain.stations.GeoPoint
import com.tanqueo.domain.stations.StationDirectory
import com.tanqueo.domain.stations.StationPlace
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import kotlin.math.cos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Gasolineras de OpenStreetMap por medio de Nominatim: gratis y sin clave. Se busca amenity=fuel dentro
 * de un cuadro alrededor del punto (redondeado a unos 100 m). Solo se llama cuando la persona abre la
 * pantalla, muy por debajo del límite de una consulta por segundo.
 */
class NominatimStationDirectory @Inject constructor() : StationDirectory {

    override suspend fun around(point: GeoPoint, radiusMeters: Int): List<StationPlace>? = withContext(Dispatchers.IO) {
        try {
            withTimeoutOrNull(TIMEOUT_MS) { parseNominatim(get(url(point, radiusMeters))) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun get(address: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS.toInt()
            connection.readTimeout = TIMEOUT_MS.toInt()
            // Nominatim pide identificar la app; va un nombre genérico, sin datos personales.
            connection.setRequestProperty("User-Agent", "Tanqueo-WearOS/0.1")
            connection.setRequestProperty("Accept-Language", "es")
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "Nominatim ${connection.responseCode}" }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val TIMEOUT_MS = 10_000L
        private const val METERS_PER_DEGREE = 111_320.0
        private const val LIMIT = 40

        /** Cuadro de [radiusMeters] alrededor del punto: oeste, norte, este, sur. */
        internal fun url(point: GeoPoint, radiusMeters: Int): String {
            val lat = Math.round(point.latitude * 1000) / 1000.0
            val lon = Math.round(point.longitude * 1000) / 1000.0
            val dLat = radiusMeters / METERS_PER_DEGREE
            val dLon = radiusMeters / (METERS_PER_DEGREE * cos(Math.toRadians(lat)).coerceAtLeast(0.01))
            val box = listOf(lon - dLon, lat + dLat, lon + dLon, lat - dLat).joinToString(",") { String.format(Locale.US, "%.4f", it) }
            return "https://nominatim.openstreetmap.org/search?amenity=fuel&format=jsonv2&extratags=1&bounded=1&limit=$LIMIT&viewbox=$box"
        }
    }
}

@Serializable
private data class NominatimPlace(
    val lat: String,
    val lon: String,
    val name: String? = null,
    @kotlinx.serialization.SerialName("display_name") val displayName: String? = null,
    val extratags: Map<String, String>? = null,
)

private val json = Json { ignoreUnknownKeys = true }

internal fun parseNominatim(body: String): List<StationPlace> =
    json.decodeFromString<List<NominatimPlace>>(body).mapNotNull { place ->
        val lat = place.lat.toDoubleOrNull() ?: return@mapNotNull null
        val lon = place.lon.toDoubleOrNull() ?: return@mapNotNull null
        val name = place.name?.takeIf { it.isNotBlank() }
        val tags = place.extratags.orEmpty()
        // display_name empieza por el nombre (si lo hay) y sigue con la calle.
        val parts = place.displayName.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val street = (if (name != null && parts.firstOrNull() == name) parts.getOrNull(1) else parts.firstOrNull())
            ?.takeIf { it.any(Char::isLetter) }
        StationPlace(
            name = name,
            brand = (tags["brand"] ?: tags["operator"])?.takeIf { it.isNotBlank() },
            location = GeoPoint(lat, lon),
            street = street,
        )
    }
