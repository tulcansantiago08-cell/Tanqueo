package com.tanqueo.complication

import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.format.Formats

/** Lo que muestra la complicación. Sin estimación no hay cifra: se muestra un guion. */
data class RangeComplicationContent(
    val text: String,
    val tankPercent: Float,
    val hasData: Boolean,
) {
    companion object {
        private const val NO_DATA_TEXT = "—"

        fun from(estimate: RangeEstimate): RangeComplicationContent = when (estimate) {
            RangeEstimate.Insufficient -> RangeComplicationContent(NO_DATA_TEXT, 0f, hasData = false)
            is RangeEstimate.Available -> RangeComplicationContent(
                text = Formats.roundedKm(estimate.kmRemaining),
                tankPercent = (estimate.tankFraction * 100).toFloat(),
                hasData = true,
            )
        }
    }
}
