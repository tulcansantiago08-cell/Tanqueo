package com.tanqueo.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.tanqueo.R
import com.tanqueo.domain.format.DaysDisplay
import com.tanqueo.domain.format.Formats

@Composable
fun rangeKmText(km: Double): String = stringResource(R.string.range_km, Formats.roundedKm(km))

@Composable
fun rangeDaysText(days: Double): String = when (val display = Formats.days(days)) {
    DaysDisplay.LessThanOne -> stringResource(R.string.range_days_less_than_one)
    is DaysDisplay.About -> pluralStringResource(R.plurals.range_days, display.days, display.days)
}
