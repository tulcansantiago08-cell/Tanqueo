package com.tanqueo.data.stations

import com.tanqueo.domain.stations.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverpassStationDirectoryTest {
    @Test fun `lee nodos y areas con su centro y descarta los que no tienen ubicacion`() {
        val body = """
            {"elements":[
              {"type":"node","lat":1.21,"lon":-77.28,"tags":{"amenity":"fuel","name":"EDS La 16","brand":"Terpel"}},
              {"type":"way","center":{"lat":1.22,"lon":-77.29},"tags":{"amenity":"fuel","operator":"Primax"}},
              {"type":"way","tags":{"amenity":"fuel","name":"Sin centro"}}
            ]}
        """.trimIndent()
        val places = parseOverpass(body)
        assertEquals(2, places.size)
        assertEquals("EDS La 16", places[0].name)
        assertEquals("Terpel", places[0].brand)
        assertNull(places[1].name)
        assertEquals("Primax", places[1].brand)
        assertEquals(GeoPoint(1.22, -77.29), places[1].location)
    }

    @Test fun `la consulta solo lleva el punto redondeado y el radio`() {
        val query = OverpassStationDirectory.query(GeoPoint(1.213612, -77.281145), 5_000)
        assertTrue(query.contains("around:5000,1.214,-77.281"))
        assertTrue(query.contains("\"amenity\"=\"fuel\""))
    }
}
