package com.tanqueo.data.stations

import android.content.Context
import com.tanqueo.domain.stations.GeoPoint
import com.tanqueo.domain.stations.SavedStations
import com.tanqueo.domain.stations.StationCache
import com.tanqueo.domain.stations.StationPlace
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * La última búsqueda de gasolineras, en un archivo JSON del reloj. No entra en la copia de
 * seguridad (ver data_extraction_rules.xml): solo sirve para ver algo cuando no hay internet.
 */
@Singleton
class FileStationCache internal constructor(private val file: File) : StationCache {
    @Inject constructor(@ApplicationContext context: Context) : this(File(context.noBackupFilesDir, FILE_NAME))

    override suspend fun load(): SavedStations? = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext null
            decode(JSONObject(file.readText()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null // archivo dañado o de otra versión: como si no hubiera nada
        }
    }

    override suspend fun save(saved: SavedStations) = withContext(Dispatchers.IO) {
        try {
            // Se escribe aparte y se renombra: un corte a mitad no deja un archivo a medias.
            val tmp = File(file.parentFile, "$FILE_NAME.tmp")
            tmp.writeText(encode(saved).toString())
            if (!tmp.renameTo(file)) tmp.delete()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // No poder guardar no cambia la búsqueda que ya se hizo.
        }
        Unit
    }

    companion object {
        private const val FILE_NAME = "gasolineras.json"
        private const val VERSION = 1

        internal fun encode(saved: SavedStations) = JSONObject()
            .put("version", VERSION)
            .put("at", saved.at.toEpochMilli())
            .put("lat", saved.origin.latitude)
            .put("lon", saved.origin.longitude)
            .put(
                "places",
                JSONArray().apply {
                    saved.places.forEach { place ->
                        put(
                            JSONObject()
                                .put("lat", place.location.latitude)
                                .put("lon", place.location.longitude)
                                .putOpt("name", place.name)
                                .putOpt("brand", place.brand)
                                .putOpt("street", place.street),
                        )
                    }
                },
            )

        internal fun decode(json: JSONObject): SavedStations? {
            if (json.optInt("version") != VERSION) return null
            val places = json.getJSONArray("places")
            return SavedStations(
                origin = GeoPoint(json.getDouble("lat"), json.getDouble("lon")),
                places = (0 until places.length()).map { i ->
                    val p = places.getJSONObject(i)
                    StationPlace(
                        name = p.optStringOrNull("name"),
                        brand = p.optStringOrNull("brand"),
                        location = GeoPoint(p.getDouble("lat"), p.getDouble("lon")),
                        street = p.optStringOrNull("street"),
                    )
                },
                at = Instant.ofEpochMilli(json.getLong("at")),
            )
        }

        private fun JSONObject.optStringOrNull(key: String): String? =
            if (has(key) && !isNull(key)) getString(key) else null
    }
}
