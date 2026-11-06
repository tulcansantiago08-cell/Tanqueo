package com.tanqueo.domain

import com.tanqueo.domain.calc.RangeEstimator
import com.tanqueo.domain.format.Formats
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RefuelDateTest {
    private val today = LocalDate.of(2026, 11, 6)

    @Test fun `cuenta solo los dias completos que alcanza`() {
        assertEquals(LocalDate.of(2026, 11, 15), RangeEstimator.refuelDate(today, 9.8))
    }

    @Test fun `con menos de un dia toca hoy`() {
        assertEquals(today, RangeEstimator.refuelDate(today, 0.4))
    }

    @Test fun `pasa bien de un mes a otro`() {
        assertEquals(LocalDate.of(2026, 12, 2), RangeEstimator.refuelDate(today, 26.0))
    }

    @Test fun `la fecha corta no lleva mes ni puntos`() {
        assertEquals("sáb 14", Formats.dayDate(LocalDate.of(2026, 11, 14)))
    }
}
