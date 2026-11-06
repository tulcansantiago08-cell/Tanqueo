package com.tanqueo.data.ai

import com.tanqueo.domain.model.FuelType
import com.tanqueo.domain.voice.DictatedFillUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DictationDtoTest {
    @Test fun `entiende el viaje que devuelve la ia`() {
        val intent = com.tanqueo.data.ai.parseQuestion("""{"intent":"TRIP","origin":"Pasto","destination":"Medellín","daysUntilTrip":1}""")
        org.junit.Assert.assertEquals(com.tanqueo.domain.assistant.AskIntent.Trip("Pasto", "Medellín", null, 1), intent)
    }

    @Test fun `un valor sin cifra o un texto roto no se toman como respuesta`() {
        org.junit.Assert.assertEquals(com.tanqueo.domain.assistant.AskIntent.Unknown, com.tanqueo.data.ai.parseQuestion("""{"intent":"BUDGET"}"""))
        org.junit.Assert.assertNull(com.tanqueo.data.ai.parseQuestion("no es json"))
    }

    @Test fun `lee la respuesta completa`() {
        val text = """{"gallons":10.5,"paidCop":165000,"fuelType":"DIESEL","isFull":false,"odometerKm":41600}"""
        assertEquals(DictatedFillUp(10.5, 165_000, FuelType.DIESEL, false, 41_600), parseDictation(text))
    }

    @Test fun `lo omitido queda en null y lo desconocido se ignora`() {
        assertEquals(DictatedFillUp(gallons = 3.0), parseDictation("""{"gallons":3,"otra":"cosa"}"""))
    }

    @Test fun `un combustible desconocido no se inventa`() {
        assertNull(parseDictation("""{"fuelType":"GAS"}""")!!.fuelType)
    }

    @Test fun `una respuesta que no es json no rompe`() {
        assertNull(parseDictation("no entendí"))
    }
}
