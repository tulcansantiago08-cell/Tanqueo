package com.tanqueo.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

object TanqueoColors {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val Secondary = Color(0xFFB9B9C2)
    val Tertiary = Color(0xFF85858F)

    /** Violeta reservado para la acción principal de cada pantalla. */
    val Accent = Color(0xFF7C5CFF)

    val GlassTop = Color(0x2EFFFFFF)
    val GlassBottom = Color(0x17FFFFFF)
    val GlassBorder = Color(0x40FFFFFF)
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
