package com.tanqueo.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.calc.RangeLevel
import com.tanqueo.domain.format.Formats
import com.tanqueo.domain.usecase.Dashboard
import com.tanqueo.ui.components.PrimaryPillButton
import com.tanqueo.ui.components.RangeRing
import com.tanqueo.ui.components.ScreenSize
import com.tanqueo.ui.components.rangeDaysText
import com.tanqueo.ui.components.refuelDateText
import com.tanqueo.ui.components.rememberAlertPulse
import com.tanqueo.ui.components.rememberCountUp
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.theme.TanqueoColors
import com.tanqueo.ui.theme.alertColor

@Composable
fun RangePage(dashboard: Dashboard, isCurrent: Boolean, actions: HomeActions) {
    val screen = rememberScreenSize()
    val estimate = dashboard.estimate as? RangeEstimate.Available
    val level = estimate?.let { RangeLevel.of(it.kmRemaining) } ?: RangeLevel.NORMAL
    val fraction = rememberCountUp(estimate?.tankFraction?.toFloat() ?: 0f, isCurrent)
    val ringSize = screen.dp(0.64f)
    val ringTop = screen.contentTop - screen.dp(0.02f)

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = ringTop)
                .size(ringSize),
            contentAlignment = Alignment.Center,
        ) {
            RangeRing(fraction, level)
            if (estimate != null) {
                RangeRingContent(estimate, level, isCurrent, screen)
            } else {
                Text(
                    text = stringResource(R.string.range_insufficient),
                    color = TanqueoColors.Secondary,
                    fontSize = screen.sp(10f),
                    lineHeight = screen.sp(12f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(screen.dp(0.42f))
                        .offset(y = -screen.dp(0.03f)),
                )
            }
        }
        PrimaryPillButton(
            text = stringResource(R.string.action_add_fill_up),
            onClick = actions.onAddFillUp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = ringTop + ringSize - screen.dp(0.135f)),
        )
    }
}

@Composable
private fun RangeRingContent(
    estimate: RangeEstimate.Available,
    level: RangeLevel,
    isCurrent: Boolean,
    screen: ScreenSize,
) {
    val km = rememberCountUp(estimate.kmRemaining.toFloat(), isCurrent)
    val pulse = rememberAlertPulse(level)
    val color = level.alertColor(TanqueoColors.White).copy(alpha = pulse)
    Column(
        modifier = Modifier.offset(y = -screen.dp(0.035f)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_title_range),
            color = TanqueoColors.Secondary,
            fontSize = screen.sp(10f),
            lineHeight = screen.sp(12f),
            maxLines = 1,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text = stringResource(R.string.range_km_number, Formats.roundedKm(km.toDouble())),
                style = TextStyle(
                    color = color,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp,
                ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(14.sp, screen.sp(30f), 1.sp),
                modifier = Modifier
                    .width(screen.dp(0.37f))
                    .alignByBaseline(),
            )
            Text(
                text = stringResource(R.string.unit_km),
                color = color,
                fontSize = screen.sp(11f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
        }
        estimate.daysRemaining?.let { days ->
            // Limitado al interior del anillo: "menos de 1 día" no debe tocar el arco.
            BasicText(
                text = rangeDaysText(days),
                style = TextStyle(color = TanqueoColors.Secondary, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(8.sp, screen.sp(11f), 0.5.sp),
                modifier = Modifier.width(screen.dp(0.38f)),
            )
            BasicText(
                text = refuelDateText(days),
                style = TextStyle(
                    color = TanqueoColors.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(8.sp, screen.sp(10f), 0.5.sp),
                modifier = Modifier.width(screen.dp(0.38f)),
            )
        }
    }
}
