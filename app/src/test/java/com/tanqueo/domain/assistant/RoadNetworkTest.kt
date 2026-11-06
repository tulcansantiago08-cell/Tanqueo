package com.tanqueo.domain.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadNetworkTest {
    @Test fun `pasto a medellin va por carretera y no en linea recta`() {
        val route = RoadNetwork.route("Pasto", "Medellín")!!
        assertEquals(800, route.km)
        assertEquals(listOf("Popayán", "Cali", "Pereira"), route.waypoints.map { it.place })
        assertEquals(240, route.waypoints.first().atKm)
    }

    @Test fun `la distancia es igual en ambos sentidos`() {
        assertEquals(RoadNetwork.route("Cali", "Bogotá")!!.km, RoadNetwork.route("Bogotá", "Cali")!!.km)
    }

    @Test fun `todas las ciudades se conectan entre si`() {
        val places = listOf("Bogotá", "Medellín", "Cali", "Barranquilla", "Cartagena", "Cúcuta", "Pasto", "Ipiales", "Riohacha", "Turbo", "Yopal", "Tumaco", "Florencia")
        for (a in places) for (b in places) if (a != b) assertNotNull("$a → $b", RoadNetwork.route(a, b))
    }

    @Test fun `distancias conocidas quedan cerca de lo publicado`() {
        assertTrue(RoadNetwork.route("Bogotá", "Medellín")!!.km in 399..430)
        assertTrue(RoadNetwork.route("Bogotá", "Cali")!!.km in 440..511)
    }

    @Test fun `un mismo lugar o uno desconocido no tiene ruta`() {
        assertNull(RoadNetwork.route("Pasto", "Pasto"))
        assertNull(RoadNetwork.route("Pasto", "Lima"))
    }

    @Test fun `reconoce nombres sin tildes y apodos`() {
        assertEquals("Medellín", RoadNetwork.resolve("medellin"))
        assertEquals("Medellín", RoadNetwork.resolve("Medallo"))
        assertEquals("Popayán", RoadNetwork.resolve(" POPAYÁN "))
        assertNull(RoadNetwork.resolve("Gotham"))
    }
}
