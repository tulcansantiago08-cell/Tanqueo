package com.tanqueo.ui.register

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
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
import com.tanqueo.ui.components.SavedConfirmation
import com.tanqueo.ui.components.ValueCardModel
import com.tanqueo.ui.components.ValueCardsScreen
import com.tanqueo.ui.components.labelRes
import kotlinx.coroutines.launch

private val GALLON_STEPS = listOf(1, 10) // en décimas de galón
private val COP_STEPS = listOf(1, 10, 100) // en múltiplos de $100
private val KM_STEPS = listOf(1, 10, 100, 1_000, 10_000)

private const val PAID_PAGE = 1

@Composable
fun RegisterScreen(onSaved: () -> Unit, viewModel: RegisterViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var gallonStep by rememberSaveable { mutableIntStateOf(0) }
    var copStep by rememberSaveable { mutableIntStateOf(1) }
    var kmStep by rememberSaveable { mutableIntStateOf(1) }
    // Sin historia hay que recorrer el odómetro completo: arranca de a mil.
    LaunchedEffect(state.hasOdometerHistory, state.isLoading) {
        if (!state.isLoading && !state.hasOdometerHistory) kmStep = 3
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    if (state.isSaved) {
        ScreenScaffold { SavedConfirmation(onFinished = onSaved) }
        return
    }

    val stepLabel = R.string.hint_step
    val cards = listOf(
        ValueCardModel(
            label = stringResource(R.string.card_gallons),
            value = Formats.oneDecimal(state.gallons),
            onSteps = { viewModel.adjustGallons(it * GALLON_STEPS[gallonStep]) },
            hint = stringResource(stepLabel, Formats.oneDecimal(GALLON_STEPS[gallonStep] / 10.0)),
            onHintClick = { gallonStep = (gallonStep + 1) % GALLON_STEPS.size },
        ),
        ValueCardModel(
            label = stringResource(R.string.card_paid),
            value = Formats.cop(state.paidCop),
            onSteps = { viewModel.adjustPaid(it * COP_STEPS[copStep]) },
            hint = stringResource(stepLabel, Formats.cop(COP_STEPS[copStep] * 100L)),
            onHintClick = { copStep = (copStep + 1) % COP_STEPS.size },
        ),
        ValueCardModel(
            label = stringResource(R.string.card_fuel),
            value = stringResource(state.fuelType.labelRes),
            onSteps = viewModel::adjustFuel,
            decreaseIcon = R.drawable.ic_chevron_left,
            increaseIcon = R.drawable.ic_chevron_right,
        ),
        ValueCardModel(
            label = stringResource(R.string.card_fill_kind),
            value = stringResource(if (state.isFull) R.string.fill_full else R.string.fill_partial),
            onSteps = { viewModel.toggleFull() },
            decreaseIcon = R.drawable.ic_chevron_left,
            increaseIcon = R.drawable.ic_chevron_right,
        ),
        ValueCardModel(
            label = stringResource(R.string.card_odometer),
            value = state.odometerKm?.let { Formats.integer(it) } ?: stringResource(R.string.value_empty),
            onSteps = { viewModel.adjustOdometer(it * KM_STEPS[kmStep]) },
            hint = if (state.odometerKm == null) {
                stringResource(R.string.hint_odometer_recommended)
            } else {
                stringResource(stepLabel, Formats.integer(KM_STEPS[kmStep]))
            },
            onHintClick = if (state.odometerKm == null) {
                { viewModel.adjustOdometer(0) }
            } else {
                { kmStep = (kmStep + 1) % KM_STEPS.size }
            },
            secondaryHint = if (state.odometerKm != null) stringResource(R.string.action_clear) else null,
            onSecondaryHintClick = viewModel::clearOdometer,
        ),
    )

    val startDictation = rememberDictationLauncher(viewModel::onDictated, viewModel::onDictationUnavailable)
    val saveOrComplete = {
        if (state.canSave) {
            viewModel.save()
        } else {
            // Falta el valor (primer tanqueo, sin precios de referencia): se lleva ahí.
            viewModel.closeDictation()
            scope.launch { pagerState.animateScrollToPage(PAID_PAGE) }
        }
        Unit
    }

    if (state.dictation != DictationStatus.IDLE) {
        // Atrás vuelve a las tarjetas con lo dictado, sin perderlo ni salir de Registrar.
        BackHandler(onBack = viewModel::closeDictation)
        DictationPanel(
            state = state,
            onEdit = viewModel::closeDictation,
            onSave = saveOrComplete,
            onRetry = {
                viewModel.closeDictation()
                startDictation()
            },
        )
        return
    }

    ScreenScaffold {
        ValueCardsScreen(
            pagerState = pagerState,
            cards = cards,
            bottomAction = {
                ActionRow(
                    secondaryIcon = R.drawable.ic_mic,
                    secondaryDescription = stringResource(R.string.action_dictate),
                    onSecondary = startDictation,
                    primaryText = stringResource(R.string.action_save),
                    onPrimary = saveOrComplete,
                )
            },
        )
    }
}
