package com.tanqueo.ui

import com.tanqueo.domain.assistant.AskIntent
import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.OdometerUpdate
import com.tanqueo.domain.model.TankSettings
import com.tanqueo.domain.model.VehicleType
import com.tanqueo.domain.repository.CachedTip
import com.tanqueo.domain.repository.FillUpRepository
import com.tanqueo.domain.repository.FuelAssistant
import com.tanqueo.domain.repository.SettingsRepository
import com.tanqueo.domain.repository.SurfaceRefresher
import com.tanqueo.domain.voice.DictatedFillUp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class FakeFillUpRepository(initial: List<FillUp> = emptyList()) : FillUpRepository {
    val items = MutableStateFlow(initial.sortedByDescending { it.at })
    private var nextId = 1_000L

    /** El vehículo "activo" para [observeAll] sin argumento. */
    var activeVehicle: VehicleType = VehicleType.DEFAULT

    override fun observeAll(): Flow<List<FillUp>> = observeAll(activeVehicle)

    override fun observeAll(vehicle: VehicleType): Flow<List<FillUp>> =
        items.map { list -> list.filter { it.vehicle == vehicle } }

    override fun observe(id: Long): Flow<FillUp?> = items.map { list -> list.firstOrNull { it.id == id } }

    /** Simula que la base de datos rechaza el siguiente [add]. */
    var failNextAdd = false

    override suspend fun add(fillUp: FillUp): Long {
        if (failNextAdd) {
            failNextAdd = false
            throw IllegalStateException("base de datos no disponible")
        }
        val id = nextId++
        items.value = (items.value + fillUp.copy(id = id)).sortedByDescending { it.at }
        return id
    }

    override suspend fun update(fillUp: FillUp) {
        check(items.value.any { it.id == fillUp.id }) { "no existe" }
        items.value = items.value.map { if (it.id == fillUp.id) fillUp else it }.sortedByDescending { it.at }
    }

    override suspend fun delete(id: Long) {
        items.value = items.value.filterNot { it.id == id }
    }
}

/**
 * Como el real: [settings] y [odometerUpdate] son los del vehículo activo y los `set…` cambian solo
 * ese. Los del otro vehículo quedan guardados aparte.
 */
class FakeSettingsRepository(
    initial: TankSettings = TankSettings(),
    update: OdometerUpdate? = null,
) : SettingsRepository {
    override val settings = MutableStateFlow(initial)
    override val odometerUpdate = MutableStateFlow(update)
    private val stored = MutableStateFlow<Map<VehicleType, TankSettings>>(emptyMap())
    private val storedOdometer = MutableStateFlow<Map<VehicleType, OdometerUpdate?>>(emptyMap())

    private fun defaultsFor(vehicle: VehicleType) =
        TankSettings(tankCapacityGallons = vehicle.defaultCapacityGallons, vehicleType = vehicle)

    override fun settingsFor(vehicle: VehicleType): Flow<TankSettings> =
        kotlinx.coroutines.flow.combine(settings, stored) { active, others ->
            if (active.vehicle == vehicle) active.copy(vehicleType = vehicle) else others[vehicle] ?: defaultsFor(vehicle)
        }

    override fun odometerUpdateFor(vehicle: VehicleType): Flow<OdometerUpdate?> =
        kotlinx.coroutines.flow.combine(settings, odometerUpdate, storedOdometer) { active, update, others ->
            if (active.vehicle == vehicle) update else others[vehicle]
        }

    override suspend fun setTankCapacity(gallons: Double) {
        settings.value = settings.value.copy(tankCapacityGallons = gallons)
    }

    override suspend fun setKmThreshold(km: Int) {
        settings.value = settings.value.copy(reminderKmThreshold = km)
    }

    override suspend fun setDaysThreshold(days: Int) {
        settings.value = settings.value.copy(reminderDaysThreshold = days)
    }

    override suspend fun setVehicleType(type: VehicleType) {
        val current = settings.value
        if (current.vehicle == type) {
            settings.value = current.copy(vehicleType = type)
            return
        }
        stored.value = stored.value + (current.vehicle to current.copy(vehicleType = current.vehicle))
        storedOdometer.value = storedOdometer.value + (current.vehicle to odometerUpdate.value)
        odometerUpdate.value = storedOdometer.value[type]
        settings.value = (stored.value[type] ?: defaultsFor(type)).copy(vehicleType = type)
    }

    override suspend fun setOdometerUpdate(update: OdometerUpdate) {
        odometerUpdate.value = update
    }

    var tip: CachedTip? = null
    override suspend fun cachedTip(): CachedTip? = tip
    override suspend fun setCachedTip(tip: CachedTip) {
        this.tip = tip
    }
}

class FakeFuelAssistant(
    var dictation: DictatedFillUp? = null,
    var tip: String? = null,
    var question: AskIntent? = null,
) : FuelAssistant {
    var tipCalls = 0
    val heard = mutableListOf<String>()

    override suspend fun interpretFillUp(spokenText: String): DictatedFillUp? {
        heard += spokenText
        return dictation
    }

    override suspend fun interpretQuestion(question: String): AskIntent? = this.question

    override suspend fun writeTip(monthFacts: String): String? {
        tipCalls++
        return tip
    }
}

class CountingRefresher : SurfaceRefresher {
    var count = 0
    override fun refresh() {
        count++
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
