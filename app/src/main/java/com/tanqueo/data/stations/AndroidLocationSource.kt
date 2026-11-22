package com.tanqueo.data.stations

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import com.tanqueo.domain.stations.GeoPoint
import com.tanqueo.domain.stations.LocationSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Ubicación del reloj con el LocationManager del sistema, sin servicios extra: sirve en el emulador
 * (con una ubicación simulada) y en un reloj real con GPS. No se guarda en ningún lado.
 */
class AndroidLocationSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LocationSource {

    override suspend fun current(): GeoPoint? {
        if (!hasPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = manager.getProviders(true)
        // Una lectura reciente basta para buscar gasolineras a pocos km; si no hay, se pide una nueva a
        // todos los proveedores a la vez y gana el primero que responda.
        recent(manager, providers, MAX_AGE)?.let { return it.toPoint() }
        val candidates = PREFERRED.filter { it in providers }
        val first = withTimeoutOrNull(FRESH_TIMEOUT_MS) {
            channelFlow {
                candidates.forEach { provider -> launch { fresh(manager, provider)?.let { send(it) } } }
            }.firstOrNull()
        }
        // Sin respuesta (dentro de un edificio, sin señal): sirve una lectura de las últimas horas.
        return (first ?: recent(manager, providers, FALLBACK_AGE))?.toPoint()
    }

    private fun hasPermission(): Boolean = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    @Suppress("MissingPermission") // Revisado en hasPermission().
    private fun recent(manager: LocationManager, providers: List<String>, maxAge: Duration): Location? =
        providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { Duration.ofNanos(android.os.SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos) < maxAge }
            .maxByOrNull { it.time }

    @Suppress("MissingPermission") // Revisado en hasPermission().
    private suspend fun fresh(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancel = CancellationSignal()
            continuation.invokeOnCancellation { cancel.cancel() }
            manager.getCurrentLocation(provider, cancel, ContextCompat.getMainExecutor(context)) { location ->
                if (continuation.isActive) continuation.resume(location)
            }
        }

    private fun Location.toPoint() = GeoPoint(latitude, longitude)

    private companion object {
        // "fused" es LocationManager.FUSED_PROVIDER (API 31); en API 30 no aparece entre los activos y se salta.
        val PREFERRED = listOf("fused", LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val MAX_AGE: Duration = Duration.ofMinutes(10)
        val FALLBACK_AGE: Duration = Duration.ofHours(3)
        const val FRESH_TIMEOUT_MS = 10_000L
    }
}
