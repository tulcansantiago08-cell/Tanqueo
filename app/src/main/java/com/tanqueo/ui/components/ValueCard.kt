package com.tanqueo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.ui.theme.TanqueoColors

/** Un dato editable: la corona, el bisel o los botones − y + cambian el valor grande. */
data class ValueCardModel(
    val label: String,
    val value: String,
    val onSteps: (Int) -> Unit,
    /** Texto pequeño bajo el valor, por ejemplo el paso actual. */
    val hint: String? = null,
    val onHintClick: (() -> Unit)? = null,
    /** Segunda acción pequeña, por ejemplo quitar el kilometraje. */
    val secondaryHint: String? = null,
    val onSecondaryHintClick: (() -> Unit)? = null,
)

/** Hora arriba, una tarjeta por dato en un carrusel vertical y el botón principal abajo. */
@Composable
fun ValueCardsScreen(
    pagerState: PagerState,
    cards: List<ValueCardModel>,
    bottomAction: @Composable () -> Unit,
) {
    val screen = rememberScreenSize()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .rotarySteps { steps ->
                cards.getOrNull(pagerState.currentPage)?.onSteps?.invoke(steps)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TimeHeader()
        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            pageSpacing = 8.dp,
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = screen.sideMargin * 0.6f, vertical = 3.dp),
            ) {
                ValueCard(cards[page], position = page + 1, total = cards.size)
            }
        }
        Box(Modifier.padding(top = 2.dp, bottom = screen.dp(if (screen.isRound) 0.035f else 0.015f))) {
            bottomAction()
        }
    }
}

@Composable
private fun ValueCard(model: ValueCardModel, position: Int, total: Int) {
    val screen = rememberScreenSize()
    GlassCard(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                text = if (total > 1) {
                    stringResource(R.string.card_label_position, model.label, position, total)
                } else {
                    model.label
                },
                color = TanqueoColors.Secondary,
                fontSize = screen.sp(11f),
                lineHeight = screen.sp(13f),
                maxLines = 1,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(
                    icon = R.drawable.ic_remove,
                    contentDescription = stringResource(R.string.action_decrease, model.label),
                    size = screen.dp(0.16f),
                    touchSize = screen.dp(0.19f),
                    onClick = { model.onSteps(-1) },
                )
                BasicText(
                    text = model.value,
                    style = TextStyle(
                        color = TanqueoColors.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 12.sp,
                        maxFontSize = screen.sp(26f),
                        stepSize = 1.sp,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 1.dp),
                )
                RoundIconButton(
                    icon = R.drawable.ic_add,
                    contentDescription = stringResource(R.string.action_increase, model.label),
                    size = screen.dp(0.16f),
                    touchSize = screen.dp(0.19f),
                    onClick = { model.onSteps(1) },
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                model.hint?.let { HintChip(it, model.onHintClick) }
                model.secondaryHint?.let { HintChip(it, model.onSecondaryHintClick) }
            }
        }
    }
}

@Composable
private fun HintChip(text: String, onClick: (() -> Unit)?) {
    val screen = rememberScreenSize()
    Text(
        text = text,
        color = TanqueoColors.Secondary,
        fontSize = screen.sp(10f),
        lineHeight = screen.sp(12f),
        maxLines = 1,
        modifier = if (onClick != null) {
            Modifier
                .clip(RoundedCornerShape(50))
                .glass(RoundedCornerShape(50))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        } else {
            Modifier
        },
    )
}
