package com.tanqueo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

private const val COUNT_UP_MILLIS = 1_000
private const val FADE_IN_MILLIS = 320

/**
 * Sube de 0 a [target] cuando [play] pasa a verdadero, para que los números cuenten y el anillo se
 * llene al abrir la página. Mientras [play] sea falso se queda en cero, listo para la próxima vez.
 * Si [target] cambia estando visible (la autonomía baja con los minutos), no vuelve a contar desde 0.
 */
@Composable
fun rememberCountUp(target: Float, play: Boolean = true): Float {
    val value = remember { Animatable(0f) }
    LaunchedEffect(target, play) {
        if (play) {
            value.animateTo(target, tween(COUNT_UP_MILLIS, easing = FastOutSlowInEasing))
        } else {
            value.snapTo(0f)
        }
    }
    return value.value
}

/** Fundido leve al aparecer. */
@Composable
fun Modifier.fadeInOnAppear(): Modifier {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alpha.animateTo(1f, tween(FADE_IN_MILLIS)) }
    return graphicsLayer { this.alpha = alpha.value }
}
