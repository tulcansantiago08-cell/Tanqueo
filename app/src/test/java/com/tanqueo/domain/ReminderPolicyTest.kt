package com.tanqueo.domain

import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.calc.ReminderPolicy
import com.tanqueo.domain.model.TankSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPolicyTest {
    private val defaults = TankSettings()

    private fun estimate(km: Double, days: Double?) = RangeEstimate.Available(
        kmRemaining = km,
        daysRemaining = days,
        kmPerGallon = 40.0,
        kmPerDay = 40.0,
        gallonsInTank = 5.0,
        kmSinceLastFillUp = 0.0,
        usesManualOdometer = false,
    )

    @Test fun `sin estimacion no avisa`() {
        assertFalse(ReminderPolicy.shouldNotify(RangeEstimate.Insufficient, defaults))
    }

    @Test fun `avisa con menos km que el umbral`() {
        assertTrue(ReminderPolicy.shouldNotify(estimate(49.9, 5.0), defaults))
    }

    @Test fun `avisa con menos dias que el umbral`() {
        assertTrue(ReminderPolicy.shouldNotify(estimate(400.0, 0.9), defaults))
    }

    @Test fun `no avisa justo en los umbrales`() {
        assertFalse(ReminderPolicy.shouldNotify(estimate(50.0, 1.0), defaults))
    }

    @Test fun `respeta umbrales configurados`() {
        val custom = TankSettings(reminderKmThreshold = 200, reminderDaysThreshold = 3)
        assertTrue(ReminderPolicy.shouldNotify(estimate(150.0, 10.0), custom))
        assertTrue(ReminderPolicy.shouldNotify(estimate(500.0, 2.0), custom))
        assertFalse(ReminderPolicy.shouldNotify(estimate(500.0, 10.0), custom))
    }

    @Test fun `sin dias conocidos decide solo por km`() {
        assertFalse(ReminderPolicy.shouldNotify(estimate(100.0, null), defaults))
        assertTrue(ReminderPolicy.shouldNotify(estimate(10.0, null), defaults))
    }
}
