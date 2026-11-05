package com.tanqueo.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanqueo.domain.usecase.Dashboard
import com.tanqueo.domain.usecase.ObserveDashboard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SummaryViewModel @Inject constructor(observeDashboard: ObserveDashboard) : ViewModel() {
    val dashboard: StateFlow<Dashboard?> = observeDashboard.observe()
        .map<_, Dashboard?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
