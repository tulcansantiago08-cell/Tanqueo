package com.tanqueo.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.ScreenScaffold
import com.tanqueo.R
import com.tanqueo.domain.budget.FuelBudget
import com.tanqueo.domain.budget.ReferencePrices
import com.tanqueo.domain.format.Formats
import com.tanqueo.ui.components.ValueCardModel
import com.tanqueo.ui.components.ValueCardsScreen
import com.tanqueo.ui.components.rangeDaysText
import com.tanqueo.ui.components.rangeKmText
import com.tanqueo.ui.components.rememberTextInputLauncher
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.theme.TanqueoColors

private val MONEY_STEPS = listOf(1, 10) // en múltiplos de $ 1.000

/** "¿Cuánto me alcanza?": el valor y el precio arriba; los km, los días y los galones siempre a la vista abajo. */
@Composable
fun BudgetScreen(viewModel: BudgetViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    var step by rememberSaveable { mutableIntStateOf(1) }
    val option = state.option
    val typeMoney = rememberTextInputLauncher(
        prompt = stringResource(R.string.budget_type_prompt),
        onResult = viewModel::onMoneyTyped,
        onUnavailable = {},
    )

    val cards = listOf(
        ValueCardModel(
            label = stringResource(R.string.budget_money),
            value = Formats.cop(state.moneyCop),
            onSteps = { viewModel.adjustMoney(it * MONEY_STEPS[step]) },
            hint = if (state.isMoneyInvalid) {
                stringResource(R.string.budget_invalid_money)
            } else {
                stringResource(R.string.hint_step, Formats.cop(MONEY_STEPS[step] * 1_000L))
            },
            onHintClick = { step = (step + 1) % MONEY_STEPS.size },
            onValueClick = typeMoney,
            valueClickLabel = stringResource(R.string.budget_type_value),
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
    val km = result?.km
    val headline = when {
        result == null -> stringResource(R.string.value_empty)
        km == null -> stringResource(R.string.value_gallons, Formats.oneDecimal(result.gallons))
        result.days == null -> rangeKmText(km)
        else -> stringResource(R.string.range_full, rangeKmText(km), rangeDaysText(result.days))
    }
    val detail = when {
        km == null -> null
        result.exceedsTank -> stringResource(R.string.budget_tank_full)
        else -> stringResource(R.string.value_gallons, Formats.oneDecimal(result.gallons))
    }
    // Ancho limitado: abajo el círculo es más angosto y el resultado no se debe cortar.
    Column(
        modifier = Modifier.heightIn(min = screen.bottomRowHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ResultLine(headline, TanqueoColors.White, FontWeight.Bold, screen.sp(15f), screen.dp(if (screen.isRound) 0.74f else 0.9f))
        detail?.let {
            ResultLine(it, TanqueoColors.Secondary, FontWeight.SemiBold, screen.sp(12f), screen.dp(if (screen.isRound) 0.5f else 0.9f))
        }
    }
}

@Composable
private fun ResultLine(text: String, color: Color, weight: FontWeight, maxSize: TextUnit, width: Dp) {
    BasicText(
        text = text,
        style = TextStyle(color = color, fontWeight = weight, textAlign = TextAlign.Center),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(10.sp, maxSize, 0.5.sp),
        modifier = Modifier.width(width),
    )
}
