package com.tanqueo.domain

import com.tanqueo.domain.calc.FillUpInput
import com.tanqueo.domain.calc.FillUpWarning
import com.tanqueo.domain.model.FuelType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FillUpInputTest {
    @Test fun `avisa de lo que no cabe en el tanque`() {
        org.junit.Assert.assertEquals(FillUpWarning.OVER_CAPACITY, FillUpInput.warning(13.0, 200_000, 12.0))
    }

    @Test fun `avisa de un precio por galon poco creible`() {
        org.junit.Assert.assertEquals(FillUpWarning.ODD_PRICE, FillUpInput.warning(10.0, 16_000, 12.0))
        org.junit.Assert.assertEquals(FillUpWarning.ODD_PRICE, FillUpInput.warning(1.0, 900_000, 12.0))
    }

    @Test fun `un valor aun sin poner no es un precio raro`() {
        org.junit.Assert.assertNull(FillUpInput.warning(10.0, 0, 12.0))
    }

    @Test fun `un tanqueo normal no lleva aviso`() {
        org.junit.Assert.assertNull(FillUpInput.warning(10.0, 165_000, 12.0))
    }

    @Test fun `galones avanzan de a una decima sin error de redondeo`() {
        var value = 10.0
        repeat(3) { value = FillUpInput.adjustGallons(value, 1) }
        assertEquals(10.3, value, 0.0)
    }

    @Test fun `galones respetan los limites`() {
        assertEquals(0.1, FillUpInput.adjustGallons(0.2, -5), 0.0)
        assertEquals(99.9, FillUpInput.adjustGallons(99.8, 5), 0.0)
    }

    @Test fun `el valor pagado va de a cien pesos y nunca llega a cero`() {
        assertEquals(165_000L, FillUpInput.adjustCop(164_900, 1))
        assertEquals(100L, FillUpInput.adjustCop(200, -5))
    }

    @Test fun `el odometro no baja del minimo`() {
        assertEquals(1000, FillUpInput.adjustOdometer(1002, -5, minKm = 1000))
        assertEquals(1007, FillUpInput.adjustOdometer(1002, 5, minKm = 1000))
    }

    @Test fun `umbrales respetan sus pasos y limites`() {
        assertEquals(60, FillUpInput.adjustKmThreshold(50, 1))
        assertEquals(10, FillUpInput.adjustKmThreshold(10, -1))
        assertEquals(1, FillUpInput.adjustDaysThreshold(1, -1))
    }

    @Test fun `precio sugerido sale del ultimo tanqueo del mismo combustible`() {
        val corriente = full(0, 1000, 10.0).copy(paidCop = 160_000)
        val extra = full(5, 1200, 10.0).copy(paidCop = 200_000, fuelType = FuelType.EXTRA)
        val list = listOf(corriente, extra)
        assertEquals(16_000.0, FillUpInput.lastPricePerGallon(list, FuelType.CORRIENTE)!!, 0.01)
        assertEquals(20_000.0, FillUpInput.lastPricePerGallon(list, FuelType.EXTRA)!!, 0.01)
        assertEquals(20_000.0, FillUpInput.lastPricePerGallon(list, FuelType.DIESEL)!!, 0.01)
        assertNull(FillUpInput.lastPricePerGallon(emptyList(), FuelType.DIESEL))
    }

    @Test fun `valor sugerido se redondea a cien pesos`() {
        assertEquals(164_700L, FillUpInput.suggestedPaid(10.2, 16_150.0))
    }

    @Test fun `minimo del odometro es el mayor registrado`() {
        assertEquals(0, FillUpInput.minOdometer(emptyList()))
        assertEquals(1500, FillUpInput.minOdometer(listOf(full(0, 1500), full(1, 1200), full(2, null))))
        assertEquals(1800, FillUpInput.minOdometer(listOf(full(0, 1500)), extraKm = 1800))
    }
}
