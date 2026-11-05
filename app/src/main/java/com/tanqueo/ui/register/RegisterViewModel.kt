package com.tanqueo.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanqueo.domain.calc.FillUpInput
import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.calc.RangeEstimator
import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.FuelType
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

data class RegisterUiState(
    val isLoading: Boolean = true,
    val gallons: Double = FillUpInput.DEFAULT_GALLONS,
    /** Cero solo si no hay historia de precios: obliga a escribir el valor antes de guardar. */
    val paidCop: Long = 0,
    val fuelType: FuelType = FuelType.CORRIENTE,
    val isFull: Boolean = true,
    /** Null hasta que el usuario lo toque: nunca se guarda un kilometraje que no eligió. */
    val odometerKm: Int? = null,
    val minOdometerKm: Int = 0,
    val suggestedOdometerKm: Int = 0,
    val hasOdometerHistory: Boolean = false,
    val isSaved: Boolean = false,
) {
    val canSave: Boolean get() = !isLoading && gallons > 0 && paidCop > 0
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val fillUps: FillUpRepository,
    private val settings: SettingsRepository,
    private val surfaces: SurfaceRefresher,
    private val clock: Clock,
) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    private var history: List<FillUp> = emptyList()
    private var paidEdited = false
    private var isSaving = false

    init {
        viewModelScope.launch {
            history = fillUps.observeAll().first()
            val config = settings.settings.first()
            val update = settings.odometerUpdate.first()
            val last = history.firstOrNull()
            val fuel = last?.fuelType ?: FuelType.CORRIENTE
            val lastFullGallons = history.firstOrNull { it.isFull }?.gallons
            val gallons = lastFullGallons ?: FillUpInput.DEFAULT_GALLONS
            val minKm = FillUpInput.minOdometer(history)
            val estimate = RangeEstimator.estimate(history, config.tankCapacityGallons, update, clock.instant())
            val driven = (estimate as? RangeEstimate.Available)?.kmSinceLastFillUp?.roundToInt() ?: 0
            _state.update {
                it.copy(
                    isLoading = false,
                    gallons = gallons,
                    fuelType = fuel,
                    paidCop = suggestedPaid(gallons, fuel),
                    minOdometerKm = minKm,
                    suggestedOdometerKm = (minKm + driven).coerceAtMost(FillUpInput.MAX_ODOMETER_KM),
                    hasOdometerHistory = minKm > 0,
                )
            }
        }
    }

    fun adjustGallons(steps: Int) = _state.update { current ->
        val gallons = FillUpInput.adjustGallons(current.gallons, steps)
        current.copy(gallons = gallons, paidCop = paidFor(gallons, current.fuelType, current.paidCop))
    }

    fun adjustPaid(steps: Int) {
        paidEdited = true
        _state.update { it.copy(paidCop = FillUpInput.adjustCop(it.paidCop, steps)) }
    }

    fun adjustFuel(steps: Int) = _state.update { current ->
        val all = FuelType.entries
        val fuel = all[(current.fuelType.ordinal + steps).mod(all.size)]
        current.copy(fuelType = fuel, paidCop = paidFor(current.gallons, fuel, current.paidCop))
    }

    fun toggleFull() = _state.update { it.copy(isFull = !it.isFull) }

    /** El primer toque solo muestra la sugerencia; desde ahí la corona ajusta. */
    fun adjustOdometer(steps: Int) = _state.update { current ->
        val km = current.odometerKm
        current.copy(
            odometerKm = if (km == null) {
                current.suggestedOdometerKm
            } else {
                FillUpInput.adjustOdometer(km, steps, current.minOdometerKm)
            },
        )
    }

    fun clearOdometer() = _state.update { it.copy(odometerKm = null) }

    fun save() {
        val current = _state.value
        if (!current.canSave || isSaving) return
        isSaving = true
        viewModelScope.launch {
            fillUps.add(
                FillUp(
                    at = clock.instant(),
                    gallons = current.gallons,
                    paidCop = current.paidCop,
                    fuelType = current.fuelType,
                    isFull = current.isFull,
                    odometerKm = current.odometerKm,
                ),
            )
            surfaces.refresh()
            _state.update { it.copy(isSaved = true) }
        }
    }

    private fun paidFor(gallons: Double, fuel: FuelType, currentPaid: Long): Long =
        if (paidEdited) currentPaid else suggestedPaid(gallons, fuel)

    private fun suggestedPaid(gallons: Double, fuel: FuelType): Long =
        FillUpInput.lastPricePerGallon(history, fuel)
            ?.let { FillUpInput.suggestedPaid(gallons, it) }
            ?: 0L
}
