package com.tanqueo.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Medidas proporcionales al lado corto, para verse igual en relojes redondos y cuadrados. */
data class ScreenSize(val sideDp: Int, val isRound: Boolean) {
    /** Fracción del lado corto, en dp. */
    fun dp(fraction: Float): Dp = (sideDp * fraction).dp

    /** Tamaño de texto de diseño (pensado para 180 dp) escalado a esta pantalla. */
    fun sp(designSp: Float): TextUnit = (designSp * sideDp / DESIGN_SIDE_DP).sp

    /** Margen lateral: en pantallas redondas hay menos ancho arriba y abajo. */
    val sideMargin: Dp get() = dp(if (isRound) 0.07f else 0.04f)

    /** Separación superior del encabezado: en lo redondo baja para que no lo corte el círculo. */
    val headerTopPadding: Dp get() = dp(if (isRound) 0.07f else 0.02f)

    /** Dónde empieza el contenido debajo del encabezado. */
    val contentTop: Dp get() = dp(if (isRound) 0.215f else 0.175f)

    /** Altura de la fila inferior de las pantallas de tarjetas: en lo redondo debe caber en la curva. */
    val bottomRowHeight: Dp get() = dp(if (isRound) 0.15f else 0.19f)

    /** Separación bajo la fila inferior. */
    val bottomRowPadding: Dp get() = dp(if (isRound) 0.075f else 0.025f)

    private companion object {
        const val DESIGN_SIDE_DP = 180f
    }
}

@Composable
fun rememberScreenSize(): ScreenSize {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        ScreenSize(
            sideDp = minOf(configuration.screenWidthDp, configuration.screenHeightDp),
            isRound = configuration.isScreenRound,
        )
    }
}
