package com.tanqueo.ui

import com.tanqueo.domain.START
import com.tanqueo.domain.dayN
import com.tanqueo.domain.full
import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.FuelType
import com.tanqueo.domain.partial
import com.tanqueo.domain.usecase.InterpretDictation
import com.tanqueo.ui.register.DictationStatus
import com.tanqueo.ui.register.RegisterViewModel
import java.time.Clock
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RegisterViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val now = dayN(13.0)
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val refresher = CountingRefresher()
    private val assistant = FakeFuelAssistant()

    private fun viewModel(fillUps: FakeFillUpRepository) =
        RegisterViewModel(fillUps, FakeSettingsRepository(), refresher, clock, InterpretDictation(assistant))

    private val history = listOf(full(0, 1000, 10.0), full(10, 1400, 10.0)) // $16.000/galón

    @Test fun `sin historia no se puede guardar hasta poner el valor`() = runTest {
        val repo = FakeFillUpRepository()
        val vm = viewModel(repo)
        assertEquals(0L, vm.state.value.paidCop)
        assertFalse(vm.state.value.canSave)
        vm.save()
        assertTrue(repo.items.value.isEmpty())

        vm.adjustPaid(10)
        assertTrue(vm.state.value.canSave)
        assertEquals(1_000L, vm.state.value.paidCop)
    }

    @Test fun `parte del ultimo tanqueo lleno y su precio`() = runTest {
        val state = viewModel(FakeFillUpRepository(history)).state.value
        assertEquals(10.0, state.gallons, 0.0)
        assertEquals(160_000L, state.paidCop)
        assertEquals(FuelType.CORRIENTE, state.fuelType)
        assertTrue(state.isFull)
    }

    @Test fun `el valor sigue a los galones hasta que el usuario lo edita`() = runTest {
        val vm = viewModel(FakeFillUpRepository(history))
        vm.adjustGallons(5)
        assertEquals(168_000L, vm.state.value.paidCop)
        vm.adjustPaid(1)
        assertEquals(168_100L, vm.state.value.paidCop)
        vm.adjustGallons(5)
        assertEquals(168_100L, vm.state.value.paidCop)
    }

    @Test fun `el combustible recorre las tres opciones en ambos sentidos`() = runTest {
        val vm = viewModel(FakeFillUpRepository(history))
        vm.adjustFuel(1)
        assertEquals(FuelType.EXTRA, vm.state.value.fuelType)
        vm.adjustFuel(1)
        assertEquals(FuelType.DIESEL, vm.state.value.fuelType)
        vm.adjustFuel(1)
        assertEquals(FuelType.CORRIENTE, vm.state.value.fuelType)
        vm.adjustFuel(-1)
        assertEquals(FuelType.DIESEL, vm.state.value.fuelType)
    }

    @Test fun `no se guarda un kilometraje que el usuario no eligio`() = runTest {
        val repo = FakeFillUpRepository(history)
        val vm = viewModel(repo)
        assertNull(vm.state.value.odometerKm)
        vm.save()
        assertNull(repo.items.value.first().odometerKm)
    }

    @Test fun `el primer toque del odometro muestra la sugerencia y luego ajusta`() = runTest {
        val vm = viewModel(FakeFillUpRepository(history))
        vm.adjustOdometer(1)
        assertEquals(1520, vm.state.value.odometerKm) // 1400 + 3 días × 40 km
        vm.adjustOdometer(10)
        assertEquals(1530, vm.state.value.odometerKm)
        vm.adjustOdometer(-10_000)
        assertEquals(1400, vm.state.value.odometerKm) // no baja del último registrado
        vm.clearOdometer()
        assertNull(vm.state.value.odometerKm)
    }

    @Test fun `sin estimacion la sugerencia es el ultimo kilometraje`() = runTest {
        val vm = viewModel(FakeFillUpRepository(listOf(partial(0, 900))))
        vm.adjustOdometer(1)
        assertEquals(900, vm.state.value.odometerKm)
    }

    @Test fun `guardar usa la hora actual, actualiza la tile y termina`() = runTest {
        val repo = FakeFillUpRepository(history)
        val vm = viewModel(repo)
        vm.toggleFull()
        vm.adjustOdometer(1)
        vm.save()
        val saved: FillUp = repo.items.value.first()
        assertEquals(now, saved.at)
        assertFalse(saved.isFull)
        assertEquals(1520, saved.odometerKm)
        assertEquals(1, refresher.count)
        assertTrue(vm.state.value.isSaved)
    }

    @Test fun `lo dictado llena las tarjetas sin guardar`() = runTest {
        val repo = FakeFillUpRepository(history)
        val vm = viewModel(repo)
        vm.onDictated("12 galones, 200 mil, extra, parcial, 1500 km")
        val state = vm.state.value
        assertEquals(DictationStatus.REVIEW, state.dictation)
        assertEquals(12.0, state.gallons, 0.0)
        assertEquals(200_000L, state.paidCop)
        assertEquals(FuelType.EXTRA, state.fuelType)
        assertFalse(state.isFull)
        assertEquals(1500, state.odometerKm)
        assertEquals(2, repo.items.value.size)
    }

    @Test fun `dictar solo los galones recalcula el valor y no inventa km`() = runTest {
        val vm = viewModel(FakeFillUpRepository(history))
        vm.onDictated("15 galones")
        assertEquals(240_000L, vm.state.value.paidCop)
        assertNull(vm.state.value.odometerKm)
    }

    @Test fun `el valor dictado no lo pisan los galones despues`() = runTest {
        val vm = viewModel(FakeFillUpRepository(history))
        vm.onDictated("200 mil pesos")
        vm.adjustGallons(5)
        assertEquals(200_000L, vm.state.value.paidCop)
    }

    @Test fun `una frase sin datos pide repetir y deja todo igual`() = runTest {
        val vm = viewModel(FakeFillUpRepository(history))
        vm.onDictated("hola qué tal")
        assertEquals(DictationStatus.NOT_UNDERSTOOD, vm.state.value.dictation)
        assertEquals(160_000L, vm.state.value.paidCop)
    }

    @Test fun `cancelar el dictado no cambia nada`() = runTest {
        val vm = viewModel(FakeFillUpRepository(history))
        vm.onDictated(null)
        assertEquals(DictationStatus.IDLE, vm.state.value.dictation)
    }

    @Test fun `un km dictado menor al ultimo se descarta`() = runTest {
        val vm = viewModel(FakeFillUpRepository(history))
        vm.onDictated("10 galones 900 km")
        assertNull(vm.state.value.odometerKm)
    }

    @Test fun `tocar guardar dos veces crea un solo tanqueo`() = runTest {
        val repo = FakeFillUpRepository(history)
        val vm = viewModel(repo)
        vm.save()
        vm.save()
        assertEquals(3, repo.items.value.size)
        assertEquals(START, repo.items.value.last().at)
    }
}
