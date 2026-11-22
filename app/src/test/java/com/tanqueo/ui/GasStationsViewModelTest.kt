package com.tanqueo.ui

import com.tanqueo.domain.stations.FindGasStations
import com.tanqueo.domain.stations.GeoPoint
import com.tanqueo.domain.stations.StationPlace
import com.tanqueo.domain.stations.StationSearch
import com.tanqueo.ui.stations.GasStationsViewModel
import com.tanqueo.ui.stations.StationsUiState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GasStationsViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    @Test fun `sin permiso lo dice y con permiso busca`() = runTest {
        val here = GeoPoint(1.2136, -77.2811)
        val vm = GasStationsViewModel(FindGasStations({ here }, { _, _ -> listOf(StationPlace("Terpel", null, here)) }))
        assertEquals(StationsUiState.Starting, vm.state.value)
        vm.onPermissionDenied()
        assertEquals(StationsUiState.PermissionDenied, vm.state.value)
        vm.search()
        val done = vm.state.value as StationsUiState.Done
        assertTrue(done.search is StationSearch.Found)
    }
}
