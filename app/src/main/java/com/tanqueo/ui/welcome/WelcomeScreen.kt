package com.tanqueo.ui.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.domain.model.VehicleType
import com.tanqueo.ui.components.GlassCard
import com.tanqueo.ui.components.MinTouchSize
import com.tanqueo.ui.components.VehicleIllustration
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.theme.TanqueoColors

/** Primera pantalla: qué se va a tanquear. Se puede cambiar después en Ajustes. */
@Composable
fun WelcomeScreen(onChoose: (VehicleType) -> Unit) {
    val screen = rememberScreenSize()
    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = screen.sideMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.welcome_title),
                color = TanqueoColors.Accent,
                fontSize = screen.sp(11f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.welcome_question),
                color = TanqueoColors.White,
                fontSize = screen.sp(14f),
                lineHeight = screen.sp(17f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp, bottom = screen.dp(0.04f)),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(screen.dp(0.04f))) {
                VehicleType.entries.forEach { vehicle ->
                    VehicleOption(vehicle, onClick = { onChoose(vehicle) })
                }
            }
            Text(
                text = stringResource(R.string.welcome_hint),
                color = TanqueoColors.Secondary,
                fontSize = screen.sp(10f),
                lineHeight = screen.sp(13f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(top = screen.dp(0.04f)),
            )
        }
    }
}

@Composable
private fun VehicleOption(vehicle: VehicleType, onClick: () -> Unit) {
    val screen = rememberScreenSize()
    GlassCard(
        onClick = onClick,
        modifier = Modifier
            .width(screen.dp(0.38f))
            .heightIn(min = MinTouchSize)
            .semantics { role = Role.Button },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = screen.dp(0.03f)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VehicleIllustration(vehicle, screen.dp(0.12f), glow = false)
            Text(
                text = stringResource(vehicle.label),
                color = TanqueoColors.White,
                fontSize = screen.sp(14f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Nombre del vehículo para mostrar. */
val VehicleType.label: Int
    get() = when (this) {
        VehicleType.CARRO -> R.string.vehicle_car
        VehicleType.MOTO -> R.string.vehicle_moto
    }
