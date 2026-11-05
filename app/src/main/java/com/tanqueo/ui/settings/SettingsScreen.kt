package com.tanqueo.ui.settings

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.ScreenScaffold
import com.tanqueo.R
import com.tanqueo.domain.format.Formats
import com.tanqueo.domain.model.TankSettings
import com.tanqueo.ui.components.ValueCardModel
import com.tanqueo.ui.components.ValueCardsScreen
import com.tanqueo.ui.components.rememberNotificationAccess

private val CAPACITY_STEPS = listOf(1, 10) // en décimas de galón
private val KM_THRESHOLD_STEPS = listOf(1, 10) // en múltiplos de 10 km

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val loaded by viewModel.settings.collectAsStateWithLifecycle()
    val settings = loaded ?: TankSettings()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val notifications = rememberNotificationAccess()
    var capacityStep by rememberSaveable { mutableIntStateOf(1) }
    var kmStep by rememberSaveable { mutableIntStateOf(0) }

    val cards = listOf(
        ValueCardModel(
            label = stringResource(R.string.settings_capacity),
            value = stringResource(R.string.value_gallons, Formats.oneDecimal(settings.tankCapacityGallons)),
            onSteps = { viewModel.adjustCapacity(it * CAPACITY_STEPS[capacityStep]) },
            hint = stringResource(R.string.hint_step, Formats.oneDecimal(CAPACITY_STEPS[capacityStep] / 10.0)),
            onHintClick = { capacityStep = (capacityStep + 1) % CAPACITY_STEPS.size },
        ),
        ValueCardModel(
            label = stringResource(R.string.settings_km_threshold),
            value = stringResource(R.string.value_km, Formats.integer(settings.reminderKmThreshold)),
            onSteps = { viewModel.adjustKmThreshold(it * KM_THRESHOLD_STEPS[kmStep]) },
            hint = stringResource(R.string.hint_step, Formats.integer(KM_THRESHOLD_STEPS[kmStep] * 10)),
            onHintClick = { kmStep = (kmStep + 1) % KM_THRESHOLD_STEPS.size },
        ),
        ValueCardModel(
            label = stringResource(R.string.settings_days_threshold),
            value = pluralStringResource(
                R.plurals.value_days,
                settings.reminderDaysThreshold,
                settings.reminderDaysThreshold,
            ),
            onSteps = viewModel::adjustDaysThreshold,
        ),
        ValueCardModel(
            label = stringResource(R.string.settings_notifications),
            value = stringResource(
                if (notifications.enabled) R.string.settings_notifications_on else R.string.settings_notifications_off,
            ),
            onSteps = {},
            showSteppers = false,
            hint = if (notifications.enabled) null else stringResource(R.string.settings_notifications_enable),
            onHintClick = notifications.request,
        ),
    )

    ScreenScaffold {
        ValueCardsScreen(pagerState = pagerState, cards = cards, bottomAction = {})
    }
}
