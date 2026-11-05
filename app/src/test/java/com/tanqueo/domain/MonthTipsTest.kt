package com.tanqueo.domain

import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.FuelType
import com.tanqueo.domain.tip.MonthFacts
import com.tanqueo.domain.tip.MonthTip
import com.tanqueo.domain.tip.MonthTips
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthTipsTest {
    private val now = Instant.parse("2026-11-10T12:00:00Z")

    private fun fillUp(at: String, cop: Long, gallons: Double = 10.0, fuel: FuelType = FuelType.CORRIENTE, km: Int? = null, full: Boolean = true) =
        FillUp(0, Instant.parse(at), gallons, cop, fuel, full, km)

    private fun facts(
        spent: Long = 100_000,
        previous: Long? = null,
        prices: Map<FuelType, Long> = emptyMap(),
        average: Double? = null,
        last: Double? = null,
    ) = MonthFacts(spent, 1, previous, prices, average, last)

    @Test fun `compara con el mismo periodo del mes pasado`() {
        val list = listOf(
            fillUp("2026-11-05T12:00:00Z", 200_000),
            fillUp("2026-10-05T12:00:00Z", 100_000),
            fillUp("2026-10-25T12:00:00Z", 900_000), // después del día 10: no entra
        )
        val facts = MonthTips.facts(list, now, ZoneOffset.UTC)
        assertEquals(200_000L, facts.spentCop)
        assertEquals(100_000L, facts.spentLastMonthSamePeriodCop)
        assertEquals(MonthTip.SpendingUp(100), MonthTips.localTip(facts))
    }

    @Test fun `sin mes pasado no hay comparacion`() {
        val facts = MonthTips.facts(listOf(fillUp("2026-11-05T12:00:00Z", 200_000)), now, ZoneOffset.UTC)
        assertNull(facts.spentLastMonthSamePeriodCop)
        assertEquals(MonthTip.KeepLogging, MonthTips.localTip(facts))
    }

    @Test fun `el ahorro tambien se avisa`() {
        assertEquals(MonthTip.SpendingDown(50), MonthTips.localTip(facts(spent = 50_000, previous = 100_000)))
    }

    @Test fun `cambios pequenos no generan consejo`() {
        assertEquals(MonthTip.KeepLogging, MonthTips.localTip(facts(spent = 105_000, previous = 100_000)))
    }

    @Test fun `una caida de rendimiento tiene prioridad`() {
        val tip = MonthTips.localTip(facts(spent = 200_000, previous = 100_000, average = 40.0, last = 30.0))
        assertEquals(MonthTip.EfficiencyDrop(25), tip)
    }

    @Test fun `avisa cuanto mas cuesta la extra`() {
        val tip = MonthTips.localTip(facts(prices = mapOf(FuelType.EXTRA to 19_800, FuelType.CORRIENTE to 16_200)))
        assertEquals(MonthTip.PremiumCostsMore(3_600), tip)
    }

    @Test fun `el ultimo tramo necesita tres llenos con km`() {
        val two = listOf(fillUp("2026-11-01T12:00:00Z", 1, km = 1000), fillUp("2026-11-05T12:00:00Z", 1, km = 1400))
        assertNull(MonthTips.facts(two, now, ZoneOffset.UTC).lastSegmentKmPerGallon)
        val three = two + fillUp("2026-11-09T12:00:00Z", 1, km = 1700)
        assertEquals(30.0, MonthTips.facts(three, now, ZoneOffset.UTC).lastSegmentKmPerGallon!!, 0.001)
    }

    @Test fun `el texto para la ia no incluye el kilometraje ni fechas`() {
        val text = MonthTips.facts(listOf(fillUp("2026-11-05T12:00:00Z", 200_000, km = 41_580)), now, ZoneOffset.UTC).asPromptText()
        assertFalse(text.contains("41"))
        assertFalse(text.contains("2026"))
    }

    @Test fun `un texto de la ia con cifras inventadas se rechaza`() {
        val facts = facts(spent = 226_300)
        assertTrue(MonthTips.isTrustworthy("Llevas $ 226.300 este mes, vas bien.", facts))
        assertFalse(MonthTips.isTrustworthy("Podrías ahorrar $ 50.000 este mes.", facts))
        assertFalse(MonthTips.isTrustworthy("Gastas 30 % más que otros.", facts))
    }

    @Test fun `un texto vacio o muy largo se rechaza`() {
        assertFalse(MonthTips.isTrustworthy("  ", facts()))
        assertFalse(MonthTips.isTrustworthy("a".repeat(MonthTips.MAX_TIP_LENGTH + 1), facts()))
        assertTrue(MonthTips.isTrustworthy("Tanquea con calma y compara estaciones.", facts()))
    }
}
