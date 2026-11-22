package com.tanqueo.ui.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanqueo.domain.stations.FindGasStations
import com.tanqueo.domain.stations.StationSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StationsUiState {
    /** Todavía no se sabe si hay permiso de ubicación. */
    data object Starting : StationsUiState
    data object PermissionDenied : StationsUiState
    data object Searching : StationsUiState
    data class Done(val search: StationSearch) : StationsUiState
}

@HiltViewModel
class GasStationsViewModel @Inject constructor(private val findGasStations: FindGasStations) : ViewModel() {
    private val _state = MutableStateFlow<StationsUiState>(StationsUiState.Starting)
    val state: StateFlow<StationsUiState> = _state.asStateFlow()

    fun search() {
        if (_state.value == StationsUiState.Searching) return
        _state.value = StationsUiState.Searching
        viewModelScope.launch { _state.value = StationsUiState.Done(findGasStations()) }
    }

    fun onPermissionDenied() {
        _state.value = StationsUiState.PermissionDenied
    }
}
