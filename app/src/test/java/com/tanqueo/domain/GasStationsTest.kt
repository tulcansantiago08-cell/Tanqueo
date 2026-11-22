package com.tanqueo.domain

import com.tanqueo.domain.format.Formats
import com.tanqueo.domain.stations.FindGasStations
import com.tanqueo.domain.stations.GasStationRanking
import com.tanqueo.domain.stations.GeoPoint
import com.tanqueo.domain.stations.StationPlace
import com.tanqueo.domain.stations.StationSearch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GasStationsTest {
    private val pasto = GeoPoint(1.2136, -77.2811)

    @Test fun `la distancia en linea recta es la esperada`() {
        // Pasto–Ipiales: unos 63 km en línea recta.
        val ipiales = GeoPoint(0.8302, -77.6444)
        assertEquals(58.6, GasStationRanking.distanceKm(pasto, ipiales), 1.5)
        assertEquals(0.0, GasStationRanking.distanceKm(pasto, pasto), 0.0001)
    }

    @Test fun `ordena de la mas cercana a la mas lejana y quita repetidas`() {
        val near = StationPlace("Terpel Centro", "Terpel", GeoPoint(1.2140, -77.2811))
        val far = StationPlace("Primax Norte", "Primax", GeoPoint(1.2300, -77.2811))
        val duplicate = near.copy(location = GeoPoint(1.21401, -77.28111))
        val ranked = GasStationRanking.rank(pasto, listOf(far, near, duplicate))
        assertEquals(listOf("Terpel Centro", "Primax Norte"), ranked.map { it.name })
        assertTrue(ranked[0].distanceKm < ranked[1].distanceKm)
    }

    @Test fun `sin ubicacion no busca y sin internet lo dice`() = runTest {
        var asked = false
        val noLocation = FindGasStations({ null }, { _, _ -> asked = true; emptyList() })
        assertEquals(StationSearch.NoLocation, noLocation())
        assertTrue(!asked)
        assertEquals(StationSearch.Offline, FindGasStations({ pasto }, { _, _ -> null })())
        assertEquals(StationSearch.NoneNearby, FindGasStations({ pasto }, { _, _ -> emptyList() })())
    }

    @Test fun `encuentra y entrega las gasolineras ordenadas`() = runTest {
        val place = StationPlace(null, "Biomax", GeoPoint(1.2150, -77.2811))
        val result = FindGasStations({ pasto }, { _, radius -> assertEquals(5_000, radius); listOf(place) })()
        result as StationSearch.Found
        assertEquals("Biomax", result.stations.single().brand)
    }

    @Test fun `distancias cortas en metros y largas en km`() {
        assertEquals("350 m", Formats.distance(0.34))
        assertEquals("50 m", Formats.distance(0.001))
        assertEquals("1,2 km", Formats.distance(1.23))
        assertEquals("1,0 km", Formats.distance(0.99))
    }

    @Test fun `no muestra lo que queda fuera del radio`() {
        val outside = StationPlace("Lejana", null, GeoPoint(1.30, -77.2811)) // ~9,5 km al norte
        assertTrue(GasStationRanking.rank(pasto, listOf(outside)).isEmpty())
    }
}
