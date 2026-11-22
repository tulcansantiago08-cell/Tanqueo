package com.tanqueo.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanqueo.domain.model.VehicleType
import com.tanqueo.domain.repository.SettingsRepository
import com.tanqueo.domain.repository.SurfaceRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Qué mostrar al abrir: nada mientras carga, la bienvenida si falta elegir vehículo, o la app. */
enum class StartState { LOADING, CHOOSE_VEHICLE, READY }

@HiltViewModel
class StartViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val surfaces: SurfaceRefresher,
) : ViewModel() {
    val state: StateFlow<StartState> = settings.settings
        .map { if (it.vehicleType == null) StartState.CHOOSE_VEHICLE else StartState.READY }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StartState.LOADING)

    /** Al guardarse, [state] pasa solo a READY. */
    fun choose(vehicle: VehicleType) {
        viewModelScope.launch { select(vehicle) }
    }

    /** Deja activo [vehicle] y espera a que quede guardado, por ejemplo antes de abrir su registro. */
    suspend fun select(vehicle: VehicleType) {
        settings.setVehicleType(vehicle)
        surfaces.refresh()
    }
}
