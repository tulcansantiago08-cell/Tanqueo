package com.tanqueo.domain

import com.tanqueo.domain.calc.RangeLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class RangeLevelTest {
    @Test fun `con 50 km o mas el nivel es normal`() {
        assertEquals(RangeLevel.NORMAL, RangeLevel.of(50.0))
        assertEquals(RangeLevel.NORMAL, RangeLevel.of(385.0))
    }

    @Test fun `con menos de 50 km el nivel es bajo`() {
        assertEquals(RangeLevel.LOW, RangeLevel.of(49.9))
        assertEquals(RangeLevel.LOW, RangeLevel.of(20.0))
    }

    @Test fun `con menos de 20 km el nivel es critico`() {
        assertEquals(RangeLevel.CRITICAL, RangeLevel.of(19.9))
        assertEquals(RangeLevel.CRITICAL, RangeLevel.of(0.0))
    }
}
