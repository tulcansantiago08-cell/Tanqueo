package com.tanqueo.ui.components

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.tanqueo.domain.calc.RangeLevel
import com.tanqueo.ui.theme.TanqueoColors
import com.tanqueo.ui.theme.alertColor

/**
 * El medidor abre abajo: ahí se apoya el botón, sin taparle el arranque, que es lo único que se ve
 * cuando queda poca autonomía. Los ángulos cuentan desde las 3 en punto.
 */
private const val START_ANGLE = 150f
private const val FULL_SWEEP = 240f

/** Pulso para llamar la atención cuando queda poca autonomía. Normal: siempre 1. */
@Composable
fun rememberAlertPulse(level: RangeLevel): Float {
    if (level == RangeLevel.NORMAL) return 1f
    val transition = rememberInfiniteTransition(label = "pulso")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(850, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulso",
    )
    return pulse
}

/**
 * Anillo tipo medidor. Con nivel normal lleva el degradado violeta → azul y un resplandor; si queda
 * poca autonomía es de un solo color, ámbar o rojo. [fraction] va de 0 a 1.
 */
@Composable
fun RangeRing(fraction: Float, level: RangeLevel, modifier: Modifier = Modifier) {
    val pulse = rememberAlertPulse(level)
    Canvas(modifier.fillMaxSize()) {
        val strokeWidth = size.minDimension * 0.085f
        val inset = strokeWidth * 1.1f
        val oval = RectF(inset, inset, size.width - inset, size.height - inset)
        val alert = level.alertColor(TanqueoColors.Accent)

        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            this.strokeWidth = strokeWidth
            color = TanqueoColors.RingTrack.toArgb()
        }
        val indicator = Paint(track).apply {
            alpha = (255 * pulse).toInt()
            color = alert.copy(alpha = pulse).toArgb()
            setShadowLayer(strokeWidth * 0.9f, 0f, 0f, alert.copy(alpha = 0.7f * pulse).toArgb())
            if (level == RangeLevel.NORMAL) shader = ringGradient(size.width / 2, size.height / 2)
        }
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawArc(oval, START_ANGLE, FULL_SWEEP, false, track)
            if (fraction > 0f) {
                canvas.nativeCanvas.drawArc(oval, START_ANGLE, FULL_SWEEP * fraction, false, indicator)
            }
        }
    }
}

/** Vuelve a violeta al final de la vuelta para que la punta redondeada inicial no salga azul. */
private fun ringGradient(cx: Float, cy: Float): SweepGradient {
    val violet = TanqueoColors.Accent.toArgb()
    val blue = TanqueoColors.RingEnd.toArgb()
    return SweepGradient(
        cx,
        cy,
        intArrayOf(violet, blue, violet),
        floatArrayOf(0f, FULL_SWEEP / 360f, 1f),
    ).apply {
        setLocalMatrix(Matrix().apply { setRotate(START_ANGLE, cx, cy) })
    }
}

