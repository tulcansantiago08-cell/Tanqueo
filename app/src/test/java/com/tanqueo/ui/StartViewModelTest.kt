package com.tanqueo.ui

import app.cash.turbine.test
import com.tanqueo.domain.model.TankSettings
import com.tanqueo.domain.model.VehicleType
import com.tanqueo.ui.welcome.StartState
import com.tanqueo.ui.welcome.StartViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StartViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    @Test fun `sin vehiculo pide elegir y al elegir moto abre la app`() = runTest {
        val repo = FakeSettingsRepository()
        val refresher = CountingRefresher()
        val vm = StartViewModel(repo, refresher)
        vm.state.test {
            assertEquals(StartState.CHOOSE_VEHICLE, expectMostRecentItem())
            vm.choose(VehicleType.MOTO)
            assertEquals(StartState.READY, awaitItem())
        }
        assertEquals(VehicleType.MOTO, repo.settings.value.vehicleType)
        assertEquals(3.0, repo.settings.value.tankCapacityGallons, 0.0)
        assertEquals(1, refresher.count)
    }

    @Test fun `con vehiculo ya elegido va directo a la app`() = runTest {
        val vm = StartViewModel(FakeSettingsRepository(TankSettings(vehicleType = VehicleType.CARRO)), CountingRefresher())
        vm.state.test {
            assertEquals(StartState.READY, expectMostRecentItem())
        }
    }
}
