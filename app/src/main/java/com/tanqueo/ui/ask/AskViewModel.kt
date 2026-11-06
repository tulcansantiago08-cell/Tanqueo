package com.tanqueo.ui.ask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanqueo.domain.usecase.AnswerQuestion
import com.tanqueo.domain.usecase.AskAnswer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AskUiState {
    /** [inputUnavailable]: el reloj no tiene teclado ni dictado, solo sirven los ejemplos. */
    data class Idle(val inputUnavailable: Boolean = false) : AskUiState
    data class Thinking(val question: String) : AskUiState
    data class Answered(val question: String, val answer: AskAnswer) : AskUiState
}

@HiltViewModel
class AskViewModel @Inject constructor(private val answerQuestion: AnswerQuestion) : ViewModel() {
    private val _state = MutableStateFlow<AskUiState>(AskUiState.Idle())
    val state: StateFlow<AskUiState> = _state.asStateFlow()

    /** [question] es null si se canceló el teclado o el dictado: no cambia nada. */
    fun ask(question: String?) {
        val text = question?.trim().orEmpty()
        if (text.isEmpty() || _state.value is AskUiState.Thinking) return
        _state.value = AskUiState.Thinking(text)
        viewModelScope.launch { _state.value = AskUiState.Answered(text, answerQuestion(text)) }
    }

    fun reset() {
        _state.value = AskUiState.Idle()
    }

    fun onInputUnavailable() {
        _state.value = AskUiState.Idle(inputUnavailable = true)
    }
}
