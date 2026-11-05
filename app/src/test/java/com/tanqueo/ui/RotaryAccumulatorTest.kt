package com.tanqueo.ui

import com.tanqueo.ui.components.RotaryAccumulator
import org.junit.Assert.assertEquals
import org.junit.Test

class RotaryAccumulatorTest {
    @Test fun `la corona acumula pixeles hasta completar un paso`() {
        val rotary = RotaryAccumulator(pixelsPerStep = 30f)
        assertEquals(0, rotary.add(20f))
        assertEquals(1, rotary.add(20f))
        assertEquals(0, rotary.add(15f))
        assertEquals(1, rotary.add(15f))
    }

    @Test fun `una muesca grande del bisel es un solo paso`() {
        val rotary = RotaryAccumulator(pixelsPerStep = 30f)
        assertEquals(1, rotary.add(240f))
        assertEquals(-1, rotary.add(-240f))
    }

    @Test fun `cambiar de sentido descarta lo acumulado`() {
        val rotary = RotaryAccumulator(pixelsPerStep = 30f)
        assertEquals(0, rotary.add(25f))
        assertEquals(0, rotary.add(-10f))
        assertEquals(-1, rotary.add(-20f))
    }
}
