package com.tanqueo.data.settings

import java.time.Instant
import com.tanqueo.domain.model.OdometerUpdate
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.tanqueo.domain.model.VehicleType
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private fun TestScope.repository(): DataStoreSettingsRepository {
        val file = File(folder.root, "ajustes_${System.nanoTime()}.preferences_pb")
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        return DataStoreSettingsRepository(store)
    }

    @Test fun `al principio no hay vehiculo elegido`() = runTest(UnconfinedTestDispatcher()) {
        assertNull(repository().settings.first().vehicleType)
    }

    @Test fun `elegir moto la primera vez pone su capacidad`() = runTest(UnconfinedTestDispatcher()) {
        val repo = repository()
        repo.setVehicleType(VehicleType.MOTO)
        val settings = repo.settings.first()
        assertEquals(VehicleType.MOTO, settings.vehicleType)
        assertEquals(3.0, settings.tankCapacityGallons, 0.0)
    }

    @Test fun `la primera eleccion respeta una capacidad ya ajustada`() = runTest(UnconfinedTestDispatcher()) {
        val repo = repository()
        repo.setTankCapacity(14.5)
        repo.setVehicleType(VehicleType.CARRO)
        assertEquals(14.5, repo.settings.first().tankCapacityGallons, 0.0)
    }

    @Test fun `cambiar de vehiculo pone la capacidad del nuevo`() = runTest(UnconfinedTestDispatcher()) {
        val repo = repository()
        repo.setVehicleType(VehicleType.CARRO)
        repo.setTankCapacity(14.5)
        repo.setVehicleType(VehicleType.MOTO)
        assertEquals(3.0, repo.settings.first().tankCapacityGallons, 0.0)
        repo.setVehicleType(VehicleType.MOTO)
        assertEquals(3.0, repo.settings.first().tankCapacityGallons, 0.0)
    }

    @Test fun `al volver al carro conserva lo suyo`() = runTest(UnconfinedTestDispatcher()) {
        val repo = repository()
        repo.setVehicleType(VehicleType.CARRO)
        repo.setTankCapacity(14.5)
        repo.setOdometerUpdate(OdometerUpdate(41_000, Instant.parse("2026-11-01T10:00:00Z")))
        repo.setVehicleType(VehicleType.MOTO)
        repo.setTankCapacity(3.5)
        assertNull(repo.odometerUpdate.first()) // la moto no hereda el km del carro
        repo.setVehicleType(VehicleType.CARRO)
        assertEquals(14.5, repo.settings.first().tankCapacityGallons, 0.0)
        assertEquals(41_000, repo.odometerUpdate.first()?.km)
        assertEquals(3.5, repo.settingsFor(VehicleType.MOTO).first().tankCapacityGallons, 0.0)
    }
}
