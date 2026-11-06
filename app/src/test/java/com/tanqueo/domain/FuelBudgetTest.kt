package com.tanqueo.domain

import com.tanqueo.domain.budget.FuelBudgetCalculator
import com.tanqueo.domain.budget.ReferencePrices
import com.tanqueo.domain.model.FuelType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuelBudgetTest {
    @Test fun `calcula galones y km con el rendimiento real`() {
        val result = FuelBudgetCalculator.calculate(100_000, 15_886.0, 42.0, 12.0)!!
        assertEquals(6.295, result.gallons, 0.001)
        assertEquals(264.4, result.km!!, 0.1)
        assertFalse(result.exceedsTank)
    }

    @Test fun `sin rendimiento conocido da galones pero no inventa km`() {
        val result = FuelBudgetCalculator.calculate(100_000, 15_886.0, null, 12.0)!!
        assertEquals(6.295, result.gallons, 0.001)
        assertNull(result.km)
    }

    @Test fun `lo que no cabe en el tanque no suma km`() {
        val result = FuelBudgetCalculator.calculate(300_000, 15_000.0, 42.0, 12.0)!!
        assertEquals(20.0, result.gallons, 0.001)
        assertEquals(504.0, result.km!!, 0.001)
        assertTrue(result.exceedsTank)
    }

    @Test fun `los dias salen de los km que se manejan a diario`() {
        val result = FuelBudgetCalculator.calculate(100_000, 15_886.0, 42.0, 12.0, kmPerDay = 50.0)!!
        assertEquals(5.29, result.days!!, 0.01)
    }

    @Test fun `sin promedio diario o sin rendimiento no inventa dias`() {
        assertNull(FuelBudgetCalculator.calculate(100_000, 15_886.0, 42.0, 12.0, kmPerDay = null)!!.days)
        assertNull(FuelBudgetCalculator.calculate(100_000, 15_886.0, 42.0, 12.0, kmPerDay = 0.0)!!.days)
        assertNull(FuelBudgetCalculator.calculate(100_000, 15_886.0, null, 12.0, kmPerDay = 50.0)!!.days)
    }

    @Test fun `entiende el valor como lo escriba el usuario`() {
        listOf("50000", "50.000", "$ 50.000", "50 mil", "cincuenta mil", "50000 pesos").forEach {
            assertEquals(it, 50_000L, FuelBudgetCalculator.parseMoney(it))
        }
    }

    @Test fun `rechaza lo que no es un valor posible`() {
        listOf("", "hola", "50", "999", "3000000").forEach {
            assertNull(it, FuelBudgetCalculator.parseMoney(it))
        }
    }

    @Test fun `precio invalido no da resultado`() {
        assertNull(FuelBudgetCalculator.calculate(100_000, 0.0, 42.0, 12.0))
    }

    @Test fun `el dinero respeta los limites`() {
        assertEquals(51_000L, FuelBudgetCalculator.adjustMoney(50_000, 1))
        assertEquals(FuelBudgetCalculator.MIN_MONEY_COP, FuelBudgetCalculator.adjustMoney(1_000, -5))
        assertEquals(FuelBudgetCalculator.MAX_MONEY_COP, FuelBudgetCalculator.adjustMoney(2_000_000, 5))
    }

    @Test fun `los precios de referencia corresponden al combustible`() {
        assertTrue(ReferencePrices.forFuel(FuelType.DIESEL).all { it.fuelType == FuelType.DIESEL })
        assertTrue(ReferencePrices.forFuel(FuelType.CORRIENTE).all { it.fuelType == FuelType.CORRIENTE })
        assertTrue(ReferencePrices.forFuel(FuelType.EXTRA).all { it.fuelType == FuelType.CORRIENTE })
        assertEquals(13_920L, ReferencePrices.forFuel(FuelType.CORRIENTE).first { it.place == "Pasto" }.pricePerGallon)
    }
}
