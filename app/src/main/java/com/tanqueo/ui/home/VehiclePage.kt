package com.tanqueo.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.tanqueo.ui.components.ScreenSize
import com.tanqueo.ui.components.fadeInOnAppear
import com.tanqueo.ui.components.rememberAlertPulse
import com.tanqueo.ui.components.rememberCountUp
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.components.softGlow
import com.tanqueo.ui.theme.TanqueoColors
import com.tanqueo.ui.theme.alertColor

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
        HomeMainActions(actions)
    }
}

@Composable
internal fun PageTitle(title: String, screen: ScreenSize) {
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
internal fun CarIllustration(height: Dp, glow: Boolean, modifier: Modifier = Modifier) {
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
