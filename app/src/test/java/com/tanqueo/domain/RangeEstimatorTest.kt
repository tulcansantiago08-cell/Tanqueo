package com.tanqueo.domain

import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.calc.RangeEstimator
import com.tanqueo.domain.model.OdometerUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RangeEstimatorTest {
    private val delta = 0.001

    private fun available(estimate: RangeEstimate) = estimate as RangeEstimate.Available

    // --- Rendimiento ---

    @Test fun `rendimiento es km entre dos llenos sobre galones del segundo`() {
        assertEquals(40.0, RangeEstimator.kmPerGallon(listOf(full(0, 1000), full(10, 1400, 10.0)))!!, delta)
    }

    @Test fun `sin dos llenos con km no hay rendimiento`() {
        assertNull(RangeEstimator.kmPerGallon(emptyList()))
        assertNull(RangeEstimator.kmPerGallon(listOf(full(0, 1000))))
        assertNull(RangeEstimator.kmPerGallon(listOf(full(0, 1000), partial(5, 1200))))
        assertNull(RangeEstimator.kmPerGallon(listOf(full(0, null), full(5, null))))
    }

    @Test fun `parciales entre dos llenos suman sus galones al tramo`() {
        val list = listOf(full(0, 1000), partial(5, 1200, 5.0), full(10, 1400, 5.0))
        assertEquals(40.0, RangeEstimator.kmPerGallon(list)!!, delta)
    }

    @Test fun `parcial sin km entre dos llenos tambien cuenta sus galones`() {
        val list = listOf(full(0, 1000), partial(5, null, 2.0), full(10, 1400, 8.0))
        assertEquals(40.0, RangeEstimator.kmPerGallon(list)!!, delta)
    }

    @Test fun `lleno sin km no corta el tramo y sus galones cuentan`() {
        val list = listOf(full(0, 1000), full(5, null, 5.0), full(10, 1400, 5.0))
        assertEquals(40.0, RangeEstimator.kmPerGallon(list)!!, delta)
    }

    @Test fun `promedia los tramos sin importar el orden de la lista`() {
        val list = listOf(full(20, 1700, 10.0), full(0, 1000), full(10, 1400, 10.0))
        assertEquals(35.0, RangeEstimator.kmPerGallon(list)!!, delta)
    }

    @Test fun `solo promedia los ultimos tramos`() {
        // El primer tramo (10 km/gal) queda fuera de los últimos cinco (40 km/gal).
        val list = buildList {
            add(full(0, 0))
            add(full(1, 100))
            (2..6).forEach { add(full(it, 100 + (it - 1) * 400)) }
        }
        assertEquals(40.0, RangeEstimator.kmPerGallon(list)!!, delta)
    }

    @Test fun `odometro que retrocede no genera un tramo`() {
        assertNull(RangeEstimator.kmPerGallon(listOf(full(0, 1000), full(5, 900))))
    }

    // --- Km por día ---

    @Test fun `km por dia usa tambien los parciales`() {
        val list = listOf(full(0, 1000), partial(4, 1200), full(10, 1500))
        assertEquals(50.0, RangeEstimator.kmPerDay(list)!!, delta)
    }

    @Test fun `con menos de un dia de historia no hay km por dia`() {
        val sameDay = listOf(
            full(0, 1000),
            full(0, 1100).copy(at = dayN(0.25), id = 99),
        )
        assertNull(RangeEstimator.kmPerDay(sameDay))
        assertNull(RangeEstimator.kmPerDay(listOf(full(0, 1000))))
    }

    // --- Galones tras el último tanqueo ---

    @Test fun `tras un lleno hay la capacidad del tanque`() {
        assertEquals(12.0, RangeEstimator.gallonsAfterLastFillUp(listOf(full(0, 1000)), 12.0, 40.0, null)!!, delta)
    }

    @Test fun `tras un parcial se descuenta lo consumido y se suma lo cargado`() {
        val list = listOf(full(0, 1000), partial(5, 1200, 3.0))
        // 12 − 200/40 + 3 = 10
        assertEquals(10.0, RangeEstimator.gallonsAfterLastFillUp(list, 12.0, 40.0, null)!!, delta)
    }

    @Test fun `un parcial no puede dejar mas que la capacidad`() {
        val list = listOf(full(0, 1000), partial(1, 1040, 8.0))
        assertEquals(12.0, RangeEstimator.gallonsAfterLastFillUp(list, 12.0, 40.0, null)!!, delta)
    }

    @Test fun `sin ningun lleno no se sabe cuanto hay`() {
        assertNull(RangeEstimator.gallonsAfterLastFillUp(listOf(partial(0, 1000)), 12.0, 40.0, 50.0))
    }

    @Test fun `parcial sin km estima el tramo con el promedio diario`() {
        val list = listOf(full(0, 1000), partial(4, null, 2.0))
        // 12 − (4 días × 50 km)/40 + 2 = 9
        assertEquals(9.0, RangeEstimator.gallonsAfterLastFillUp(list, 12.0, 40.0, 50.0)!!, delta)
        assertNull(RangeEstimator.gallonsAfterLastFillUp(list, 12.0, 40.0, null))
    }

    // --- Estimación completa ---

    @Test fun `sin datos suficientes no estima`() {
        val now = dayN(30.0)
        assertEquals(RangeEstimate.Insufficient, RangeEstimator.estimate(emptyList(), 12.0, null, now))
        assertEquals(RangeEstimate.Insufficient, RangeEstimator.estimate(listOf(full(0, 1000)), 12.0, null, now))
        assertEquals(
            RangeEstimate.Insufficient,
            RangeEstimator.estimate(listOf(full(0, null), full(5, null)), 12.0, null, now),
        )
        assertEquals(
            RangeEstimate.Insufficient,
            RangeEstimator.estimate(listOf(partial(0, 1000), partial(10, 1400)), 12.0, null, now),
        )
    }

    @Test fun `estima con el promedio diario desde el ultimo tanqueo`() {
        val list = listOf(full(0, 1000), full(10, 1400)) // 40 km/gal, 40 km/día
        val result = available(RangeEstimator.estimate(list, 12.0, null, dayN(13.0)))
        assertEquals(12.0, result.gallonsInTank, delta)
        assertEquals(120.0, result.kmSinceLastFillUp, delta)
        assertEquals(360.0, result.kmRemaining, delta) // 12 × 40 − 120
        assertEquals(9.0, result.daysRemaining!!, delta)
        assertFalse(result.usesManualOdometer)
    }

    @Test fun `ultimo tanqueo parcial descuenta del tanque`() {
        val list = listOf(full(0, 1000), full(10, 1400), partial(15, 1600, 3.0)) // 40 km/gal, 40 km/día
        val result = available(RangeEstimator.estimate(list, 12.0, null, dayN(15.0)))
        assertEquals(10.0, result.gallonsInTank, delta) // 12 − 200/40 + 3
        assertEquals(400.0, result.kmRemaining, delta)
    }

    @Test fun `kilometraje actualizado a mano reemplaza al promedio`() {
        val list = listOf(full(0, 1000), full(10, 1400))
        val now = dayN(13.0)
        val result = available(RangeEstimator.estimate(list, 12.0, OdometerUpdate(1700, now), now))
        assertEquals(300.0, result.kmSinceLastFillUp, delta)
        assertEquals(180.0, result.kmRemaining, delta)
        assertTrue(result.usesManualOdometer)
    }

    @Test fun `lectura antigua suma el promedio solo por los dias desde la lectura`() {
        val list = listOf(full(0, 1000), full(10, 1400))
        val update = OdometerUpdate(1500, dayN(12.0))
        val result = available(RangeEstimator.estimate(list, 12.0, update, dayN(14.0)))
        assertEquals(180.0, result.kmSinceLastFillUp, delta) // 100 reales + 2 días × 40
    }

    @Test fun `lectura anterior al ultimo tanqueo se ignora`() {
        val list = listOf(full(0, 1000), full(10, 1400))
        val result = available(RangeEstimator.estimate(list, 12.0, OdometerUpdate(1300, dayN(5.0)), dayN(11.0)))
        assertEquals(40.0, result.kmSinceLastFillUp, delta)
        assertFalse(result.usesManualOdometer)
    }

    @Test fun `lectura menor al odometro del ultimo tanqueo se ignora`() {
        val list = listOf(full(0, 1000), full(10, 1400))
        val result = available(RangeEstimator.estimate(list, 12.0, OdometerUpdate(1200, dayN(11.0)), dayN(11.0)))
        assertFalse(result.usesManualOdometer)
    }

    @Test fun `lectura manual sin promedio diario da km pero no dias`() {
        val list = listOf(
            full(0, 1000),
            full(0, 1400, 10.0).copy(at = dayN(0.25), id = 99),
        )
        val now = dayN(0.5)
        assertEquals(RangeEstimate.Insufficient, RangeEstimator.estimate(list, 12.0, null, now))
        val result = available(RangeEstimator.estimate(list, 12.0, OdometerUpdate(1500, now), now))
        assertEquals(380.0, result.kmRemaining, delta)
        assertNull(result.daysRemaining)
    }

    @Test fun `ultimo tanqueo sin km ignora la lectura manual y usa el promedio`() {
        val list = listOf(full(0, 1000), full(10, 1400), full(20, null))
        val result = available(RangeEstimator.estimate(list, 12.0, OdometerUpdate(1900, dayN(21.0)), dayN(21.0)))
        assertFalse(result.usesManualOdometer)
        assertEquals(40.0, result.kmSinceLastFillUp, delta)
    }

    @Test fun `el porcentaje de tanque baja con los km recorridos`() {
        val list = listOf(full(0, 1000), full(10, 1400)) // 40 km/gal, 40 km/día, 480 km en 12 gal
        assertEquals(1.0, available(RangeEstimator.estimate(list, 12.0, null, dayN(10.0))).tankFraction, delta)
        assertEquals(0.75, available(RangeEstimator.estimate(list, 12.0, null, dayN(13.0))).tankFraction, delta)
        assertEquals(0.0, available(RangeEstimator.estimate(list, 12.0, null, dayN(100.0))).tankFraction, delta)
    }

    @Test fun `el odometro actual es la lectura mas reciente, nunca una estimacion`() {
        assertNull(RangeEstimator.latestOdometer(emptyList(), null))
        assertNull(RangeEstimator.latestOdometer(listOf(full(0, null)), null))
        val list = listOf(full(0, 1000), full(10, 1400), full(12, null))
        assertEquals(1400, RangeEstimator.latestOdometer(list, null))
        assertEquals(1400, RangeEstimator.latestOdometer(list, OdometerUpdate(1200, dayN(5.0))))
        assertEquals(1500, RangeEstimator.latestOdometer(list, OdometerUpdate(1500, dayN(11.0))))
        assertEquals(1500, RangeEstimator.latestOdometer(emptyList(), OdometerUpdate(1500, dayN(11.0))))
    }

    @Test fun `la autonomia nunca es negativa`() {
        val list = listOf(full(0, 1000), full(10, 1400))
        val result = available(RangeEstimator.estimate(list, 12.0, null, dayN(100.0)))
        assertEquals(0.0, result.kmRemaining, delta)
        assertEquals(0.0, result.daysRemaining!!, delta)
    }

    @Test fun `un reloj atrasado no suma km negativos`() {
        val list = listOf(full(0, 1000), full(10, 1400))
        val result = available(RangeEstimator.estimate(list, 12.0, null, dayN(9.0)))
        assertEquals(0.0, result.kmSinceLastFillUp, delta)
    }
}
