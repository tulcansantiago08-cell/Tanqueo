package com.tanqueo.debug

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.tanqueo.StartupTask
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
        if (dataStore.data.first()[SEEDED] == true) return
        if (repository.observeAll().first().isEmpty()) {
            SampleData.fillUps(clock.instant()).forEach { repository.add(it) }
        }
        dataStore.edit { it[SEEDED] = true }
    }

    private companion object {
        val SEEDED = booleanPreferencesKey("debug_sample_data_seeded")
    }
}
