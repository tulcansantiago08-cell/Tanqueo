package com.tanqueo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.usecase.ObserveDashboard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(observeDashboard: ObserveDashboard) : ViewModel() {
    /** Null mientras carga: así no parpadea el mensaje de "sin datos". */
    val estimate: StateFlow<RangeEstimate?> = observeDashboard.observe()
        .map<_, RangeEstimate?> { it.estimate }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
