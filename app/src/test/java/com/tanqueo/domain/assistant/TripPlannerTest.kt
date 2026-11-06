package com.tanqueo.domain.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripPlannerTest {
    private val pastoMedellin = RoadNetwork.route("Pasto", "Medellín")!!

    private fun plan(km: Int, inTank: Double, waypoints: List<Waypoint> = emptyList(), price: Double? = 16_000.0) =
        TripPlanner.plan(km, waypoints, kmPerGallon = 40.0, capacityGallons = 12.0, gallonsInTank = inTank, pricePerGallon = price)

    @Test fun `si lo que hay alcanza no hay que tanquear`() {
        val result = plan(km = 100, inTank = 10.0)
        assertTrue(result.alreadyEnough)
        assertEquals(0.0, result.gallonsBeforeLeaving, 0.0)
        assertTrue(result.stops.isEmpty())
    }

    @Test fun `una diferencia que no se ve con un decimal cuenta como que alcanza`() {
        assertTrue(plan(km = 100, inTank = 2.72).alreadyEnough) // necesita 2,75
        assertFalse(plan(km = 100, inTank = 2.6).alreadyEnough)
    }

    @Test fun `la reserva cuenta en los galones necesarios`() {
        assertEquals(2.75, plan(km = 100, inTank = 0.0).gallonsNeeded, 0.001) // 100 ÷ 40 × 1,10
    }

    @Test fun `solo pide lo que falta`() {
        val result = plan(km = 400, inTank = 5.0) // necesita 11 gal
        assertEquals(6.0, result.gallonsBeforeLeaving, 0.001)
        assertEquals(96_000.0, result.copBeforeLeaving!!, 1.0)
        assertTrue(result.stops.isEmpty())
    }

    @Test fun `un viaje largo llena el tanque y para en la ultima ciudad que alcanza`() {
        val result = plan(pastoMedellin.km, inTank = 4.0, waypoints = pastoMedellin.waypoints) // 22 gal, tanque de 12
        assertEquals(8.0, result.gallonsBeforeLeaving, 0.001)
        assertEquals(18.0, result.gallonsTotal, 0.001)
        assertEquals(listOf("Cali"), result.stops.map { it.place }) // Con 12 gal llega a 436 km: Cali está en 379.
        assertEquals(379, result.stops.single().atKm)
    }

    @Test fun `el combustible de la parada completa el viaje sin sobrar`() {
        val result = plan(pastoMedellin.km, inTank = 12.0, waypoints = pastoMedellin.waypoints)
        assertEquals(result.gallonsTotal, result.stops.sumOf { it.gallons }, 0.001)
    }

    @Test fun `sin ciudades conocidas dice en que kilometro parar`() {
        val result = plan(km = 1_000, inTank = 12.0)
        assertNull(result.stops.first().place)
        assertEquals(436, result.stops.first().atKm)
        assertFalse(result.stops.isEmpty())
    }

    @Test fun `lo que hay en el tanque nunca pasa de su capacidad ni baja de cero`() {
        assertEquals(12.0, plan(km = 10, inTank = 99.0).gallonsInTank, 0.0)
        assertEquals(0.0, plan(km = 10, inTank = -3.0).gallonsInTank, 0.0)
    }

    @Test fun `sin precio no inventa el costo`() {
        assertNull(plan(km = 400, inTank = 0.0, price = null).copTotal)
        assertNull(plan(km = 400, inTank = 0.0, price = null).copOfTrip)
    }

    @Test fun `el costo del viaje no depende de lo que haya en el tanque`() {
        assertEquals(176_000.0, plan(km = 400, inTank = 12.0).copOfTrip!!, 1.0) // 11 gal × 16.000
        assertEquals(176_000.0, plan(km = 400, inTank = 0.0).copOfTrip!!, 1.0)
    }
}
