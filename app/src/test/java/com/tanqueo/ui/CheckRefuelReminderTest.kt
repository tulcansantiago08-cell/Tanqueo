package com.tanqueo.ui

import com.tanqueo.domain.model.VehicleType
import com.tanqueo.domain.dayN
import com.tanqueo.domain.full
import com.tanqueo.domain.model.OdometerUpdate
import com.tanqueo.domain.model.TankSettings
import com.tanqueo.domain.usecase.CheckRefuelReminder
import com.tanqueo.domain.usecase.ObserveDashboard
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CheckRefuelReminderTest {
    private val history = listOf(full(0, 1000), full(10, 1400)) // 40 km/gal, 40 km/día, tanque 12 gal = 480 km

    private fun check(
        now: Instant,
        settings: TankSettings = TankSettings(),
        update: OdometerUpdate? = null,
        fillUps: List<com.tanqueo.domain.model.FillUp> = history,
    ) = CheckRefuelReminder(
        ObserveDashboard(
            FakeFillUpRepository(fillUps),
            FakeSettingsRepository(settings, update),
            Clock.fixed(now, ZoneOffset.UTC),
        ),
    )

    @Test fun `sin datos no avisa`() = runTest {
        assertNull(check(dayN(30.0), fillUps = emptyList())())
    }

    @Test fun `con tanque suficiente no avisa`() = runTest {
        assertNull(check(dayN(12.0))())
    }

    @Test fun `avisa cuando la estimacion queda bajo el umbral`() = runTest {
        val result = check(dayN(21.0))() // 480 − 11 × 40 = 40 km
        assertNotNull(result)
        assertEquals(40.0, result!!.kmRemaining, 0.01)
    }

    @Test fun `un umbral mayor adelanta el aviso`() = runTest {
        assertNotNull(check(dayN(12.0), TankSettings(reminderKmThreshold = 2000))())
    }

    @Test fun `el kilometraje real tambien dispara el aviso`() = runTest {
        val now = dayN(11.0)
        assertNotNull(check(now, update = OdometerUpdate(1840, now))()) // 480 − 440 = 40 km
    }

    @Test fun `cada vehiculo se revisa con sus propios datos`() = runTest {
        // El carro está casi vacío; la moto acaba de tanquear.
        val moto = listOf(full(0, 100, 3.0, id = 50), full(12, 1_300, 3.0, id = 51)).map { it.copy(vehicle = VehicleType.MOTO) }
        val result = check(dayN(22.0), fillUps = history + moto).allVehicles()
        assertNotNull(result[VehicleType.CARRO])
        assertNull(result[VehicleType.MOTO])
    }
}
