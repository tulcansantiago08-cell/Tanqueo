package com.tanqueo.domain

import com.tanqueo.domain.stations.GeoPoint
import com.tanqueo.domain.stations.MapMath
import com.tanqueo.domain.stations.TileKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapMathTest {
    private val pasto = GeoPoint(1.2136, -77.2811)

    @Test fun `el centro del mundo cae en el centro de la primera tesela`() {
        val p = MapMath.toPixel(GeoPoint(0.0, 0.0), 0)
        assertEquals(128.0, p.x, 0.0001)
        assertEquals(128.0, p.y, 0.0001)
    }

    @Test fun `Pasto cae en la tesela que usa OpenStreetMap`() {
        val p = MapMath.toPixel(pasto, 15)
        // Fórmula estándar de OSM: x = (−77,2811 + 180) / 360 × 2^15 ≈ 9349,7; y ≈ 16273,5.
        assertEquals(TileKey(15, 9349, 16273), TileKey(15, (p.x / 256).toInt(), (p.y / 256).toInt()))
    }

    @Test fun `una ventana pequena pide pocas teselas y todas del mismo zoom`() {
        val tiles = MapMath.visibleTiles(pasto, 15, 260.0, 180.0)
        assertTrue(tiles.size in 2..6)
        assertTrue(tiles.all { it.zoom == 15 })
    }

    @Test fun `el zoom se aleja hasta que caben las gasolineras`() {
        val near = listOf(GeoPoint(1.2140, -77.2815))
        val far = listOf(GeoPoint(1.2500, -77.2811))
        val zoomNear = MapMath.fitZoom(pasto, near, 260.0, 180.0)
        val zoomFar = MapMath.fitZoom(pasto, far, 260.0, 180.0)
        assertEquals(MapMath.MAX_ZOOM, zoomNear)
        assertTrue(zoomFar < zoomNear)
        val c = MapMath.toPixel(pasto, zoomFar)
        val p = MapMath.toPixel(far.single(), zoomFar)
        assertTrue(kotlin.math.abs(p.y - c.y) <= 180.0 * 0.42)
    }
}
