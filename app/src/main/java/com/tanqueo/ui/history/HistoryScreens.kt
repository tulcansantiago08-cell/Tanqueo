package com.tanqueo.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.domain.format.Formats
import com.tanqueo.domain.model.FillUp
import com.tanqueo.ui.components.BigValueCard
import com.tanqueo.ui.components.GlassCard
import com.tanqueo.ui.components.GlassPillButton
import com.tanqueo.ui.components.edgeAware
import com.tanqueo.ui.components.ListScreen
import com.tanqueo.ui.components.labelRes
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.theme.TanqueoColors
import java.time.ZoneId

@Composable
fun HistoryScreen(onOpen: (Long) -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val fillUps by viewModel.fillUps.collectAsStateWithLifecycle()
    ListScreen {
        val list = fillUps
        if (list != null && list.isEmpty()) {
            item { EmptyHistory() }
        }
        items(list.orEmpty().size) { index ->
            val fillUp = list.orEmpty()[index]
            FillUpRow(fillUp, modifier = Modifier.edgeAware(this), onClick = { onOpen(fillUp.id) })
        }
    }
}

@Composable
private fun EmptyHistory() {
    val screen = rememberScreenSize()
    Text(
        text = stringResource(R.string.history_empty),
        color = TanqueoColors.Secondary,
        fontSize = screen.sp(13f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    )
}

@Composable
private fun FillUpRow(fillUp: FillUp, modifier: Modifier, onClick: () -> Unit) {
    val screen = rememberScreenSize()
    val date = fillUp.at.atZone(ZoneId.systemDefault())
    GlassCard(modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = Formats.shortDate(date.toLocalDate()),
                color = TanqueoColors.Secondary,
                fontSize = screen.sp(11f),
                lineHeight = screen.sp(13f),
                maxLines = 1,
            )
            Text(
                text = Formats.cop(fillUp.paidCop),
                color = TanqueoColors.White,
                fontSize = screen.sp(19f),
                lineHeight = screen.sp(22f),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = stringResource(
                    R.string.history_row_detail,
                    Formats.oneDecimal(fillUp.gallons),
                    stringResource(if (fillUp.isFull) R.string.fill_full else R.string.fill_partial),
                ),
                color = TanqueoColors.Secondary,
                fontSize = screen.sp(11f),
                lineHeight = screen.sp(13f),
                maxLines = 1,
            )
        }
    }
}

@Composable
fun FillUpDetailScreen(onDeleted: () -> Unit, viewModel: FillUpDetailViewModel = hiltViewModel()) {
    val fillUp by viewModel.fillUp.collectAsStateWithLifecycle()
    val isDeleted by viewModel.isDeleted.collectAsStateWithLifecycle()
    var confirming by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isDeleted) {
        if (isDeleted) onDeleted()
    }

    ListScreen {
        val item = fillUp ?: return@ListScreen
        val date = item.at.atZone(ZoneId.systemDefault())
        item {
            BigValueCard(
                modifier = Modifier.edgeAware(this),
                label = stringResource(
                    R.string.detail_date,
                    Formats.shortDate(date.toLocalDate()),
                    Formats.time(date.toLocalDateTime()),
                ),
                value = Formats.cop(item.paidCop),
            )
        }
        item {
            BigValueCard(
                modifier = Modifier.edgeAware(this),
                label = stringResource(R.string.card_gallons),
                value = Formats.oneDecimal(item.gallons),
                unit = stringResource(R.string.unit_gal),
            )
        }
        item {
            BigValueCard(
                modifier = Modifier.edgeAware(this),
                label = stringResource(R.string.detail_price_per_gallon),
                value = Formats.cop(item.paidCop / item.gallons),
            )
        }
        item {
            BigValueCard(
                modifier = Modifier.edgeAware(this),
                label = stringResource(R.string.card_fuel),
                value = stringResource(item.fuelType.labelRes),
            )
        }
        item {
            BigValueCard(
                modifier = Modifier.edgeAware(this),
                label = stringResource(R.string.card_fill_kind),
                value = stringResource(if (item.isFull) R.string.fill_full else R.string.fill_partial),
            )
        }
        item {
            BigValueCard(
                modifier = Modifier.edgeAware(this),
                label = stringResource(R.string.card_odometer),
                value = item.odometerKm?.let { Formats.integer(it) } ?: stringResource(R.string.value_empty),
                unit = item.odometerKm?.let { stringResource(R.string.unit_km) },
            )
        }
        item {
            // Sin diálogo: el primer toque pide confirmar y el segundo borra.
            GlassPillButton(
                modifier = Modifier.edgeAware(this).fillMaxWidth(),
                text = stringResource(if (confirming) R.string.action_delete_confirm else R.string.action_delete),
                icon = R.drawable.ic_delete,
                onClick = { if (confirming) viewModel.delete() else confirming = true },
            )
        }
    }
}
