package com.tanqueo.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive

/**
 * Convierte los píxeles de la corona o el bisel en pasos enteros. La corona manda muchos eventos
 * pequeños y se acumulan; el bisel manda un evento grande por muesca y cuenta como un solo paso.
 */
class RotaryAccumulator(private val pixelsPerStep: Float = DEFAULT_PIXELS_PER_STEP) {
    private var pending = 0f

    fun add(pixels: Float): Int {
        if (pending != 0f && (pending > 0) != (pixels > 0)) pending = 0f
        if (kotlin.math.abs(pixels) >= pixelsPerStep * DETENT_FACTOR) {
            pending = 0f
            return if (pixels > 0) 1 else -1
        }
        pending += pixels
        val steps = (pending / pixelsPerStep).toInt()
        pending -= steps * pixelsPerStep
        return steps
    }

    companion object {
        const val DEFAULT_PIXELS_PER_STEP = 30f
        private const val DETENT_FACTOR = 1.5f
    }
}

/** Entrega a [onSteps] los pasos de la corona o el bisel mientras esta pantalla está activa. */
@Composable
fun Modifier.rotarySteps(onSteps: (Int) -> Unit): Modifier {
    val accumulator = remember { RotaryAccumulator() }
    val focusRequester = remember { FocusRequester() }
    return this
        .onRotaryScrollEvent { event ->
            val steps = accumulator.add(event.verticalScrollPixels)
            if (steps != 0) onSteps(steps)
            true
        }
        .focusRequester(focusRequester)
        .requestFocusOnHierarchyActive()
        .focusable()
}
