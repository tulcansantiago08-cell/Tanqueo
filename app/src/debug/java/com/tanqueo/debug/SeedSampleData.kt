package com.tanqueo.debug

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.tanqueo.StartupTask
import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.VehicleType
import com.tanqueo.domain.repository.FillUpRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Carga los datos de ejemplo una sola vez, para que borrarlos no los haga reaparecer. */
class SeedSampleData @Inject constructor(
    private val repository: FillUpRepository,
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
) : StartupTask {
    override suspend fun run() {
        seedOnce(SEEDED, VehicleType.CARRO) { SampleData.fillUps(clock.instant()) }
        seedOnce(SEEDED_MOTO, VehicleType.MOTO) { SampleData.motoFillUps(clock.instant()) }
    }

    private suspend fun seedOnce(flag: Preferences.Key<Boolean>, vehicle: VehicleType, samples: () -> List<FillUp>) {
        if (dataStore.data.first()[flag] == true) return
        if (repository.observeAll(vehicle).first().isEmpty()) samples().forEach { repository.add(it) }
        dataStore.edit { it[flag] = true }
    }

    private companion object {
        val SEEDED = booleanPreferencesKey("debug_sample_data_seeded")
        val SEEDED_MOTO = booleanPreferencesKey("debug_sample_moto_seeded")
    }
}
