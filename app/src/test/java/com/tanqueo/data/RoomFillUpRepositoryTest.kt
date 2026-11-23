package com.tanqueo.data

import com.tanqueo.domain.model.VehicleType
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tanqueo.data.local.RoomFillUpRepository
import com.tanqueo.data.local.TanqueoDatabase
import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.FuelType
import com.tanqueo.ui.FakeSettingsRepository
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class RoomFillUpRepositoryTest {
    private lateinit var database: TanqueoDatabase
    private lateinit var repository: RoomFillUpRepository
    private val settings = FakeSettingsRepository()

    private fun fillUp(at: String, km: Int? = null, fuel: FuelType = FuelType.CORRIENTE) =
        FillUp(0, Instant.parse(at), 10.5, 170_000, fuel, true, km)

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            TanqueoDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomFillUpRepository(database.fillUpDao(), settings)
    }

    @After fun tearDown() = database.close()

    @Test fun `lista del mas reciente al mas antiguo`() = runTest {
        repository.add(fillUp("2026-10-01T10:00:00Z"))
        repository.add(fillUp("2026-11-01T10:00:00Z"))
        repository.add(fillUp("2026-10-15T10:00:00Z"))
        assertEquals(
            listOf("2026-11-01T10:00:00Z", "2026-10-15T10:00:00Z", "2026-10-01T10:00:00Z"),
            repository.observeAll().first().map { it.at.toString() },
        )
    }

    @Test fun `conserva todos los campos, tambien el km nulo`() = runTest {
        val id = repository.add(fillUp("2026-10-01T10:00:00Z", km = null, fuel = FuelType.DIESEL))
        val saved = repository.observe(id).first()!!
        assertEquals(fillUp("2026-10-01T10:00:00Z", km = null, fuel = FuelType.DIESEL).copy(id = id), saved)
    }

    @Test fun `corregir cambia solo ese tanqueo y conserva su fecha`() = runTest {
        val a = repository.add(fillUp("2026-10-01T10:00:00Z", km = 1000))
        val b = repository.add(fillUp("2026-10-02T10:00:00Z", km = 1300))
        val original = repository.observe(a).first()!!
        repository.update(original.copy(gallons = 9.0, paidCop = 150_000, odometerKm = 1100))
        assertEquals(original.copy(gallons = 9.0, paidCop = 150_000, odometerKm = 1100), repository.observe(a).first())
        assertEquals(1300, repository.observe(b).first()!!.odometerKm)
    }

    @Test fun `corregir un tanqueo borrado falla en vez de pasar en silencio`() = runTest {
        val a = repository.add(fillUp("2026-10-01T10:00:00Z"))
        val original = repository.observe(a).first()!!
        repository.delete(a)
        assertTrue(runCatching { repository.update(original) }.isFailure)
    }

    @Test fun `borrar quita solo ese tanqueo`() = runTest {
        val a = repository.add(fillUp("2026-10-01T10:00:00Z"))
        val b = repository.add(fillUp("2026-10-02T10:00:00Z"))
        repository.delete(a)
        assertNull(repository.observe(a).first())
        assertEquals(listOf(b), repository.observeAll().first().map { it.id })
    }

    @Test fun `los tanqueos del carro y de la moto no se mezclan`() = runTest {
        repository.add(fillUp("2026-10-01T10:00:00Z"))
        repository.add(fillUp("2026-10-02T10:00:00Z").copy(vehicle = VehicleType.MOTO, gallons = 2.5))
        assertEquals(listOf(10.5), repository.observeAll(VehicleType.CARRO).first().map { it.gallons })
        assertEquals(listOf(2.5), repository.observeAll(VehicleType.MOTO).first().map { it.gallons })
        // Sin argumento: los del vehículo activo.
        assertEquals(listOf(VehicleType.CARRO), repository.observeAll().first().map { it.vehicle })
        settings.setVehicleType(VehicleType.MOTO)
        assertEquals(listOf(VehicleType.MOTO), repository.observeAll().first().map { it.vehicle })
    }
}
