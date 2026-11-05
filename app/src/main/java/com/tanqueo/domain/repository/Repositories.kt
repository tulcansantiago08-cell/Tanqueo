package com.tanqueo.domain.repository

import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.OdometerUpdate
import com.tanqueo.domain.model.TankSettings
import com.tanqueo.domain.voice.DictatedFillUp
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

    /** Último consejo aceptado de la IA y la clave de los datos con que se generó. */
    suspend fun cachedTip(): CachedTip?
    suspend fun setCachedTip(tip: CachedTip)
}

data class CachedTip(val key: String, val text: String)

/**
 * La IA. Solo interpreta y redacta; las cifras siempre salen de los cálculos locales. Devuelve null si
 * no está configurada, no hay internet o falla: la app sigue funcionando sin ella.
 */
interface FuelAssistant {
    suspend fun interpretFillUp(spokenText: String): DictatedFillUp?
    suspend fun writeTip(monthFacts: String): String?
}

/** Avisa a las superficies fuera de la app (la tile) que los datos cambiaron. */
fun interface SurfaceRefresher {
    fun refresh()
}
