package com.tanqueo.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tanqueo.domain.model.OdometerUpdate
import com.tanqueo.domain.model.TankSettings
import com.tanqueo.domain.repository.CachedTip
import com.tanqueo.domain.repository.SettingsRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<TankSettings> = dataStore.data.map { prefs ->
        TankSettings(
            tankCapacityGallons = prefs[CAPACITY] ?: TankSettings.DEFAULT_CAPACITY_GALLONS,
            reminderKmThreshold = prefs[KM_THRESHOLD] ?: TankSettings.DEFAULT_KM_THRESHOLD,
            reminderDaysThreshold = prefs[DAYS_THRESHOLD] ?: TankSettings.DEFAULT_DAYS_THRESHOLD,
        )
    }.distinctUntilChanged()

    override val odometerUpdate: Flow<OdometerUpdate?> = dataStore.data.map { prefs ->
        val km = prefs[ODOMETER_KM]
        val at = prefs[ODOMETER_AT]
        if (km != null && at != null) OdometerUpdate(km, Instant.ofEpochMilli(at)) else null
    }.distinctUntilChanged()

    override suspend fun setTankCapacity(gallons: Double) {
        dataStore.edit { it[CAPACITY] = gallons }
    }

    override suspend fun setKmThreshold(km: Int) {
        dataStore.edit { it[KM_THRESHOLD] = km }
    }

    override suspend fun setDaysThreshold(days: Int) {
        dataStore.edit { it[DAYS_THRESHOLD] = days }
    }

    override suspend fun setOdometerUpdate(update: OdometerUpdate) {
        dataStore.edit {
            it[ODOMETER_KM] = update.km
            it[ODOMETER_AT] = update.at.toEpochMilli()
        }
    }

    override suspend fun cachedTip(): CachedTip? {
        val prefs = dataStore.data.first()
        val key = prefs[TIP_KEY] ?: return null
        val text = prefs[TIP_TEXT] ?: return null
        return CachedTip(key, text)
    }

    override suspend fun setCachedTip(tip: CachedTip) {
        dataStore.edit {
            it[TIP_KEY] = tip.key
            it[TIP_TEXT] = tip.text
        }
    }

    private companion object {
        val CAPACITY = doublePreferencesKey("tank_capacity_gallons")
        val KM_THRESHOLD = intPreferencesKey("reminder_km_threshold")
        val DAYS_THRESHOLD = intPreferencesKey("reminder_days_threshold")
        val ODOMETER_KM = intPreferencesKey("odometer_update_km")
        val ODOMETER_AT = longPreferencesKey("odometer_update_at")
        val TIP_KEY = stringPreferencesKey("tip_key")
        val TIP_TEXT = stringPreferencesKey("tip_text")
    }
}
