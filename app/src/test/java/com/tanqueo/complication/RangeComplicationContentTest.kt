package com.tanqueo.complication

import com.tanqueo.domain.calc.RangeEstimate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RangeComplicationContentTest {
    @Test fun `sin estimacion muestra un guion y el arco vacio`() {
        val content = RangeComplicationContent.from(RangeEstimate.Insufficient)
        assertEquals("—", content.text)
        assertEquals(0f, content.tankPercent, 0f)
        assertFalse(content.hasData)
    }

    @Test fun `con estimacion muestra los km redondeados y el porcentaje del tanque`() {
        val estimate = RangeEstimate.Available(
            kmRemaining = 1_384.6,
            daysRemaining = 9.0,
            kmPerGallon = 40.0,
            kmPerDay = 40.0,
            gallonsInTank = 10.0,
            kmSinceLastFillUp = 0.0,
            usesManualOdometer = false,
            tankFraction = 0.75,
        )
        val content = RangeComplicationContent.from(estimate)
        assertEquals("1.385", content.text)
        assertEquals(75f, content.tankPercent, 0.001f)
        assertTrue(content.hasData)
    }
}
