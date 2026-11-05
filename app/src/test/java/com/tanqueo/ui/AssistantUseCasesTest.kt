package com.tanqueo.ui

import com.tanqueo.domain.model.FuelType
import com.tanqueo.domain.tip.MonthFacts
import com.tanqueo.domain.usecase.InterpretDictation
import com.tanqueo.domain.usecase.WriteMonthTip
import com.tanqueo.domain.voice.DictatedFillUp
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssistantUseCasesTest {
    private val facts = MonthFacts(226_300, 2, null, emptyMap(), 42.0, null)
    private val today = LocalDate.of(2026, 11, 4)

    @Test fun `sin ia responde el lector local`() = runTest {
        val result = InterpretDictation(FakeFuelAssistant(dictation = null))("10 galones 165 mil corriente", 0)
        assertEquals(10.0, result.gallons!!, 0.0)
        assertEquals(165_000L, result.paidCop)
        assertEquals(FuelType.CORRIENTE, result.fuelType)
    }

    @Test fun `la ia manda y el lector local completa lo que ella omita`() = runTest {
        val ai = FakeFuelAssistant(dictation = DictatedFillUp(gallons = 12.0))
        val result = InterpretDictation(ai)("10 galones 165 mil", 0)
        assertEquals(12.0, result.gallons!!, 0.0)
        assertEquals(165_000L, result.paidCop)
    }

    @Test fun `un valor imposible de la ia se descarta y lo cubre el lector local`() = runTest {
        val ai = FakeFuelAssistant(dictation = DictatedFillUp(gallons = 900.0, odometerKm = 5))
        val result = InterpretDictation(ai)("10 galones", 1000)
        assertEquals(10.0, result.gallons!!, 0.0)
        assertNull(result.odometerKm)
    }

    @Test fun `el consejo aceptado se guarda y no vuelve a gastar cuota`() = runTest {
        val ai = FakeFuelAssistant(tip = "Llevas $ 226.300 este mes.")
        val settings = FakeSettingsRepository()
        val write = WriteMonthTip(ai, settings)
        assertEquals("Llevas $ 226.300 este mes.", write(facts, today))
        assertEquals("Llevas $ 226.300 este mes.", write(facts, today))
        assertEquals(1, ai.tipCalls)
    }

    @Test fun `con otro dia o datos distintos se pide de nuevo`() = runTest {
        val ai = FakeFuelAssistant(tip = "Vas bien este mes.")
        val write = WriteMonthTip(ai, FakeSettingsRepository())
        write(facts, today)
        write(facts, today.plusDays(1))
        write(facts.copy(spentCop = 1), today.plusDays(1))
        assertEquals(3, ai.tipCalls)
    }

    @Test fun `un consejo con cifras inventadas no se muestra ni se guarda`() = runTest {
        val settings = FakeSettingsRepository()
        assertNull(WriteMonthTip(FakeFuelAssistant(tip = "Ahorra $ 50.000."), settings)(facts, today))
        assertNull(settings.tip)
    }

    @Test fun `sin ia no hay consejo redactado`() = runTest {
        assertNull(WriteMonthTip(FakeFuelAssistant(tip = null), FakeSettingsRepository())(facts, today))
    }
}
