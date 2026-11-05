package com.tanqueo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanqueo.domain.calc.FillUpInput
import com.tanqueo.domain.model.TankSettings
import com.tanqueo.domain.repository.SettingsRepository
import com.tanqueo.domain.repository.SurfaceRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * El estado se actualiza primero en memoria y luego se escribe en DataStore: la corona manda
 * eventos más rápido de lo que tarda el disco y cada paso debe partir del valor del anterior.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val surfaces: SurfaceRefresher,
) : ViewModel() {
    private val _settings = MutableStateFlow<TankSettings?>(null)
    val settings: StateFlow<TankSettings?> = _settings.asStateFlow()

    init {
        viewModelScope.launch { _settings.value = repository.settings.first() }
    }

    fun adjustCapacity(steps: Int) {
        val current = _settings.value ?: return
        val gallons = FillUpInput.adjustGallons(
            current.tankCapacityGallons,
            steps,
            FillUpInput.MIN_CAPACITY_GALLONS,
            FillUpInput.MAX_CAPACITY_GALLONS,
        )
        _settings.value = current.copy(tankCapacityGallons = gallons)
        persist { repository.setTankCapacity(gallons) }
    }

    fun adjustKmThreshold(steps: Int) {
        val current = _settings.value ?: return
        val km = FillUpInput.adjustKmThreshold(current.reminderKmThreshold, steps)
        _settings.value = current.copy(reminderKmThreshold = km)
        persist { repository.setKmThreshold(km) }
    }

    fun adjustDaysThreshold(steps: Int) {
        val current = _settings.value ?: return
        val days = FillUpInput.adjustDaysThreshold(current.reminderDaysThreshold, steps)
        _settings.value = current.copy(reminderDaysThreshold = days)
        persist { repository.setDaysThreshold(days) }
    }

    private fun persist(write: suspend () -> Unit) {
        viewModelScope.launch {
            write()
            surfaces.refresh()
        }
    }
}
