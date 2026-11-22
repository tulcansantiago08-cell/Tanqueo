package com.tanqueo.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
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
import com.tanqueo.domain.model.VehicleType
import com.tanqueo.ui.components.MinTouchSize
import com.tanqueo.ui.components.VehicleIllustration
import com.tanqueo.ui.welcome.label
import com.tanqueo.ui.theme.TanqueoColors
import com.tanqueo.ui.theme.alertColor

@Composable
fun VehiclePage(dashboard: Dashboard, isCurrent: Boolean, actions: HomeActions, onSwitchVehicle: () -> Unit) {
    val screen = rememberScreenSize()
    val estimate = dashboard.estimate as? RangeEstimate.Available
    val km = rememberCountUp(estimate?.kmRemaining?.toFloat() ?: 0f, isCurrent)
    val odometer = rememberCountUp(dashboard.latestOdometerKm?.toFloat() ?: 0f, isCurrent)
    val level = estimate?.let { RangeLevel.of(it.kmRemaining) } ?: RangeLevel.NORMAL
    val pulse = rememberAlertPulse(level)

    PageColumn(screen) {
        VehicleTitle(dashboard.settings.vehicle, screen, onSwitchVehicle)
        VehicleIllustration(
            dashboard.settings.vehicle,
            screen.dp(0.2f),
            glow = true,
            modifier = Modifier.clickable(
                onClickLabel = stringResource(R.string.home_switch_to, stringResource(dashboard.settings.vehicle.other.label)),
                onClick = onSwitchVehicle,
            ),
        )
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

/**
 * "Mi carro ⇄" o "Mi moto ⇄": tocarlo pasa al otro vehículo, con sus propios datos. Mide al menos
 * [MinTouchSize] de alto aunque el texto sea pequeño.
 */
@Composable
internal fun VehicleTitle(vehicle: VehicleType, screen: ScreenSize, onSwitch: () -> Unit) {
    val switchLabel = stringResource(R.string.home_switch_to, stringResource(vehicle.other.label))
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClickLabel = switchLabel, role = Role.Button, onClick = onSwitch)
            .heightIn(min = MinTouchSize)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(vehicle.homeTitle),
                color = TanqueoColors.White,
                fontSize = screen.sp(13f),
                lineHeight = screen.sp(15f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Icon(
                painter = painterResource(R.drawable.ic_swap),
                contentDescription = null,
                tint = TanqueoColors.Accent,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(screen.dp(0.075f)),
            )
        }
        Box(
            Modifier
                .padding(top = 3.dp)
                .width(screen.dp(0.46f))
                .height(1.dp)
                .background(TanqueoColors.Accent),
        )
    }
}

private val VehicleType.homeTitle: Int
    get() = when (this) {
        VehicleType.CARRO -> R.string.home_title_car
        VehicleType.MOTO -> R.string.home_title_moto
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
