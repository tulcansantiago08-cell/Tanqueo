package com.tanqueo.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.repository.FillUpRepository
import com.tanqueo.domain.repository.SurfaceRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(repository: FillUpRepository) : ViewModel() {
    /** Del más reciente al más antiguo. Null mientras carga. */
    val fillUps: StateFlow<List<FillUp>?> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@HiltViewModel
class FillUpDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FillUpRepository,
    private val surfaces: SurfaceRefresher,
) : ViewModel() {
    private val id: Long = checkNotNull(savedStateHandle[ARG_ID])

    val fillUp: StateFlow<FillUp?> = repository.observe(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted: StateFlow<Boolean> = _isDeleted.asStateFlow()

    fun delete() {
        viewModelScope.launch {
            repository.delete(id)
            surfaces.refresh()
            _isDeleted.value = true
        }
    }

    companion object {
        const val ARG_ID = "id"
    }
}
