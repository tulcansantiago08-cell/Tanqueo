package com.tanqueo.ui

import com.tanqueo.domain.assistant.AskIntent
import com.tanqueo.domain.dayN
import com.tanqueo.domain.full
import com.tanqueo.domain.usecase.AnswerQuestion
import com.tanqueo.domain.usecase.AskAnswer
import com.tanqueo.domain.usecase.ObserveDashboard
import java.time.Clock
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerQuestionTest {
    private val clock = Clock.fixed(dayN(10.0), ZoneOffset.UTC)
    private val history = listOf(full(0, 1000, 10.0), full(10, 1400, 10.0)) // 40 km/gal, 40 km/día, $ 16.000/gal

    private fun answer(ai: AskIntent? = null, fills: List<com.tanqueo.domain.model.FillUp> = history): AnswerQuestion {
        val repo = FakeFillUpRepository(fills)
        return AnswerQuestion(FakeFuelAssistant(question = ai), ObserveDashboard(repo, FakeSettingsRepository(), clock), repo, clock)
    }

    @Test fun `sin ia responde el viaje con el lector local`() = runTest {
        val result = answer()("Mañana me voy de Pasto a Medellín, ¿cuánto debo tanquear?") as AskAnswer.Trip
        assertEquals("Pasto", result.origin)
        assertEquals("Medellín", result.destination)
        assertEquals(800, result.plan.km)
        assertEquals(1, result.daysUntil)
        assertEquals("Pasto", result.priceReference)
        assertEquals(13_920.0, result.plan.pricePerGallon!!, 0.01)
        assertTrue(result.plan.stops.isNotEmpty())
    }

    @Test fun `la ia entiende y el reloj calcula`() = runTest {
        val result = answer(AskIntent.Trip("Bogotá", "Tunja"))("me voy pa Tunja saliendo de la capital") as AskAnswer.Trip
        assertEquals(140, result.plan.km)
    }

    @Test fun `una distancia que no se dijo se descarta`() = runTest {
        val result = answer(AskIntent.Trip("Pasto", "Medellín", distanceKm = 123))("voy de Pasto a Medellín") as AskAnswer.Trip
        assertEquals(800, result.plan.km)
    }

    @Test fun `un valor que no se dijo se descarta`() = runTest {
        val result = answer(AskIntent.FuelRange(moneyCop = 999_000))("¿cuánto me queda?")
        assertTrue(result is AskAnswer.Range)
    }

    @Test fun `si la ia no entiende queda el lector local`() = runTest {
        assertTrue(answer(AskIntent.Unknown)("¿cuánta gasolina me queda?") is AskAnswer.Range)
    }

    @Test fun `un lugar desconocido se dice en vez de inventar la distancia`() = runTest {
        assertEquals(AskAnswer.UnknownPlace("Gotham"), answer(AskIntent.Trip("Pasto", "Gotham"))("voy de Pasto a Gotham"))
    }

    @Test fun `un nombre raro de la ia no le gana a uno valido`() = runTest {
        val result = answer(AskIntent.Trip("Bogotá D.C.", "Tunja"))("voy de Bogotá a Tunja") as AskAnswer.Trip
        assertEquals("Bogotá", result.origin)
        assertEquals(140, result.plan.km)
    }

    @Test fun `un nombre vacio de la ia no cuenta`() = runTest {
        val result = answer(AskIntent.Trip("", "Tunja"))("voy de Bogotá a Tunja") as AskAnswer.Trip
        assertEquals("Bogotá", result.origin)
    }

    @Test fun `si dijo los km un lugar desconocido no impide responder`() = runTest {
        val result = answer(AskIntent.Trip("Sandoná", "Pasto", distanceKm = 50))("voy de Sandoná a Pasto, 50 km") as AskAnswer.Trip
        assertEquals(50, result.plan.km)
    }

    @Test fun `una fecha lejana o imposible de la ia se descarta`() = runTest {
        val far = answer(AskIntent.Trip("Pasto", "Cali", daysUntil = 30))("voy de Pasto a Cali el próximo mes") as AskAnswer.Trip
        val negative = answer(AskIntent.Trip("Pasto", "Cali", daysUntil = -5))("voy de Pasto a Cali") as AskAnswer.Trip
        assertNull(far.daysUntil)
        assertNull(negative.daysUntil)
    }

    @Test fun `el promedio no se presenta como precio propio`() = runTest {
        val result = answer(fills = emptyList())("voy de Tunja a Bogotá") as AskAnswer.Trip
        assertEquals("Promedio", result.priceReference)
    }

    @Test fun `si falta el origen lo pide`() = runTest {
        assertEquals(AskAnswer.TripMissingPlace(null, "Cali"), answer()("quiero ir a Cali"))
    }

    @Test fun `sin historial usa la referencia y lo dice`() = runTest {
        val result = answer(fills = emptyList())("voy de Pasto a Medellín") as AskAnswer.Trip
        assertTrue(result.efficiency.isReference)
        assertEquals(35.0, result.efficiency.kmPerGallon, 0.0)
        assertTrue(!result.tankKnown)
        assertEquals(25.14, result.plan.gallonsNeeded, 0.01) // 800 ÷ 35 × 1,10
        assertEquals(13_920.0, result.plan.pricePerGallon!!, 0.01)
    }

    @Test fun `con historial manda el consumo real`() = runTest {
        val result = answer()("voy de Pasto a Medellín") as AskAnswer.Trip
        assertTrue(!result.efficiency.isReference)
        assertEquals(40.0, result.efficiency.kmPerGallon, 0.01)
        assertTrue(result.tankKnown)
    }

    @Test fun `el costo del viaje sale en pesos aunque haya gasolina en el tanque`() = runTest {
        val result = answer()("voy de Tunja a Bogotá") as AskAnswer.Trip
        assertEquals(result.plan.gallonsNeeded * result.plan.pricePerGallon!!, result.plan.copOfTrip!!, 0.01)
        assertTrue(result.plan.copOfTrip!! > 0)
    }

    @Test fun `un valor da km y dias con el ultimo precio`() = runTest {
        val result = answer()("¿hasta dónde me alcanza con 80 mil pesos?") as AskAnswer.FuelRange
        assertEquals(5.0, result.gallons, 0.01)
        assertEquals(200.0, result.km, 0.01)
        assertEquals(5.0, result.days!!, 0.01)
    }

    @Test fun `galones echados dan km y lo que cuestan`() = runTest {
        val result = answer()("Eché 5 galones, ¿cuántos kilómetros puedo recorrer?") as AskAnswer.FuelRange
        assertEquals(200.0, result.km, 0.01)
        assertEquals(80_000.0, result.cop!!, 0.01)
    }

    @Test fun `lo que no cabe en el tanque no suma km`() = runTest {
        val result = answer()("eché 30 galones, cuantos km") as AskAnswer.FuelRange
        assertTrue(result.exceedsTank)
        assertEquals(480.0, result.km, 0.01) // 12 gal × 40
    }

    @Test fun `unos galones que no se dijeron se descartan`() = runTest {
        assertTrue(answer(AskIntent.FuelRange(gallons = 9.0))("¿cuánta gasolina me queda?") is AskAnswer.Range)
    }

    @Test fun `dice el precio de referencia y el mas barato`() = runTest {
        val result = answer()("¿cuánto cuesta el galón en Pasto?") as AskAnswer.Price
        assertEquals(13_920L, result.price!!.pricePerGallon)
        assertEquals("Pasto", result.cheapest!!.place)
    }

    @Test fun `una ciudad sin precio publicado no inventa uno`() = runTest {
        assertNull((answer()("precio en Tunja") as AskAnswer.Price).price)
    }

    @Test fun `contar un tanqueo pasa el texto al registro`() = runTest {
        assertEquals(AskAnswer.LogFillUp("tanqueé 10 galones por 165 mil"), answer()("tanqueé 10 galones por 165 mil"))
    }

    @Test fun `lo que no entiende lo admite`() = runTest {
        assertEquals(AskAnswer.NotUnderstood, answer()("¿va a llover?"))
    }
}
