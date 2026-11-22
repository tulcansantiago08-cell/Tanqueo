package com.tanqueo.ui

import androidx.lifecycle.SavedStateHandle
import com.tanqueo.domain.dayN
import com.tanqueo.domain.full
import com.tanqueo.domain.model.OdometerUpdate
import com.tanqueo.domain.model.TankSettings
import com.tanqueo.domain.model.VehicleType
import com.tanqueo.ui.history.FillUpDetailViewModel
import com.tanqueo.ui.odometer.UpdateKmViewModel
import com.tanqueo.ui.settings.SettingsViewModel
import java.time.Clock
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsAndKmViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val now = dayN(13.0)
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val refresher = CountingRefresher()
    private val history = listOf(full(0, 1000), full(10, 1400))

    @Test fun `los ajustes se guardan y cada paso parte del anterior`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo, refresher)
        repeat(3) { vm.adjustCapacity(10) }
        vm.adjustKmThreshold(5)
        vm.adjustDaysThreshold(2)
        assertEquals(15.0, repo.settings.value.tankCapacityGallons, 0.0)
        assertEquals(100, repo.settings.value.reminderKmThreshold)
        assertEquals(3, repo.settings.value.reminderDaysThreshold)
        assertEquals(repo.settings.value, vm.settings.value)
        assertEquals(5, refresher.count)
    }

    @Test fun `actualizar km sugiere lo estimado y guarda lectura con la hora actual`() = runTest {
        val settings = FakeSettingsRepository()
        val vm = UpdateKmViewModel(FakeFillUpRepository(history), settings, refresher, clock)
        assertNull(vm.state.value.km)
        vm.adjust(1)
        assertEquals(1520, vm.state.value.km) // 1400 + 3 días × 40 km
        vm.adjust(30)
        vm.save()
        assertEquals(OdometerUpdate(1550, now), settings.odometerUpdate.value)
        assertTrue(vm.state.value.isSaved)
        assertEquals(1, refresher.count)
    }

    @Test fun `actualizar km no baja del ultimo tanqueo`() = runTest {
        val vm = UpdateKmViewModel(FakeFillUpRepository(history), FakeSettingsRepository(), refresher, clock)
        vm.adjust(1)
        vm.adjust(-5_000)
        assertEquals(1400, vm.state.value.km)
    }

    @Test fun `el km escrito se puede guardar y uno imposible no`() = runTest {
        val settings = FakeSettingsRepository()
        val vm = UpdateKmViewModel(FakeFillUpRepository(history), settings, refresher, clock)
        assertTrue(!vm.type("900")) // menor que el último tanqueo
        assertNull(vm.state.value.km)
        assertTrue(vm.type("1.555"))
        vm.save()
        assertEquals(OdometerUpdate(1555, now), settings.odometerUpdate.value)
    }

    @Test fun `una estimacion sin tocar no se guarda como lectura real`() = runTest {
        val settings = FakeSettingsRepository()
        val vm = UpdateKmViewModel(FakeFillUpRepository(history), settings, refresher, clock)
        vm.save()
        assertNull(settings.odometerUpdate.value)
        assertTrue(!vm.state.value.isSaved)
    }

    @Test fun `borrar desde el detalle quita el tanqueo y actualiza la tile`() = runTest {
        val repo = FakeFillUpRepository(history)
        val vm = FillUpDetailViewModel(SavedStateHandle(mapOf("id" to 10L)), repo, refresher)
        vm.delete()
        assertNull(repo.items.value.firstOrNull { it.id == 10L })
        assertTrue(vm.isDeleted.value)
        assertEquals(1, refresher.count)
    }

    @Test fun `cambiar a moto en ajustes pone la capacidad de una moto`() = runTest {
        val repo = FakeSettingsRepository(TankSettings(vehicleType = VehicleType.CARRO))
        val vm = SettingsViewModel(repo, refresher)
        vm.adjustVehicle(1)
        assertEquals(VehicleType.MOTO, repo.settings.value.vehicleType)
        assertEquals(3.0, repo.settings.value.tankCapacityGallons, 0.0)
        assertEquals(repo.settings.value, vm.settings.value)

        vm.adjustVehicle(1)
        assertEquals(VehicleType.CARRO, repo.settings.value.vehicleType)
        assertEquals(12.0, repo.settings.value.tankCapacityGallons, 0.0)
    }

    @Test fun `cada vehiculo conserva su propia capacidad`() = runTest {
        val repo = FakeSettingsRepository(TankSettings(vehicleType = VehicleType.CARRO))
        val vm = SettingsViewModel(repo, refresher)
        vm.adjustCapacity(25) // carro: 14,5
        vm.adjustVehicle(1)
        vm.adjustCapacity(5) // moto: 3,5
        vm.adjustVehicle(1)
        assertEquals(VehicleType.CARRO, repo.settings.value.vehicleType)
        assertEquals(14.5, repo.settings.value.tankCapacityGallons, 0.001)
        vm.adjustVehicle(1)
        assertEquals(3.5, repo.settings.value.tankCapacityGallons, 0.001)
    }
}
