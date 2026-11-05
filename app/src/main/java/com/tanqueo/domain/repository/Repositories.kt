package com.tanqueo.domain.repository

import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.OdometerUpdate
import com.tanqueo.domain.model.TankSettings
import kotlinx.coroutines.flow.Flow

interface FillUpRepository {
    /** Tanqueos del más reciente al más antiguo. */
    fun observeAll(): Flow<List<FillUp>>
    fun observe(id: Long): Flow<FillUp?>
    suspend fun add(fillUp: FillUp): Long
    suspend fun delete(id: Long)
}

interface SettingsRepository {
    val settings: Flow<TankSettings>
    val odometerUpdate: Flow<OdometerUpdate?>
    suspend fun setTankCapacity(gallons: Double)
    suspend fun setKmThreshold(km: Int)
    suspend fun setDaysThreshold(days: Int)
    suspend fun setOdometerUpdate(update: OdometerUpdate)
}

/** Avisa a las superficies fuera de la app (la tile) que los datos cambiaron. */
fun interface SurfaceRefresher {
    fun refresh()
}
