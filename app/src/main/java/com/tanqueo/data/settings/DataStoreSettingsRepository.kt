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
import com.tanqueo.domain.model.VehicleType
import com.tanqueo.domain.repository.CachedTip
import com.tanqueo.domain.repository.SettingsRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Los ajustes y el "Actualizar km" se guardan aparte para cada vehículo. Las claves del carro son las
 * mismas de antes de poder elegir vehículo, así lo que ya estaba guardado sigue siendo del carro.
 */
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<TankSettings> = dataStore.data.map { prefs ->
        val active = prefs.activeVehicle()
        prefs.settingsOf(active ?: VehicleType.DEFAULT).copy(vehicleType = active)
    }.distinctUntilChanged()

    override val odometerUpdate: Flow<OdometerUpdate?> = dataStore.data.map { prefs ->
        prefs.odometerOf(prefs.activeOrDefault())
    }.distinctUntilChanged()

    override fun settingsFor(vehicle: VehicleType): Flow<TankSettings> =
        dataStore.data.map { it.settingsOf(vehicle) }.distinctUntilChanged()

    override fun odometerUpdateFor(vehicle: VehicleType): Flow<OdometerUpdate?> =
        dataStore.data.map { it.odometerOf(vehicle) }.distinctUntilChanged()

    override suspend fun setTankCapacity(gallons: Double) {
        dataStore.edit { it[Keys.of(it.activeOrDefault()).capacity] = gallons }
    }

    override suspend fun setKmThreshold(km: Int) {
        dataStore.edit { it[Keys.of(it.activeOrDefault()).kmThreshold] = km }
    }

    override suspend fun setDaysThreshold(days: Int) {
        dataStore.edit { it[Keys.of(it.activeOrDefault()).daysThreshold] = days }
    }

    override suspend fun setVehicleType(type: VehicleType) {
        dataStore.edit { it[VEHICLE] = type.name }
    }

    override suspend fun setOdometerUpdate(update: OdometerUpdate) {
        dataStore.edit {
            val keys = Keys.of(it.activeOrDefault())
            it[keys.odometerKm] = update.km
            it[keys.odometerAt] = update.at.toEpochMilli()
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

    private fun Preferences.activeVehicle(): VehicleType? =
        this[VEHICLE]?.let { name -> VehicleType.entries.firstOrNull { it.name == name } }

    private fun Preferences.activeOrDefault(): VehicleType = activeVehicle() ?: VehicleType.DEFAULT

    private fun Preferences.settingsOf(vehicle: VehicleType): TankSettings {
        val keys = Keys.of(vehicle)
        return TankSettings(
            tankCapacityGallons = this[keys.capacity] ?: vehicle.defaultCapacityGallons,
            reminderKmThreshold = this[keys.kmThreshold] ?: TankSettings.DEFAULT_KM_THRESHOLD,
            reminderDaysThreshold = this[keys.daysThreshold] ?: TankSettings.DEFAULT_DAYS_THRESHOLD,
            vehicleType = vehicle,
        )
    }

    private fun Preferences.odometerOf(vehicle: VehicleType): OdometerUpdate? {
        val keys = Keys.of(vehicle)
        val km = this[keys.odometerKm]
        val at = this[keys.odometerAt]
        return if (km != null && at != null) OdometerUpdate(km, Instant.ofEpochMilli(at)) else null
    }

    /** Claves de un vehículo. Las del carro no llevan sufijo: son las que ya existían. */
    private class Keys(suffix: String) {
        val capacity = doublePreferencesKey("tank_capacity_gallons$suffix")
        val kmThreshold = intPreferencesKey("reminder_km_threshold$suffix")
        val daysThreshold = intPreferencesKey("reminder_days_threshold$suffix")
        val odometerKm = intPreferencesKey("odometer_update_km$suffix")
        val odometerAt = longPreferencesKey("odometer_update_at$suffix")

        companion object {
            private val byVehicle = VehicleType.entries.associateWith { vehicle ->
                Keys(if (vehicle == VehicleType.CARRO) "" else "_${vehicle.name.lowercase()}")
            }

            fun of(vehicle: VehicleType): Keys = byVehicle.getValue(vehicle)
        }
    }

    private companion object {
        val VEHICLE = stringPreferencesKey("vehicle_type")
        val TIP_KEY = stringPreferencesKey("tip_key")
        val TIP_TEXT = stringPreferencesKey("tip_text")
    }
}
