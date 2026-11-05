package com.tanqueo.domain.calc

import com.tanqueo.domain.model.TankSettings

object ReminderPolicy {
    /** Avisa si quedan menos km o menos días que los umbrales. Sin estimación nunca avisa. */
    fun shouldNotify(estimate: RangeEstimate, settings: TankSettings): Boolean {
        if (estimate !is RangeEstimate.Available) return false
        if (estimate.kmRemaining < settings.reminderKmThreshold) return true
        val days = estimate.daysRemaining ?: return false
        return days < settings.reminderDaysThreshold
    }
}
