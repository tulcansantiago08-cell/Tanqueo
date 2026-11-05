package com.tanqueo.ui.odometer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanqueo.domain.calc.FillUpInput
import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.calc.RangeEstimator
import com.tanqueo.domain.model.OdometerUpdate
import com.tanqueo.domain.repository.FillUpRepository
import com.tanqueo.domain.repository.SettingsRepository
import com.tanqueo.domain.repository.SurfaceRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdateKmUiState(
    val isLoading: Boolean = true,
    val km: Int = 0,
    /** No se acepta menos que el último tanqueo; una lectura manual equivocada sí se puede bajar. */
    val minKm: Int = 0,
    val isSaved: Boolean = false,
)

@HiltViewModel
class UpdateKmViewModel @Inject constructor(
    private val fillUps: FillUpRepository,
    private val settings: SettingsRepository,
    private val surfaces: SurfaceRefresher,
    private val clock: Clock,
) : ViewModel() {
    private val _state = MutableStateFlow(UpdateKmUiState())
    val state: StateFlow<UpdateKmUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val history = fillUps.observeAll().first()
            val update = settings.odometerUpdate.first()
            val config = settings.settings.first()
            val minKm = FillUpInput.minOdometer(history)
            val estimate = RangeEstimator.estimate(history, config.tankCapacityGallons, update, clock.instant())
            val driven = (estimate as? RangeEstimate.Available)?.kmSinceLastFillUp?.roundToInt() ?: 0
            val base = history.firstOrNull()?.odometerKm
            val suggested = if (base != null) base + driven else maxOf(minKm, update?.km ?: 0)
            _state.update { it.copy(isLoading = false, km = maxOf(suggested, minKm), minKm = minKm) }
        }
    }

    fun adjust(steps: Int) = _state.update {
        it.copy(km = FillUpInput.adjustOdometer(it.km, steps, it.minKm))
    }

    fun save() {
        val current = _state.value
        if (current.isLoading || current.isSaved) return
        viewModelScope.launch {
            settings.setOdometerUpdate(OdometerUpdate(current.km, clock.instant()))
            surfaces.refresh()
            _state.update { it.copy(isSaved = true) }
        }
    }
}
