package com.tanqueo.data.stations

import com.tanqueo.domain.stations.GeoPoint
import com.tanqueo.domain.stations.StationDirectory
import com.tanqueo.domain.stations.StationPlace
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Gasolineras de OpenStreetMap por medio de Overpass: gratis, sin clave y sin cuenta. Solo se envía el
 * punto de búsqueda (redondeado a unos 100 m) y el radio; nada más de la persona.
 */
class OverpassStationDirectory @Inject constructor() : StationDirectory {

    override suspend fun around(point: GeoPoint, radiusMeters: Int): List<StationPlace>? = withContext(Dispatchers.IO) {
        try {
            withTimeoutOrNull(TIMEOUT_MS) { parseOverpass(post(query(point, radiusMeters))) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun post(body: String): String {
        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = TIMEOUT_MS.toInt()
            connection.readTimeout = TIMEOUT_MS.toInt()
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            // Overpass pide identificar al cliente; va un nombre genérico de la app, sin datos personales.
            connection.setRequestProperty("User-Agent", "Tanqueo-WearOS/0.1")
            connection.outputStream.use { it.write(("data=" + URLEncoder.encode(body, "UTF-8")).toByteArray()) }
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "Overpass ${connection.responseCode}" }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val ENDPOINT = "https://overpass-api.de/api/interpreter"
        private const val TIMEOUT_MS = 12_000L

        /** Nodos y áreas con amenity=fuel; "out center" da un punto también para las áreas. */
        internal fun query(point: GeoPoint, radiusMeters: Int): String {
            val lat = String.format(Locale.US, "%.3f", point.latitude)
            val lon = String.format(Locale.US, "%.3f", point.longitude)
            val around = "around:$radiusMeters,$lat,$lon"
            return "[out:json][timeout:10];(node[\"amenity\"=\"fuel\"]($around);way[\"amenity\"=\"fuel\"]($around););out center 60;"
        }
    }
}

@Serializable
private data class OverpassResponse(val elements: List<OverpassElement> = emptyList())

@Serializable
private data class OverpassElement(
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String> = emptyMap(),
)

@Serializable
private data class OverpassCenter(val lat: Double, val lon: Double)

private val json = Json { ignoreUnknownKeys = true }

/** Lo que respondió Overpass, como gasolineras. Las que no traen ubicación se descartan. */
internal fun parseOverpass(body: String): List<StationPlace> =
    json.decodeFromString<OverpassResponse>(body).elements.mapNotNull { element ->
        val lat = element.lat ?: element.center?.lat ?: return@mapNotNull null
        val lon = element.lon ?: element.center?.lon ?: return@mapNotNull null
        StationPlace(
            name = element.tags["name"]?.takeIf { it.isNotBlank() },
            brand = (element.tags["brand"] ?: element.tags["operator"])?.takeIf { it.isNotBlank() },
            location = GeoPoint(lat, lon),
        )
    }
