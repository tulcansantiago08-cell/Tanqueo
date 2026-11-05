package com.tanqueo.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanqueo.domain.budget.FuelBudget
import com.tanqueo.domain.budget.FuelBudgetCalculator
import com.tanqueo.domain.budget.ReferencePrices
import com.tanqueo.domain.calc.FillUpInput
import com.tanqueo.domain.calc.RangeEstimator
import com.tanqueo.domain.model.FuelType
import com.tanqueo.domain.model.TankSettings
import com.tanqueo.domain.repository.FillUpRepository
import com.tanqueo.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Un precio para elegir: el último que pagó el usuario o uno de referencia ([place] no es null). */
data class PriceOption(val pricePerGallon: Double, val place: String?)

data class BudgetUiState(
    val moneyCop: Long = FuelBudgetCalculator.DEFAULT_MONEY_COP,
    val options: List<PriceOption> = emptyList(),
    val selected: Int = 0,
    val kmPerGallon: Double? = null,
    val tankCapacityGallons: Double = TankSettings.DEFAULT_CAPACITY_GALLONS,
) {
    val option: PriceOption? get() = options.getOrNull(selected)
    val result: FuelBudget?
        get() = option?.let {
            FuelBudgetCalculator.calculate(moneyCop, it.pricePerGallon, kmPerGallon, tankCapacityGallons)
        }
}

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val fillUps: FillUpRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(BudgetUiState())
    val state: StateFlow<BudgetUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val history = fillUps.observeAll().first()
            val fuel = history.firstOrNull()?.fuelType ?: FuelType.CORRIENTE
            val own = FillUpInput.lastPricePerGallon(history, fuel)?.let { PriceOption(it, place = null) }
            val references = ReferencePrices.forFuel(fuel).map { PriceOption(it.pricePerGallon.toDouble(), it.place) }
            _state.update {
                it.copy(
                    options = listOfNotNull(own) + references,
                    kmPerGallon = RangeEstimator.kmPerGallon(history),
                    tankCapacityGallons = settings.settings.first().tankCapacityGallons,
                )
            }
        }
    }

    fun adjustMoney(steps: Int) = _state.update {
        it.copy(moneyCop = FuelBudgetCalculator.adjustMoney(it.moneyCop, steps))
    }

    fun selectPrice(steps: Int) = _state.update {
        if (it.options.isEmpty()) it else it.copy(selected = (it.selected + steps).mod(it.options.size))
    }
}
