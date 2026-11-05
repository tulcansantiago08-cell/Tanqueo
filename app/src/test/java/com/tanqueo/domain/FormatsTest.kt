package com.tanqueo.domain

import com.tanqueo.domain.format.DaysDisplay
import com.tanqueo.domain.format.Formats
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatsTest {
    @Test fun `pesos colombianos con punto de miles y sin decimales`() {
        assertEquals("$\u00A0164.700", Formats.cop(164_700L))
        assertEquals("$\u00A01.250.000", Formats.cop(1_250_000L))
        assertEquals("$\u00A00", Formats.cop(0L))
        assertEquals("$\u00A016.151", Formats.cop(16_150.6))
    }

    @Test fun `decimales con coma`() {
        assertEquals("10,5", Formats.oneDecimal(10.5))
        assertEquals("42,0", Formats.oneDecimal(41.96))
    }

    @Test fun `kilometros con punto de miles`() {
        assertEquals("41.580", Formats.integer(41_580))
        assertEquals("578", Formats.roundedKm(577.6))
    }

    @Test fun `hora de veinticuatro horas`() {
        assertEquals("19:07", Formats.time(LocalDateTime.of(2026, 11, 4, 19, 7)))
        assertEquals("07:05", Formats.time(LocalDateTime.of(2026, 11, 4, 7, 5)))
    }

    @Test fun `fecha corta en espanol`() {
        assertEquals("mié 4 nov", Formats.shortDate(LocalDate.of(2026, 11, 4)))
        assertEquals("lun 2 mar", Formats.shortDate(LocalDate.of(2026, 3, 2)))
    }

    @Test fun `fecha del encabezado en mayusculas y con tilde`() {
        assertEquals("MIÉ 4", Formats.headerDate(LocalDate.of(2026, 11, 4)))
        assertEquals("LUN 2", Formats.headerDate(LocalDate.of(2026, 3, 2)))
    }

    @Test fun `dias sin mostrar nunca cero`() {
        assertEquals(DaysDisplay.LessThanOne, Formats.days(0.0))
        assertEquals(DaysDisplay.LessThanOne, Formats.days(0.99))
        assertEquals(DaysDisplay.About(1), Formats.days(1.2))
        assertEquals(DaysDisplay.About(9), Formats.days(8.6))
    }
}
