package com.tanqueo.domain

import com.tanqueo.domain.calc.MonthSummaryCalculator
import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.FuelType
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonthSummaryTest {
    private val bogota = ZoneId.of("America/Bogota")

    private fun fillUp(at: String, gallons: Double, cop: Long) =
        FillUp(0, Instant.parse(at), gallons, cop, FuelType.CORRIENTE, true, null)

    private val now = Instant.parse("2026-11-04T19:00:00Z")

    @Test fun `sin tanqueos todo queda vacio`() {
        val s = MonthSummaryCalculator.calculate(emptyList(), now, bogota)
        assertEquals(0L, s.totalSpentCop)
        assertEquals(0, s.fillUpCount)
        assertNull(s.averagePricePerGallon)
        assertNull(s.daysSinceLastFillUp)
    }

    @Test fun `suma solo los tanqueos del mes y pondera el precio por galon`() {
        val list = listOf(
            fillUp("2026-10-20T15:00:00Z", 10.0, 160_000),
            fillUp("2026-11-01T15:00:00Z", 10.0, 200_000),
            fillUp("2026-11-03T15:00:00Z", 5.0, 75_000),
        )
        val s = MonthSummaryCalculator.calculate(list, now, bogota)
        assertEquals(275_000L, s.totalSpentCop)
        assertEquals(2, s.fillUpCount)
        assertEquals(18_333.33, s.averagePricePerGallon!!, 0.01)
        assertEquals(1L, s.daysSinceLastFillUp)
    }

    @Test fun `el limite del mes se calcula en la zona horaria local`() {
        // 1 nov 03:00 UTC sigue siendo 31 oct 22:00 en Bogotá.
        val list = listOf(fillUp("2026-11-01T03:00:00Z", 10.0, 160_000))
        assertEquals(0, MonthSummaryCalculator.calculate(list, now, bogota).fillUpCount)
    }

    @Test fun `los dias desde el ultimo cuentan aunque sea de otro mes`() {
        val list = listOf(fillUp("2026-10-20T15:00:00Z", 10.0, 160_000))
        val s = MonthSummaryCalculator.calculate(list, now, bogota)
        assertEquals(0, s.fillUpCount)
        assertNull(s.averagePricePerGallon)
        assertEquals(15L, s.daysSinceLastFillUp)
    }
}
