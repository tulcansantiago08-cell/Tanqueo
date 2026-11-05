package com.tanqueo.ui.summary

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.domain.tip.MonthTip
import com.tanqueo.domain.format.Formats
import com.tanqueo.ui.components.BigValueCard
import com.tanqueo.ui.components.GlassCard
import com.tanqueo.ui.components.ListScreen
import com.tanqueo.ui.components.edgeAware
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.theme.TanqueoColors

@Composable
fun SummaryScreen(viewModel: SummaryViewModel = hiltViewModel()) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val tip by viewModel.tip.collectAsStateWithLifecycle()
    ListScreen {
        val data = dashboard ?: return@ListScreen
        val month = data.month
        tip?.let { current ->
            item { TipCard(current, Modifier.edgeAware(this)) }
        }
        item {
            BigValueCard(
                modifier = Modifier.edgeAware(this),
                label = stringResource(R.string.summary_month_spend),
                value = Formats.cop(month.totalSpentCop),
            )
        }
        item {
            BigValueCard(
                modifier = Modifier.edgeAware(this),
                label = stringResource(R.string.summary_efficiency),
                value = data.kmPerGallon?.let { Formats.oneDecimal(it) } ?: stringResource(R.string.value_empty),
                unit = data.kmPerGallon?.let { stringResource(R.string.unit_km_per_gallon) },
            )
        }
        item {
            BigValueCard(
                modifier = Modifier.edgeAware(this),
                label = stringResource(R.string.summary_average_price),
                value = month.averagePricePerGallon?.let { Formats.cop(it) } ?: stringResource(R.string.value_empty),
            )
        }
        item {
            BigValueCard(
                modifier = Modifier.edgeAware(this),
                label = stringResource(R.string.summary_count),
                value = month.fillUpCount.toString(),
            )
        }
        item {
            BigValueCard(
                modifier = Modifier.edgeAware(this),
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

@Composable
private fun TipCard(tip: TipUi, modifier: Modifier) {
    val screen = rememberScreenSize()
    GlassCard(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tip),
                contentDescription = stringResource(R.string.tip_title),
                tint = TanqueoColors.Accent,
                modifier = Modifier
                    .padding(end = 7.dp)
                    .size(screen.dp(0.1f)),
            )
            Text(
                text = tip.written ?: localTipText(tip.local),
                color = TanqueoColors.White,
                fontSize = screen.sp(12f),
                lineHeight = screen.sp(15f),
            )
        }
    }
}

@Composable
private fun localTipText(tip: MonthTip): String = when (tip) {
    is MonthTip.PremiumCostsMore -> stringResource(R.string.tip_premium, Formats.cop(tip.extraPerGallon))
    is MonthTip.SpendingUp -> stringResource(R.string.tip_spending_up, tip.percent)
    is MonthTip.SpendingDown -> stringResource(R.string.tip_spending_down, tip.percent)
    is MonthTip.EfficiencyDrop -> stringResource(R.string.tip_efficiency_drop, tip.percent)
    MonthTip.KeepLogging -> stringResource(R.string.tip_keep_logging)
}
