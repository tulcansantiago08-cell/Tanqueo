package com.tanqueo.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.calc.RangeLevel
import com.tanqueo.domain.format.Formats
import com.tanqueo.domain.usecase.Dashboard
import com.tanqueo.ui.components.GlassCard
import com.tanqueo.ui.components.GlassPillButton
import com.tanqueo.ui.components.PrimaryPillButton
import com.tanqueo.ui.components.RangeRing
import com.tanqueo.ui.components.RoundIconButton
import com.tanqueo.ui.components.ScreenSize
import com.tanqueo.ui.components.fadeInOnAppear
import com.tanqueo.ui.components.labelRes
import com.tanqueo.ui.components.rangeDaysText
import com.tanqueo.ui.components.rememberAlertPulse
import com.tanqueo.ui.components.rememberCountUp
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.components.softGlow
import com.tanqueo.ui.theme.TanqueoColors
import com.tanqueo.ui.theme.alertColor
import java.time.ZoneId

/** Espacio común de las páginas: debajo de la hora y encima de los puntos. */
@Composable
private fun PageColumn(
    screen: ScreenSize,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaddingValues(top = screen.contentTop, bottom = screen.dp(if (screen.isRound) 0.1f else 0.07f))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        content = content,
    )
}

// --- Página 1: Mi vehículo ---

@Composable
fun VehiclePage(dashboard: Dashboard, isCurrent: Boolean, actions: HomeActions) {
    val screen = rememberScreenSize()
    val estimate = dashboard.estimate as? RangeEstimate.Available
    val km = rememberCountUp(estimate?.kmRemaining?.toFloat() ?: 0f, isCurrent)
    val odometer = rememberCountUp(dashboard.latestOdometerKm?.toFloat() ?: 0f, isCurrent)
    val level = estimate?.let { RangeLevel.of(it.kmRemaining) } ?: RangeLevel.NORMAL
    val pulse = rememberAlertPulse(level)

    PageColumn(screen) {
        PageTitle(stringResource(R.string.home_title_vehicle), screen)
        CarIllustration(screen.dp(0.2f), glow = true)
        Row(
            modifier = Modifier.fadeInOnAppear(),
            horizontalArrangement = Arrangement.spacedBy(screen.dp(0.09f)),
        ) {
            VehicleStat(
                icon = R.drawable.ic_gas,
                description = stringResource(R.string.home_fuel_range),
                text = if (estimate != null) {
                    stringResource(R.string.range_km, Formats.roundedKm(km.toDouble()))
                } else {
                    stringResource(R.string.value_empty)
                },
                color = level.alertColor(TanqueoColors.White).copy(alpha = pulse),
                screen = screen,
            )
            VehicleStat(
                icon = R.drawable.ic_speed,
                description = stringResource(R.string.home_odometer),
                text = if (dashboard.latestOdometerKm != null) {
                    stringResource(R.string.value_km, Formats.roundedKm(odometer.toDouble()))
                } else {
                    stringResource(R.string.value_empty)
                },
                color = TanqueoColors.White,
                screen = screen,
            )
        }
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(R.drawable.ic_speed, stringResource(R.string.action_update_km), screen.dp(0.2f), actions.onUpdateKm)
            RoundIconButton(R.drawable.ic_history, stringResource(R.string.action_history), screen.dp(0.2f), actions.onHistory)
            RoundIconButton(R.drawable.ic_settings, stringResource(R.string.action_settings), screen.dp(0.2f), actions.onSettings)
        }
    }
}

@Composable
private fun PageTitle(title: String, screen: ScreenSize) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = TanqueoColors.White,
            fontSize = screen.sp(13f),
            lineHeight = screen.sp(15f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Box(
            Modifier
                .padding(top = 3.dp)
                .width(screen.dp(0.46f))
                .height(1.dp)
                .background(TanqueoColors.Accent),
        )
    }
}

@Composable
private fun CarIllustration(height: Dp, glow: Boolean, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_car_top),
        contentDescription = stringResource(R.string.car_illustration),
        modifier = modifier
            .then(if (glow) Modifier.softGlow() else Modifier)
            .size(width = height * 1.8f, height = height),
    )
}

@Composable
private fun VehicleStat(
    @DrawableRes icon: Int,
    description: String,
    text: String,
    color: Color,
    screen: ScreenSize,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = TanqueoColors.Secondary,
            modifier = Modifier.size(screen.dp(0.1f)),
        )
        Text(
            text = text,
            color = color,
            fontSize = screen.sp(13f),
            lineHeight = screen.sp(15f),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

// --- Página 2: Autonomía ---

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
            Text(
                text = rangeDaysText(days),
                color = TanqueoColors.Secondary,
                fontSize = screen.sp(11f),
                lineHeight = screen.sp(13f),
                maxLines = 1,
            )
        }
    }
}

// --- Página 3: Este mes ---

@Composable
fun MonthPage(dashboard: Dashboard, isCurrent: Boolean, actions: HomeActions) {
    val screen = rememberScreenSize()
    val spent = rememberCountUp(dashboard.month.totalSpentCop.toFloat(), isCurrent)
    val last = dashboard.lastFillUp

    PageColumn(screen) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.action_view_summary),
                    onClick = actions.onSummary,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_title_month),
                    color = TanqueoColors.Secondary,
                    fontSize = screen.sp(12f),
                    lineHeight = screen.sp(14f),
                    maxLines = 1,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = TanqueoColors.Secondary,
                    modifier = Modifier.size(screen.dp(0.075f)),
                )
            }
            BasicText(
                text = Formats.cop(spent.toLong()),
                style = TextStyle(
                    color = TanqueoColors.Accent,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp,
                ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(14.sp, screen.sp(29f), 1.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = screen.dp(0.08f)),
            )
        }
        if (last != null) {
            val date = last.at.atZone(ZoneId.systemDefault()).toLocalDate()
            GlassCard(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = screen.sideMargin),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = screen.dp(if (screen.isRound) 0.02f else 0.03f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.home_last_fill_up_date, Formats.shortDate(date)),
                        color = TanqueoColors.Secondary,
                        fontSize = screen.sp(11f),
                        lineHeight = screen.sp(13f),
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(
                            R.string.home_last_fill_up_detail,
                            Formats.oneDecimal(last.gallons),
                            stringResource(last.fuelType.labelRes),
                        ),
                        color = TanqueoColors.White,
                        fontSize = screen.sp(13f),
                        lineHeight = screen.sp(16f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }
        GlassPillButton(
            text = stringResource(R.string.action_view_history),
            icon = R.drawable.ic_history,
            onClick = actions.onHistory,
        )
    }
}

// --- Estado vacío ---

@Composable
fun EmptyHome(actions: HomeActions) {
    val screen = rememberScreenSize()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = screen.contentTop, bottom = screen.dp(0.06f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        CarIllustration(screen.dp(0.24f), glow = false, modifier = Modifier.alpha(0.28f))
        Text(
            text = stringResource(R.string.home_empty),
            color = TanqueoColors.Secondary,
            fontSize = screen.sp(13f),
            lineHeight = screen.sp(16f),
            textAlign = TextAlign.Center,
            modifier = Modifier.width(screen.dp(0.68f)),
        )
        PrimaryPillButton(
            text = stringResource(R.string.action_add_fill_up),
            onClick = actions.onAddFillUp,
        )
    }
}

