package com.tanqueo.ui.components

import android.graphics.Paint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import com.tanqueo.ui.theme.TanqueoColors
import kotlin.math.max

/** Brillo suave y redondo detrás de un elemento, como el carro. Se sale de sus límites a propósito. */
fun Modifier.softGlow(
    color: Color = TanqueoColors.Accent,
    strength: Float = 0.38f,
    radiusFactor: Float = 0.62f,
): Modifier = drawBehind {
    val radius = max(size.width, size.height) * radiusFactor
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = strength), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
    )
}

/** Resplandor del botón principal, con la misma forma de píldora. */
fun Modifier.pillGlow(glowRadius: Dp, color: Color = TanqueoColors.Accent): Modifier = drawBehind {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        setShadowLayer(glowRadius.toPx(), 0f, 0f, color.copy(alpha = 0.75f).toArgb())
    }
    drawIntoCanvas {
        it.nativeCanvas.drawRoundRect(
            0f,
            0f,
            size.width,
            size.height,
            size.height / 2,
            size.height / 2,
            paint,
        )
    }
}
