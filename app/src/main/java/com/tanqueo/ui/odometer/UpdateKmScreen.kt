package com.tanqueo.ui.odometer

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.tanqueo.ui.components.ValueCardModel
import com.tanqueo.ui.components.ValueCardsScreen

private val KM_STEPS = listOf(1, 10, 100, 1_000, 10_000)

@Composable
fun UpdateKmScreen(onSaved: () -> Unit, viewModel: UpdateKmViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 1 })
    val haptics = LocalHapticFeedback.current
    var step by rememberSaveable { mutableIntStateOf(1) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            onSaved()
        }
    }

    ScreenScaffold {
        ValueCardsScreen(
            pagerState = pagerState,
            cards = listOf(
                ValueCardModel(
                    label = stringResource(R.string.update_km_title),
                    value = Formats.integer(state.km),
                    onSteps = { viewModel.adjust(it * KM_STEPS[step]) },
                    hint = stringResource(R.string.hint_step, Formats.integer(KM_STEPS[step])),
                    onHintClick = { step = (step + 1) % KM_STEPS.size },
                ),
            ),
            bottomAction = {
                PrimaryPillButton(
                    text = stringResource(R.string.action_save),
                    enabled = !state.isLoading,
                    onClick = viewModel::save,
                )
            },
        )
    }
}
