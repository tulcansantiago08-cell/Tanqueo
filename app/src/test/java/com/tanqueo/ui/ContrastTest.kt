package com.tanqueo.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.tanqueo.ui.theme.TanqueoColors
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contraste WCAG de la paleta: 4,5:1 para texto normal y 3:1 para números y botones grandes. */
class ContrastTest {
    private fun contrast(foreground: Color, background: Color): Double {
        val a = foreground.luminance().toDouble()
        val b = background.luminance().toDouble()
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    private val glass = TanqueoColors.GlassTop.compositeOver(TanqueoColors.Black)

    @Test fun `el texto se lee sobre negro`() {
        assertTrue(contrast(TanqueoColors.White, TanqueoColors.Black) >= 4.5)
        assertTrue(contrast(TanqueoColors.Secondary, TanqueoColors.Black) >= 4.5)
        assertTrue(contrast(TanqueoColors.Amber, TanqueoColors.Black) >= 4.5)
        assertTrue(contrast(TanqueoColors.Red, TanqueoColors.Black) >= 4.5)
    }

    @Test fun `el gasto en violeta se lee sobre negro`() {
        assertTrue(contrast(TanqueoColors.Accent, TanqueoColors.Black) >= 4.5)
    }

    @Test fun `las etiquetas grises se leen sobre el vidrio`() {
        assertTrue(contrast(TanqueoColors.Secondary, glass) >= 4.5)
        assertTrue(contrast(TanqueoColors.White, glass) >= 4.5)
    }

    @Test fun `el texto del boton violeta cumple para texto grande`() {
        assertTrue(contrast(TanqueoColors.White, TanqueoColors.Accent) >= 3.0)
    }
}
