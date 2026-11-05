package com.tanqueo.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.ui.components.RoundIconButton
import com.tanqueo.ui.components.ScreenSize
import com.tanqueo.ui.components.TimeHeader
import com.tanqueo.ui.components.rangeDaysText
import com.tanqueo.ui.components.rangeKmText
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.theme.TanqueoColors

data class HomeActions(
    val onAddFillUp: () -> Unit,
    val onHistory: () -> Unit,
    val onSummary: () -> Unit,
    val onUpdateKm: () -> Unit,
    val onSettings: () -> Unit,
)

// Posiciones en fracciones del lado de la pantalla. Los botones laterales siguen el borde
// del círculo; en las pantallas cuadradas se abren un poco hacia las esquinas.
private const val CAR_TOP = 0.285f
private const val CAR_HEIGHT = 0.225f
private const val KM_TOP = 0.5f
private const val DAYS_TOP = 0.668f
private const val SIDE_BUTTON = 0.19f
private const val SIDE_TOUCH = 0.225f
private const val UPPER_BUTTON_Y = 0.375f
private const val LOWER_BUTTON_Y = 0.73f
private const val MAIN_BUTTON = 0.2f
private const val MAIN_BUTTON_TOP = 0.76f

@Composable
fun HomeScreen(actions: HomeActions, viewModel: HomeViewModel = hiltViewModel()) {
    val estimate by viewModel.estimate.collectAsStateWithLifecycle()
    HomeContent(estimate, actions)
}

@Composable
fun HomeContent(estimate: RangeEstimate?, actions: HomeActions) {
    val screen = rememberScreenSize()
    ScreenScaffold {
        Box(Modifier.fillMaxSize()) {
            TimeHeader(Modifier.align(Alignment.TopCenter))

            Image(
                painter = painterResource(R.drawable.ic_car_top),
                contentDescription = stringResource(R.string.car_illustration),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = screen.dp(CAR_TOP))
                    .size(width = screen.dp(CAR_HEIGHT * 1.8f), height = screen.dp(CAR_HEIGHT)),
            )

            RangeBlock(estimate, screen)

            SideButton(screen, R.drawable.ic_history, R.string.action_history, UPPER_BUTTON_Y, left = true, actions.onHistory)
            SideButton(screen, R.drawable.ic_summary, R.string.action_summary, UPPER_BUTTON_Y, left = false, actions.onSummary)
            SideButton(screen, R.drawable.ic_odometer, R.string.action_update_km, LOWER_BUTTON_Y, left = true, actions.onUpdateKm)
            SideButton(screen, R.drawable.ic_settings, R.string.action_settings, LOWER_BUTTON_Y, left = false, actions.onSettings)

            RoundIconButton(
                icon = R.drawable.ic_add,
                contentDescription = stringResource(R.string.action_add_fill_up),
                size = screen.dp(MAIN_BUTTON),
                touchSize = screen.dp(MAIN_BUTTON + 0.03f),
                accent = true,
                onClick = actions.onAddFillUp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = screen.dp(MAIN_BUTTON_TOP)),
            )
        }
    }
}

@Composable
private fun RangeBlock(estimate: RangeEstimate?, screen: ScreenSize) {
    Box(Modifier.fillMaxSize()) {
        when (estimate) {
            null -> Unit
            RangeEstimate.Insufficient -> Text(
                text = stringResource(R.string.range_insufficient),
                color = TanqueoColors.Secondary,
                fontSize = screen.sp(11f),
                lineHeight = screen.sp(13f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = screen.dp(KM_TOP + 0.01f))
                    .width(screen.dp(0.52f)),
            )
            is RangeEstimate.Available -> {
                BasicText(
                    text = rangeKmText(estimate.kmRemaining),
                    style = TextStyle(
                        color = TanqueoColors.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 14.sp,
                        maxFontSize = screen.sp(26f),
                        stepSize = 1.sp,
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = screen.dp(KM_TOP))
                        .fillMaxWidth()
                        .padding(horizontal = screen.dp(0.1f)),
                )
                estimate.daysRemaining?.let { days ->
                    Text(
                        text = rangeDaysText(days),
                        color = TanqueoColors.Secondary,
                        fontSize = screen.sp(12f),
                        lineHeight = screen.sp(14f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = screen.dp(DAYS_TOP)),
                    )
                }
            }
        }
    }
}

@Composable
private fun SideButton(
    screen: ScreenSize,
    @DrawableRes icon: Int,
    label: Int,
    centerY: Float,
    left: Boolean,
    onClick: () -> Unit,
) {
    val center = sideButtonCenterX(screen.isRound, centerY, SIDE_BUTTON)
    val startFraction = (if (left) center else 1f - center) - SIDE_TOUCH / 2
    RoundIconButton(
        icon = icon,
        contentDescription = stringResource(label),
        size = screen.dp(SIDE_BUTTON),
        touchSize = screen.dp(SIDE_TOUCH),
        onClick = onClick,
        modifier = Modifier.offset(x = screen.dp(startFraction), y = screen.dp(centerY - SIDE_TOUCH / 2)),
    )
}

/** Centro horizontal (fracción desde la izquierda) para que el botón quede pegado al borde. */
private fun sideButtonCenterX(isRound: Boolean, centerY: Float, size: Float): Float {
    if (!isRound) return size / 2 + 0.015f
    val radius = 0.5f - size / 2 - 0.012f
    val dy = centerY - 0.5f
    return 0.5f - kotlin.math.sqrt(radius * radius - dy * dy)
}
