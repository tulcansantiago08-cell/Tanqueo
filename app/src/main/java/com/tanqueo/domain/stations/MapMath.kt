package com.tanqueo.domain.stations

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/** Un punto en píxeles del mapa del mundo a un zoom dado (proyección Web Mercator, teselas de 256). */
data class WorldPixel(val x: Double, val y: Double)

/** Una tesela de OpenStreetMap: zoom, columna y fila. */
data class TileKey(val zoom: Int, val x: Int, val y: Int)

/** Cuentas del mapa en funciones puras: de grados a píxeles, qué teselas se ven y qué zoom conviene. */
object MapMath {
    const val TILE_SIZE = 256
    const val MIN_ZOOM = 11
    const val MAX_ZOOM = 17

    /** Las [MAP_STATIONS] más cercanas son las que se marcan en el mapa y se numeran en la lista. */
    const val MAP_STATIONS = 9

    fun toPixel(point: GeoPoint, zoom: Int): WorldPixel {
        val scale = TILE_SIZE * (1 shl zoom).toDouble()
        val lat = Math.toRadians(point.latitude.coerceIn(-85.0, 85.0))
        val x = (point.longitude + 180.0) / 360.0 * scale
        val y = (1.0 - ln(tan(lat) + 1.0 / cos(lat)) / PI) / 2.0 * scale
        return WorldPixel(x, y)
    }

    /** Teselas que cubren una ventana de [width]×[height] píxeles centrada en [center]. */
    fun visibleTiles(center: GeoPoint, zoom: Int, width: Double, height: Double): List<TileKey> {
        val c = toPixel(center, zoom)
        val max = (1 shl zoom) - 1
        val firstX = floor((c.x - width / 2) / TILE_SIZE).toInt()
        val lastX = floor((c.x + width / 2) / TILE_SIZE).toInt()
        val firstY = floor((c.y - height / 2) / TILE_SIZE).toInt().coerceAtLeast(0)
        val lastY = floor((c.y + height / 2) / TILE_SIZE).toInt().coerceAtMost(max)
        return (firstY..lastY).flatMap { y -> (firstX..lastX).map { x -> TileKey(zoom, x.mod(max + 1), y) } }
    }

    /**
     * El zoom más cercano en el que [points] caben alrededor de [center], dejando un margen para que
     * ningún marcador quede pegado al borde.
     */
    fun fitZoom(center: GeoPoint, points: List<GeoPoint>, width: Double, height: Double): Int {
        if (points.isEmpty()) return 15
        for (zoom in MAX_ZOOM downTo MIN_ZOOM) {
            val c = toPixel(center, zoom)
            val fits = points.all {
                val p = toPixel(it, zoom)
                kotlin.math.abs(p.x - c.x) <= width * 0.42 && kotlin.math.abs(p.y - c.y) <= height * 0.42
            }
            if (fits) return zoom
        }
        return MIN_ZOOM
    }
}
