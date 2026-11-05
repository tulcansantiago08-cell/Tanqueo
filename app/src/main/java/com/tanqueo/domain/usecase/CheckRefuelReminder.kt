package com.tanqueo.domain.usecase

import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.calc.ReminderPolicy
import javax.inject.Inject

/** Revisa la estimación actual y devuelve lo que hay que avisar, o null si no toca avisar. */
class CheckRefuelReminder @Inject constructor(private val observeDashboard: ObserveDashboard) {
    suspend operator fun invoke(): RangeEstimate.Available? {
        val dashboard = observeDashboard.current()
        return (dashboard.estimate as? RangeEstimate.Available)
            ?.takeIf { ReminderPolicy.shouldNotify(it, dashboard.settings) }
    }
}
