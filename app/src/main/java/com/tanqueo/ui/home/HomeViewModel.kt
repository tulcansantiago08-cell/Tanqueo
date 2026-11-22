package com.tanqueo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanqueo.domain.model.VehicleType
import com.tanqueo.domain.repository.SettingsRepository
import com.tanqueo.domain.repository.SurfaceRefresher
import com.tanqueo.domain.usecase.Dashboard
import com.tanqueo.domain.usecase.ObserveDashboard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeDashboard: ObserveDashboard,
    private val settings: SettingsRepository,
    private val surfaces: SurfaceRefresher,
) : ViewModel() {
    /** Null mientras carga: así no parpadea el estado vacío. */
    val dashboard: StateFlow<Dashboard?> = observeDashboard.observe()
        .map<_, Dashboard?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Pasa al otro vehículo. Sus datos son aparte: el tablero se arma de nuevo con los suyos. */
    fun switchVehicle() {
        val current = dashboard.value?.settings?.vehicle ?: return
        viewModelScope.launch {
            settings.setVehicleType(current.other)
            surfaces.refresh()
        }
    }
}

/** Con dos vehículos, el otro. */
val VehicleType.other: VehicleType
    get() = VehicleType.entries[(ordinal + 1) % VehicleType.entries.size]
