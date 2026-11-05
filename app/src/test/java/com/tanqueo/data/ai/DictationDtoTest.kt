package com.tanqueo.data.ai

import com.tanqueo.domain.model.FuelType
import com.tanqueo.domain.voice.DictatedFillUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DictationDtoTest {
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
