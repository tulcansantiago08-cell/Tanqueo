package com.tanqueo.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tanqueo.data.local.RoomFillUpRepository
import com.tanqueo.data.local.TanqueoDatabase
import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.FuelType
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private fun fillUp(at: String, km: Int? = null, fuel: FuelType = FuelType.CORRIENTE) =
        FillUp(0, Instant.parse(at), 10.5, 170_000, fuel, true, km)

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            TanqueoDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomFillUpRepository(database.fillUpDao())
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

    @Test fun `borrar quita solo ese tanqueo`() = runTest {
        val a = repository.add(fillUp("2026-10-01T10:00:00Z"))
        val b = repository.add(fillUp("2026-10-02T10:00:00Z"))
        repository.delete(a)
        assertNull(repository.observe(a).first())
        assertEquals(listOf(b), repository.observeAll().first().map { it.id })
    }
}
