package com.tanqueo.data.stations

import com.tanqueo.domain.stations.GeoPoint
import com.tanqueo.domain.stations.StationDirectory
import com.tanqueo.domain.stations.StationPlace
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NominatimStationDirectoryTest {
    @Test fun `lee nombre, marca y calle, y sin nombre muestra la calle`() {
        val body = """
            [
              {"lat":"1.2047786","lon":"-77.2716377","name":"Estación de Servicio Metropolitana",
               "display_name":"Estación de Servicio Metropolitana, Calle 20, El Recuerdo, Pasto, Colombia",
               "extratags":{"operator":"Flota Guáitara S.A."}},
              {"lat":"1.1994028","lon":"-77.2783241","name":"",
               "display_name":"Avenida Alfonso Zambrano, Las Lunas I Etapa, Pasto, Colombia",
               "extratags":{"building":"roof","operator":"Terpel"}},
              {"lat":"x","lon":"-77.27","name":"Sin coordenadas"}
            ]
        """.trimIndent()
        val places = parseNominatim(body)
        assertEquals(2, places.size)
        assertEquals("Estación de Servicio Metropolitana", places[0].name)
        assertEquals("Calle 20", places[0].street)
        assertEquals("Flota Guáitara S.A.", places[0].brand)
        assertNull(places[1].name)
        assertEquals("Terpel", places[1].brand)
        assertEquals("Avenida Alfonso Zambrano", places[1].street)
    }

    @Test fun `busca en un cuadro alrededor del punto redondeado`() {
        val url = NominatimStationDirectory.url(GeoPoint(1.213612, -77.281145), 5_000)
        assertTrue(url.startsWith("https://nominatim.openstreetmap.org/search?amenity=fuel"))
        assertTrue(url.contains("bounded=1"))
        assertTrue(url.contains("viewbox=-77.3259,1.2589,-77.2361,1.1691"))
    }

    @Test fun `si el primero no responde usa el siguiente`() = runTest {
        val place = StationPlace("Terpel", null, GeoPoint(1.2, -77.2))
        val failing = StationDirectory { _, _ -> null }
        val working = StationDirectory { _, _ -> listOf(place) }
        assertEquals(listOf(place), FallbackStationDirectory(listOf(failing, working)).around(GeoPoint(1.2, -77.2), 5_000))
        assertNull(FallbackStationDirectory(listOf(failing, failing)).around(GeoPoint(1.2, -77.2), 5_000))
    }
}
