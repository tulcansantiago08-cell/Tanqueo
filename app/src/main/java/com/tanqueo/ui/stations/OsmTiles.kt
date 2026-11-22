package com.tanqueo.ui.stations

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.tanqueo.domain.stations.TileKey
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Teselas del mapa de OpenStreetMap. Se piden solo las que se ven y se guardan en memoria mientras la
 * app está abierta, para no volver a descargarlas al moverse o hacer zoom (como pide su política de uso).
 */
object OsmTiles {
    private const val TIMEOUT_MS = 8_000
    private val cache = LruCache<TileKey, ImageBitmap>(48)

    fun cached(key: TileKey): ImageBitmap? = cache.get(key)

    /** Null si no se pudo descargar (sin internet): el mapa sigue con los marcadores sobre fondo oscuro. */
    suspend fun load(key: TileKey): ImageBitmap? {
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL("https://tile.openstreetmap.org/${key.zoom}/${key.x}/${key.y}.png").openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = TIMEOUT_MS
                    connection.readTimeout = TIMEOUT_MS
                    // OpenStreetMap pide identificar la app; va un nombre genérico, sin datos personales.
                    connection.setRequestProperty("User-Agent", "Tanqueo-WearOS/0.1")
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                    val bytes = connection.inputStream.use { it.readBytes() }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()?.also { cache.put(key, it) }
                } finally {
                    connection.disconnect()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }
}
