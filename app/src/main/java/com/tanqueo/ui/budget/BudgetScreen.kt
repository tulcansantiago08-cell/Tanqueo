package com.tanqueo.ui.budget

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.domain.budget.FuelBudget
import com.tanqueo.domain.budget.ReferencePrices
import com.tanqueo.domain.format.Formats
import com.tanqueo.ui.components.ValueCardModel
import com.tanqueo.ui.components.ValueCardsScreen
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.theme.TanqueoColors

private val MONEY_STEPS = listOf(1, 10) // en múltiplos de $ 1.000

/** "¿Cuánto me alcanza?": el dinero y el precio arriba; los km y galones siempre a la vista abajo. */
@Composable
fun BudgetScreen(viewModel: BudgetViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    var step by rememberSaveable { mutableIntStateOf(1) }
    val option = state.option

    val cards = listOf(
        ValueCardModel(
            label = stringResource(R.string.budget_money),
            value = Formats.cop(state.moneyCop),
            onSteps = { viewModel.adjustMoney(it * MONEY_STEPS[step]) },
            hint = stringResource(R.string.hint_step, Formats.cop(MONEY_STEPS[step] * 1_000L)),
            onHintClick = { step = (step + 1) % MONEY_STEPS.size },
        ),
        ValueCardModel(
            label = if (option?.place != null) {
                stringResource(R.string.budget_reference_price, ReferencePrices.VALID_FOR)
            } else {
                stringResource(R.string.budget_own_price)
            },
            value = option?.let { Formats.cop(it.pricePerGallon) } ?: stringResource(R.string.value_empty),
            onSteps = viewModel::selectPrice,
            decreaseIcon = R.drawable.ic_chevron_left,
            increaseIcon = R.drawable.ic_chevron_right,
            hint = option?.place ?: stringResource(R.string.budget_own_place),
        ),
    )

    ScreenScaffold {
        ValueCardsScreen(
            pagerState = pagerState,
            cards = cards,
            bottomAction = { BudgetResult(state.result) },
        )
    }
}

@Composable
private fun BudgetResult(result: FuelBudget?) {
    val screen = rememberScreenSize()
    val text = when {
        result == null -> stringResource(R.string.value_empty)
        result.km == null -> stringResource(R.string.value_gallons, Formats.oneDecimal(result.gallons))
        result.exceedsTank -> stringResource(R.string.budget_result_full, Formats.roundedKm(result.km))
        else -> stringResource(
            R.string.budget_result,
            Formats.roundedKm(result.km),
            Formats.oneDecimal(result.gallons),
        )
    }
    Text(
        text = text,
        color = TanqueoColors.White,
        fontSize = screen.sp(15f),
        lineHeight = screen.sp(18f),
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier.heightIn(min = screen.dp(0.12f)),
    )
}
