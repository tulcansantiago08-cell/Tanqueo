package com.tanqueo.domain

import com.tanqueo.domain.voice.SpanishNumbers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpanishNumbersTest {
    @Test fun `lee un numero como se escriba`() {
        assertEquals(41_600.0, SpanishNumbers.parse("41.600")!!, 0.0)
        assertEquals(10.5, SpanishNumbers.parse("10,5")!!, 0.0)
        assertEquals(165_000.0, SpanishNumbers.parse("165 mil")!!, 0.0)
        assertEquals(80_000.0, SpanishNumbers.parse("80 lucas")!!, 0.0)
        assertEquals(1_000.0, SpanishNumbers.parse("una luca")!!, 0.0)
        assertEquals(110.0, SpanishNumbers.parse("ciento diez")!!, 0.0)
    }

    @Test fun `sin un unico numero no adivina`() {
        assertNull(SpanishNumbers.parse("hola"))
        assertNull(SpanishNumbers.parse("10 y 20"))
    }

    @Test fun `detecta si una cifra se dijo de verdad`() {
        assertTrue(SpanishNumbers.mentions("voy a manejar 300 km", 300.0))
        assertTrue(SpanishNumbers.mentions("con ochenta mil", 80_000.0))
        assertFalse(SpanishNumbers.mentions("voy de Pasto a Medellín", 800.0))
    }
}
