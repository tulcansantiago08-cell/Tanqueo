package com.tanqueo.ui.components

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.tanqueo.R
import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.calc.RangeEstimator
import com.tanqueo.domain.format.DaysDisplay
import com.tanqueo.domain.format.Formats
import java.time.LocalDate

/** Textos de la estimación, compartidos por las pantallas, la tile y la notificación. */
fun Resources.rangeKm(km: Double): String = getString(R.string.range_km, Formats.roundedKm(km))

fun Resources.rangeDays(days: Double): String = when (val display = Formats.days(days)) {
    DaysDisplay.LessThanOne -> getString(R.string.range_days_less_than_one)
    is DaysDisplay.About -> getQuantityString(R.plurals.range_days, display.days, display.days)
}

/** "≈ 578 km · unos 9 días", o solo los km si aún no hay promedio diario. */
fun Resources.rangeFull(estimate: RangeEstimate.Available): String {
    val km = rangeKm(estimate.kmRemaining)
    val days = estimate.daysRemaining ?: return km
    return getString(R.string.range_full, km, rangeDays(days))
}

/** "tanquea ≈ dom 15", o "tanquea hoy" si no alcanza un día completo. */
@Composable
fun refuelDateText(days: Double): String {
    val today = LocalDate.now()
    val date = RangeEstimator.refuelDate(today, days)
    return if (date == today) {
        stringResource(R.string.range_refuel_today)
    } else {
        stringResource(R.string.range_refuel_on, Formats.dayDate(date))
    }
}

@Composable
fun rangeKmText(km: Double): String = LocalResources.current.rangeKm(km)

@Composable
fun rangeDaysText(days: Double): String = LocalResources.current.rangeDays(days)
