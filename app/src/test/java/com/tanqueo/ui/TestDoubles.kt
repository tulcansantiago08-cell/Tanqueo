package com.tanqueo.ui

import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.OdometerUpdate
import com.tanqueo.domain.model.TankSettings
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

    override fun observeAll(): Flow<List<FillUp>> = items

    override fun observe(id: Long): Flow<FillUp?> = items.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun add(fillUp: FillUp): Long {
        val id = nextId++
        items.value = (items.value + fillUp.copy(id = id)).sortedByDescending { it.at }
        return id
    }

    override suspend fun delete(id: Long) {
        items.value = items.value.filterNot { it.id == id }
    }
}

class FakeSettingsRepository(
    initial: TankSettings = TankSettings(),
    update: OdometerUpdate? = null,
) : SettingsRepository {
    override val settings = MutableStateFlow(initial)
    override val odometerUpdate = MutableStateFlow(update)

    override suspend fun setTankCapacity(gallons: Double) {
        settings.value = settings.value.copy(tankCapacityGallons = gallons)
    }

    override suspend fun setKmThreshold(km: Int) {
        settings.value = settings.value.copy(reminderKmThreshold = km)
    }

    override suspend fun setDaysThreshold(days: Int) {
        settings.value = settings.value.copy(reminderDaysThreshold = days)
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
) : FuelAssistant {
    var tipCalls = 0
    val heard = mutableListOf<String>()

    override suspend fun interpretFillUp(spokenText: String): DictatedFillUp? {
        heard += spokenText
        return dictation
    }

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
