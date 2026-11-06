package com.tanqueo.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanqueo.domain.calc.FillUpInput
import com.tanqueo.domain.calc.FillUpWarning
import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.calc.RangeEstimator
import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.FuelType
import com.tanqueo.domain.model.TankSettings
import com.tanqueo.domain.repository.FillUpRepository
import com.tanqueo.domain.repository.SettingsRepository
import com.tanqueo.domain.repository.SurfaceRefresher
import com.tanqueo.domain.usecase.InterpretDictation
import com.tanqueo.domain.voice.SpanishNumbers
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

/** Qué se muestra encima de las tarjetas: el dictado en curso o el resumen antes de guardar. */
enum class RegisterPanel { NONE, INTERPRETING, REVIEW, CONFIRM, NOT_UNDERSTOOD, UNAVAILABLE }

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
    val panel: RegisterPanel = RegisterPanel.NONE,
    val capacityGallons: Double = TankSettings.DEFAULT_CAPACITY_GALLONS,
) {
    val canSave: Boolean get() = !isLoading && gallons > 0 && paidCop > 0
    val warning: FillUpWarning? get() = FillUpInput.warning(gallons, paidCop, capacityGallons)
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val fillUps: FillUpRepository,
    private val settings: SettingsRepository,
    private val surfaces: SurfaceRefresher,
    private val clock: Clock,
    private val interpretDictation: InterpretDictation,
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
                    capacityGallons = config.tankCapacityGallons,
                    suggestedOdometerKm = (minKm + driven).coerceAtMost(FillUpInput.MAX_ODOMETER_KM),
                    hasOdometerHistory = minKm > 0,
                )
            }
        }
    }

    /** Lo escrito en el teclado. Devuelve false si no es un valor posible, sin cambiar nada. */
    fun typeGallons(text: String): Boolean {
        val gallons = SpanishNumbers.parse(text)?.takeIf { it in FillUpInput.MIN_GALLONS..FillUpInput.MAX_GALLONS } ?: return false
        _state.update { current ->
            val rounded = FillUpInput.adjustGallons(gallons, 0)
            current.copy(gallons = rounded, paidCop = paidFor(rounded, current.fuelType, current.paidCop))
        }
        return true
    }

    fun typePaid(text: String): Boolean {
        val paid = SpanishNumbers.parse(text)?.toLong()?.takeIf { it in FillUpInput.COP_STEP..FillUpInput.MAX_COP } ?: return false
        paidEdited = true
        _state.update { it.copy(paidCop = paid) }
        return true
    }

    fun typeOdometer(text: String): Boolean {
        val minKm = _state.value.minOdometerKm
        val km = SpanishNumbers.parse(text)?.toInt()?.takeIf { it in maxOf(minKm, 1)..FillUpInput.MAX_ODOMETER_KM } ?: return false
        _state.update { it.copy(odometerKm = km) }
        return true
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

    /** Llena las tarjetas con lo dictado. No guarda: el usuario revisa primero. */
    fun onDictated(spokenText: String?) {
        if (spokenText.isNullOrBlank() || _state.value.isLoading) return
        _state.update { it.copy(panel = RegisterPanel.INTERPRETING) }
        viewModelScope.launch {
            val dictated = interpretDictation(spokenText, _state.value.minOdometerKm)
            if (dictated.isEmpty) {
                _state.update { it.copy(panel = RegisterPanel.NOT_UNDERSTOOD) }
                return@launch
            }
            if (dictated.paidCop != null) paidEdited = true
            _state.update { current ->
                val gallons = dictated.gallons ?: current.gallons
                val fuel = dictated.fuelType ?: current.fuelType
                current.copy(
                    gallons = gallons,
                    fuelType = fuel,
                    paidCop = dictated.paidCop ?: paidFor(gallons, fuel, current.paidCop),
                    isFull = dictated.isFull ?: current.isFull,
                    odometerKm = dictated.odometerKm ?: current.odometerKm,
                    panel = RegisterPanel.REVIEW,
                )
            }
        }
    }

    fun onDictationUnavailable() = _state.update { it.copy(panel = RegisterPanel.UNAVAILABLE) }

    /** Muestra el resumen antes de guardar, para que un toque suelto no anote un tanqueo. */
    fun confirmBeforeSaving() = _state.update {
        if (it.canSave) it.copy(panel = RegisterPanel.CONFIRM) else it
    }

    fun closePanel() = _state.update { it.copy(panel = RegisterPanel.NONE) }

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
