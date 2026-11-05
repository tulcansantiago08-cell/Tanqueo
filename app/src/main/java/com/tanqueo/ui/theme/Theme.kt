package com.tanqueo.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import com.tanqueo.domain.calc.RangeLevel

object TanqueoColors {
    /** Negro puro: elegante y ahorra batería en pantallas OLED. */
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)

    /** Etiquetas pequeñas. */
    val Secondary = Color(0xFF9A9AA5)

    /** Único color de acento: acción principal, títulos y brillos. */
    val Accent = Color(0xFF7C5CFF)

    /** Final del degradado del anillo; no se usa en ningún otro lado. */
    val RingEnd = Color(0xFF4AC0FC)

    val Amber = Color(0xFFFFB020)
    val Red = Color(0xFFFF5A4F)

    val RingTrack = Color(0x24FFFFFF)
    val GlassTop = Color(0x2EFFFFFF)
    val GlassBottom = Color(0x17FFFFFF)
    val GlassBorder = Color(0x40FFFFFF)
}

/** Color de la cifra y del anillo según qué tan poca autonomía queda. */
fun RangeLevel.alertColor(normal: Color): Color = when (this) {
    RangeLevel.NORMAL -> normal
    RangeLevel.LOW -> TanqueoColors.Amber
    RangeLevel.CRITICAL -> TanqueoColors.Red
}

private val colors = ColorScheme(
    primary = TanqueoColors.Accent,
    onPrimary = TanqueoColors.White,
    primaryContainer = TanqueoColors.Accent,
    onPrimaryContainer = TanqueoColors.White,
    background = TanqueoColors.Black,
    onBackground = TanqueoColors.White,
    onSurface = TanqueoColors.White,
    onSurfaceVariant = TanqueoColors.Secondary,
)

@Composable
fun TanqueoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
