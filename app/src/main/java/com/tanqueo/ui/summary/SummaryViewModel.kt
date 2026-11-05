package com.tanqueo.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanqueo.domain.repository.FillUpRepository
import com.tanqueo.domain.tip.MonthTip
import com.tanqueo.domain.tip.MonthTips
import com.tanqueo.domain.usecase.Dashboard
import com.tanqueo.domain.usecase.ObserveDashboard
import com.tanqueo.domain.usecase.WriteMonthTip
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TipUi(val local: MonthTip, val written: String?)

@HiltViewModel
class SummaryViewModel @Inject constructor(
    observeDashboard: ObserveDashboard,
    fillUps: FillUpRepository,
    writeMonthTip: WriteMonthTip,
    clock: Clock,
) : ViewModel() {
    private val _tip = MutableStateFlow<TipUi?>(null)
    val tip: StateFlow<TipUi?> = _tip.asStateFlow()

    init {
        viewModelScope.launch {
            val now = clock.instant()
            val facts = MonthTips.facts(fillUps.observeAll().first(), now, clock.zone)
            // El consejo local aparece al instante; la redacción de la IA lo reemplaza si llega.
            _tip.value = TipUi(MonthTips.localTip(facts), written = null)
            val written = writeMonthTip(facts, now.atZone(clock.zone).toLocalDate())
            if (written != null) _tip.update { it?.copy(written = written) }
        }
    }

    val dashboard: StateFlow<Dashboard?> = observeDashboard.observe()
        .map<_, Dashboard?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
