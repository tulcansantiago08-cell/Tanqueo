package com.tanqueo.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.ui.theme.TanqueoColors

/** Un dato editable: la corona, el bisel o los dos botones cambian el número enorme. */
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
    val showSteppers: Boolean = true,
    @param:DrawableRes val decreaseIcon: Int = R.drawable.ic_remove,
    @param:DrawableRes val increaseIcon: Int = R.drawable.ic_add,
)

/** Hora arriba, una tarjeta por dato en un carrusel vertical y el botón principal abajo. */
@Composable
fun ValueCardsScreen(
    pagerState: PagerState,
    cards: List<ValueCardModel>,
    bottomAction: (@Composable () -> Unit)?,
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
            // En una pantalla tan pequeña, medio recorrido de tarjeta es demasiado para el dedo.
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState, snapPositionalThreshold = 0.2f),
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = screen.sideMargin * 0.5f, vertical = 3.dp),
            ) {
                ValueCard(cards[page], position = page + 1, total = cards.size)
            }
        }
        val bottomPadding = screen.dp(if (screen.isRound) 0.05f else 0.025f)
        if (bottomAction != null) {
            Box(Modifier.padding(top = 3.dp, bottom = bottomPadding)) { bottomAction() }
        } else {
            Box(Modifier.padding(bottom = bottomPadding))
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
                .padding(top = screen.dp(if (screen.isRound) 0.02f else 0.03f)),
            horizontalAlignment = Alignment.CenterHorizontally,
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
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = model.value,
                    style = TextStyle(
                        color = TanqueoColors.White,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.5).sp,
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 14.sp,
                        maxFontSize = screen.sp(if (screen.isRound) 36f else 40f),
                        stepSize = 1.sp,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = screen.dp(0.05f))
                    .padding(bottom = screen.dp(if (screen.isRound) 0.015f else 0.04f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (model.showSteppers) {
                    RoundIconButton(
                        icon = model.decreaseIcon,
                        contentDescription = stringResource(R.string.action_decrease, model.label),
                        size = screen.dp(if (screen.isRound) 0.17f else 0.2f),
                        compact = true,
                        onClick = { model.onSteps(-1) },
                    )
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    model.hint?.let { HintChip(it, model.onHintClick) }
                    model.secondaryHint?.let { HintChip(it, model.onSecondaryHintClick) }
                }
                if (model.showSteppers) {
                    RoundIconButton(
                        icon = model.increaseIcon,
                        contentDescription = stringResource(R.string.action_increase, model.label),
                        size = screen.dp(if (screen.isRound) 0.17f else 0.2f),
                        compact = true,
                        onClick = { model.onSteps(1) },
                    )
                }
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
        softWrap = false,
        modifier = if (onClick != null) {
            Modifier
                .glass(RoundedCornerShape(50))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 7.dp, vertical = 4.dp)
        } else {
            Modifier
        },
    )
}
