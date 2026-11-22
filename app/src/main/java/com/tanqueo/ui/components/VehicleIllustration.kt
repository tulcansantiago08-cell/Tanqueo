package com.tanqueo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.tanqueo.R
import com.tanqueo.domain.model.VehicleType

/** El carro visto desde arriba o la moto de lado. Los dos dibujos miden lo mismo. */
@Composable
fun VehicleIllustration(
    vehicle: VehicleType,
    height: Dp,
    glow: Boolean,
    modifier: Modifier = Modifier,
) {
    val (drawable, description) = when (vehicle) {
        VehicleType.CARRO -> R.drawable.ic_car_top to R.string.car_illustration
        VehicleType.MOTO -> R.drawable.ic_moto_side to R.string.moto_illustration
    }
    Image(
        painter = painterResource(drawable),
        contentDescription = stringResource(description),
        modifier = modifier
            .then(if (glow) Modifier.softGlow() else Modifier)
            .size(width = height * 1.8f, height = height),
    )
}
