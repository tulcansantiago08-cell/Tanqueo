package com.tanqueo.ui.summary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tanqueo.R
import com.tanqueo.domain.format.Formats
import com.tanqueo.ui.components.BigValueCard
import com.tanqueo.ui.components.ListScreen

@Composable
fun SummaryScreen(viewModel: SummaryViewModel = hiltViewModel()) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    ListScreen {
        val data = dashboard ?: return@ListScreen
        val month = data.month
        item {
            BigValueCard(
                label = stringResource(R.string.summary_month_spend),
                value = Formats.cop(month.totalSpentCop),
            )
        }
        item {
            BigValueCard(
                label = stringResource(R.string.summary_efficiency),
                value = data.kmPerGallon?.let { Formats.oneDecimal(it) } ?: stringResource(R.string.value_empty),
                unit = data.kmPerGallon?.let { stringResource(R.string.unit_km_per_gallon) },
            )
        }
        item {
            BigValueCard(
                label = stringResource(R.string.summary_average_price),
                value = month.averagePricePerGallon?.let { Formats.cop(it) } ?: stringResource(R.string.value_empty),
            )
        }
        item {
            BigValueCard(
                label = stringResource(R.string.summary_count),
                value = month.fillUpCount.toString(),
            )
        }
        item {
            BigValueCard(
                label = stringResource(R.string.summary_days_since),
                value = when (val days = month.daysSinceLastFillUp) {
                    null -> stringResource(R.string.value_empty)
                    0L -> stringResource(R.string.summary_today)
                    else -> pluralStringResource(R.plurals.summary_days_ago, days.toInt(), days.toInt())
                },
            )
        }
    }
}
