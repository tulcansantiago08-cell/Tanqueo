package com.tanqueo.ui

import com.tanqueo.domain.dayN
import com.tanqueo.domain.full
import com.tanqueo.domain.model.FuelType
import com.tanqueo.domain.tip.MonthTip
import com.tanqueo.domain.usecase.ObserveDashboard
import com.tanqueo.domain.usecase.WriteMonthTip
import com.tanqueo.ui.budget.BudgetViewModel
import com.tanqueo.ui.summary.SummaryViewModel
import java.time.Clock
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BudgetAndSummaryViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val history = listOf(full(0, 1000, 10.0), full(10, 1400, 10.0)) // $ 16.000/galón, 40 km/gal

    @Test fun `el primer precio es el ultimo que pago el usuario`() = runTest {
        val vm = BudgetViewModel(FakeFillUpRepository(history), FakeSettingsRepository())
        val state = vm.state.value
        assertNull(state.option!!.place)
        assertEquals(16_000.0, state.option!!.pricePerGallon, 0.01)
        assertEquals(125.0, state.result!!.km!!, 0.01) // $ 50.000 ÷ 16.000 × 40
    }

    @Test fun `sin historia ofrece referencias y no inventa km`() = runTest {
        val state = BudgetViewModel(FakeFillUpRepository(), FakeSettingsRepository()).state.value
        assertEquals("Promedio", state.option!!.place)
        assertNull(state.result!!.km)
    }

    @Test fun `con diesel ofrece precios de diesel`() = runTest {
        val diesel = history.map { it.copy(fuelType = FuelType.DIESEL) }
        val state = BudgetViewModel(FakeFillUpRepository(diesel), FakeSettingsRepository()).state.value
        assertTrue(state.options.any { it.pricePerGallon == 11_320.0 })
        assertTrue(state.options.none { it.pricePerGallon == 15_886.0 })
    }

    @Test fun `elegir precio da la vuelta en ambos sentidos`() = runTest {
        val vm = BudgetViewModel(FakeFillUpRepository(history), FakeSettingsRepository())
        vm.selectPrice(-1)
        assertEquals(vm.state.value.options.size - 1, vm.state.value.selected)
        vm.selectPrice(1)
        assertEquals(0, vm.state.value.selected)
    }

    private fun summary(assistant: FakeFuelAssistant): SummaryViewModel {
        val clock = Clock.fixed(dayN(13.0), ZoneOffset.UTC)
        val fills = FakeFillUpRepository(history)
        val settings = FakeSettingsRepository()
        return SummaryViewModel(ObserveDashboard(fills, settings, clock), fills, WriteMonthTip(assistant, settings), clock)
    }

    @Test fun `sin ia queda el consejo local`() = runTest {
        val tip = summary(FakeFuelAssistant(tip = null)).tip.value!!
        assertEquals(MonthTip.KeepLogging, tip.local)
        assertNull(tip.written)
    }

    @Test fun `la redaccion de la ia reemplaza al consejo local`() = runTest {
        assertEquals("Vas muy bien este mes.", summary(FakeFuelAssistant(tip = "Vas muy bien este mes.")).tip.value!!.written)
    }
}
