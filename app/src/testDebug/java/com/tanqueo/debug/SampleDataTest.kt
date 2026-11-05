package com.tanqueo.debug

import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.calc.RangeEstimator
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleDataTest {
    private val now = Instant.parse("2026-11-04T19:00:00Z")
    private val data = SampleData.fillUps(now)

    @Test fun `son seis tanqueos con kilometraje creciente`() {
        assertEquals(6, data.size)
        val km = data.sortedBy { it.at }.map { it.odometerKm!! }
        assertEquals(km.sorted(), km)
    }

    @Test fun `permiten una estimacion realista`() {
        val estimate = RangeEstimator.estimate(data, 12.0, null, now) as RangeEstimate.Available
        assertEquals(42.0, estimate.kmPerGallon, 0.5)
        assertEquals(48.7, estimate.kmPerDay!!, 0.5)
        assertEquals(11.1, estimate.gallonsInTank, 0.1)
        assertTrue(estimate.kmRemaining in 400.0..440.0)
    }
}
