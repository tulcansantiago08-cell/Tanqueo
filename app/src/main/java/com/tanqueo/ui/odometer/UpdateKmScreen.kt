package com.tanqueo.ui.odometer

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.ScreenScaffold
import com.tanqueo.R
import com.tanqueo.domain.format.Formats
import com.tanqueo.ui.components.PrimaryPillButton
import com.tanqueo.ui.components.SavedConfirmation
import com.tanqueo.ui.components.ValueCardModel
import com.tanqueo.ui.components.ValueCardsScreen
import com.tanqueo.ui.components.rememberTextInputLauncher

private val KM_STEPS = listOf(1, 10, 100, 1_000, 10_000)

@Composable
fun UpdateKmScreen(onSaved: () -> Unit, viewModel: UpdateKmViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 1 })
    val haptics = LocalHapticFeedback.current
    var step by rememberSaveable { mutableIntStateOf(1) }
    var isInvalid by rememberSaveable { mutableStateOf(false) }
    val typeKm = rememberTextInputLauncher(
        prompt = stringResource(R.string.type_km_prompt),
        onResult = { text -> if (!text.isNullOrBlank()) isInvalid = !viewModel.type(text) },
        onUnavailable = {},
    )

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    if (state.isSaved) {
        ScreenScaffold { SavedConfirmation(onFinished = onSaved) }
        return
    }

    ScreenScaffold {
        ValueCardsScreen(
            pagerState = pagerState,
            cards = listOf(
                ValueCardModel(
                    label = stringResource(R.string.update_km_title),
                    value = state.km?.let { Formats.integer(it) } ?: stringResource(R.string.value_empty),
                    onSteps = {
                        isInvalid = false
                        viewModel.adjust(it * KM_STEPS[step])
                    },
                    hint = when {
                        isInvalid -> stringResource(R.string.type_invalid)
                        state.km == null -> stringResource(R.string.hint_odometer_recommended)
                        else -> stringResource(R.string.hint_step, Formats.integer(KM_STEPS[step]))
                    },
                    onValueClick = typeKm,
                    valueClickLabel = stringResource(R.string.type_value),
                    onHintClick = if (state.km == null) {
                        { viewModel.adjust(0) }
                    } else {
                        { step = (step + 1) % KM_STEPS.size }
                    },
                ),
            ),
            bottomAction = {
                PrimaryPillButton(
                    text = stringResource(R.string.action_save),
                    enabled = !state.isLoading && state.km != null,
                    onClick = viewModel::save,
                )
            },
        )
    }
}
