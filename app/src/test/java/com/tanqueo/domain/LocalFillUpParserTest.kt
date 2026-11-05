package com.tanqueo.domain

import com.tanqueo.domain.model.FuelType
import com.tanqueo.domain.voice.DictatedFillUp
import com.tanqueo.domain.voice.LocalFillUpParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFillUpParserTest {
    @Test fun `frase completa con cifras`() {
        assertEquals(
            DictatedFillUp(10.0, 165_000, FuelType.CORRIENTE, true, null),
            LocalFillUpParser.parse("Tanqueé 10 galones, 165 mil, corriente, lleno"),
        )
    }

    @Test fun `numeros en palabras`() {
        val result = LocalFillUpParser.parse("diez galones ciento sesenta y cinco mil pesos extra")
        assertEquals(10.0, result.gallons!!, 0.0)
        assertEquals(165_000L, result.paidCop)
        assertEquals(FuelType.EXTRA, result.fuelType)
    }

    @Test fun `frase larga dictada en palabras`() {
        assertEquals(
            DictatedFillUp(12.0, 180_000, FuelType.CORRIENTE, true, 42_000),
            LocalFillUpParser.parse("Doce galones ciento ochenta mil pesos corriente lleno cuarenta y dos mil km"),
        )
    }

    @Test fun `decimales y medios galones`() {
        assertEquals(10.5, LocalFillUpParser.parse("10,5 galones").gallons!!, 0.0)
        assertEquals(10.5, LocalFillUpParser.parse("10.5 galones").gallons!!, 0.0)
        assertEquals(10.5, LocalFillUpParser.parse("10 galones y medio").gallons!!, 0.0)
        assertEquals(10.5, LocalFillUpParser.parse("diez y medio galones").gallons!!, 0.0)
        assertEquals(0.5, LocalFillUpParser.parse("medio galón").gallons!!, 0.0)
    }

    @Test fun `formatos de dinero`() {
        assertEquals(165_000L, LocalFillUpParser.parse("$165.000").paidCop)
        assertEquals(165_000L, LocalFillUpParser.parse("165,000 pesos").paidCop)
        assertEquals(165_500L, LocalFillUpParser.parse("165 mil 500 pesos").paidCop)
        assertEquals(165_000L, LocalFillUpParser.parse("pagué 165000").paidCop)
        assertEquals(1_000L, LocalFillUpParser.parse("mil pesos").paidCop)
    }

    @Test fun `el kilometraje no se confunde con el dinero`() {
        val result = LocalFillUpParser.parse("165 mil pesos 10 galones 41 mil 600 km")
        assertEquals(165_000L, result.paidCop)
        assertEquals(41_600, result.odometerKm)
        assertEquals(10.0, result.gallons!!, 0.0)
        assertEquals(41_600, LocalFillUpParser.parse("kilometraje 41.600").odometerKm)
    }

    @Test fun `sin unidades no se adivinan galones`() {
        val result = LocalFillUpParser.parse("165 mil 10")
        assertEquals(165_000L, result.paidCop)
        assertNull(result.gallons)
    }

    @Test fun `combustible y tipo de tanque`() {
        assertEquals(FuelType.DIESEL, LocalFillUpParser.parse("le puse ACPM").fuelType)
        assertEquals(FuelType.DIESEL, LocalFillUpParser.parse("diésel").fuelType)
        assertEquals(false, LocalFillUpParser.parse("tanqueo parcial").isFull)
        assertEquals(false, LocalFillUpParser.parse("no lleno").isFull)
        assertEquals(true, LocalFillUpParser.parse("full").isFull)
    }

    @Test fun `un articulo no es una cantidad`() {
        assertTrue(LocalFillUpParser.parse("hice un tanqueo").isEmpty)
    }

    @Test fun `una frase sin datos queda vacia`() {
        assertTrue(LocalFillUpParser.parse("hola qué tal").isEmpty)
        assertTrue(LocalFillUpParser.parse("").isEmpty)
    }

    @Test fun `sanitizar descarta valores imposibles sin corregirlos`() {
        val clean = DictatedFillUp(gallons = 500.0, paidCop = 5, odometerKm = 900).sanitized(minOdometerKm = 1000)
        assertNull(clean.gallons)
        assertNull(clean.paidCop)
        assertNull(clean.odometerKm)
    }

    @Test fun `sanitizar conserva lo valido y redondea a una decima`() {
        val clean = DictatedFillUp(gallons = 10.27, paidCop = 165_000, odometerKm = 1500).sanitized(minOdometerKm = 1000)
        assertEquals(10.3, clean.gallons!!, 0.0)
        assertEquals(165_000L, clean.paidCop)
        assertEquals(1500, clean.odometerKm)
    }

    @Test fun `orElse solo completa lo que falta`() {
        val merged = DictatedFillUp(gallons = 10.0).orElse(DictatedFillUp(gallons = 5.0, paidCop = 100_000))
        assertEquals(10.0, merged.gallons!!, 0.0)
        assertEquals(100_000L, merged.paidCop)
    }
}
