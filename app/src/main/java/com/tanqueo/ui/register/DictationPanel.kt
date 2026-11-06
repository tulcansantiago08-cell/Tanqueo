package com.tanqueo.ui.register

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.domain.calc.RangeLevel
import com.tanqueo.domain.format.Formats
import com.tanqueo.ui.components.GlassCard
import com.tanqueo.ui.components.PrimaryPillButton
import com.tanqueo.ui.components.RoundIconButton
import com.tanqueo.ui.components.TimeHeader
import com.tanqueo.ui.components.labelRes
import com.tanqueo.ui.components.rememberAlertPulse
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.theme.TanqueoColors

/** Botón circular de vidrio y botón principal violeta, en un ancho que no toca el borde del círculo. */
@Composable
fun ActionRow(
    @DrawableRes secondaryIcon: Int,
    secondaryDescription: String,
    onSecondary: () -> Unit,
    primaryText: String,
    onPrimary: () -> Unit,
) {
    val screen = rememberScreenSize()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(screen.dp(0.035f)),
    ) {
        RoundIconButton(
            icon = secondaryIcon,
            contentDescription = secondaryDescription,
            size = screen.bottomRowHeight,
            compact = true,
            onClick = onSecondary,
        )
        PrimaryPillButton(
            text = primaryText,
            onClick = onPrimary,
            horizontalPadding = screen.dp(if (screen.isRound) 0.05f else 0.09f),
            minHeight = screen.bottomRowHeight,
        )
    }
}

/** Mientras la IA entiende, la revisión de lo entendido y el aviso si no se entendió. */
@Composable
fun DictationPanel(
    state: RegisterUiState,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    val screen = rememberScreenSize()
    ScreenScaffold {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TimeHeader()
            GlassCard(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = screen.sideMargin * 0.5f, vertical = 3.dp),
            ) {
                when (state.dictation) {
                    DictationStatus.INTERPRETING -> InterpretingContent()
                    DictationStatus.REVIEW -> ReviewContent(state)
                    else -> Message(
                        stringResource(
                            if (state.dictation == DictationStatus.UNAVAILABLE) {
                                R.string.dictation_unavailable
                            } else {
                                R.string.dictation_not_understood
                            },
                        ),
                    )
                }
            }
            Box(
                Modifier
                    .padding(top = 3.dp, bottom = screen.bottomRowPadding)
                    .size(width = screen.dp(0.9f), height = screen.bottomRowHeight),
                contentAlignment = Alignment.Center,
            ) {
                when (state.dictation) {
                    DictationStatus.INTERPRETING -> Unit
                    DictationStatus.REVIEW -> ActionRow(
                        secondaryIcon = R.drawable.ic_edit,
                        secondaryDescription = stringResource(R.string.action_edit),
                        onSecondary = onEdit,
                        primaryText = stringResource(if (state.canSave) R.string.action_save else R.string.action_complete),
                        onPrimary = onSave,
                    )
                    DictationStatus.UNAVAILABLE -> PrimaryPillButton(
                        text = stringResource(R.string.action_use_crown),
                        onClick = onEdit,
                        minHeight = screen.bottomRowHeight,
                    )
                    else -> ActionRow(
                        secondaryIcon = R.drawable.ic_edit,
                        secondaryDescription = stringResource(R.string.action_use_crown),
                        onSecondary = onEdit,
                        primaryText = stringResource(R.string.action_retry),
                        onPrimary = onRetry,
                    )
                }
            }
        }
    }
}

@Composable
private fun InterpretingContent() {
    val screen = rememberScreenSize()
    val pulse = rememberAlertPulse(RangeLevel.LOW)
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_mic),
            contentDescription = null,
            tint = TanqueoColors.Accent,
            modifier = Modifier
                .size(screen.dp(0.2f))
                .graphicsLayer { alpha = pulse },
        )
        Text(
            text = stringResource(R.string.dictation_interpreting),
            color = TanqueoColors.Secondary,
            fontSize = screen.sp(12f),
        )
    }
}

@Composable
private fun ReviewContent(state: RegisterUiState) {
    val screen = rememberScreenSize()
    val tank = stringResource(if (state.isFull) R.string.fill_full else R.string.fill_partial)
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = screen.dp(0.02f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(
            text = stringResource(R.string.dictation_review),
            color = TanqueoColors.Secondary,
            fontSize = screen.sp(11f),
            lineHeight = screen.sp(13f),
            maxLines = 1,
        )
        BasicText(
            text = Formats.cop(state.paidCop),
            style = TextStyle(
                color = TanqueoColors.White,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp,
            ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(14.sp, screen.sp(30f), 1.sp),
            modifier = Modifier.fillMaxWidth(),
        )
        // Se achica antes que perder un dato: "Parcial" cambia el cálculo.
        BasicText(
            text = stringResource(
                R.string.dictation_review_detail,
                Formats.oneDecimal(state.gallons),
                stringResource(state.fuelType.labelRes),
                tank,
            ),
            style = TextStyle(
                color = TanqueoColors.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(8.sp, screen.sp(12f), 0.5.sp),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = state.odometerKm?.let { stringResource(R.string.value_km, Formats.integer(it)) }
                ?: stringResource(R.string.dictation_no_km),
            color = TanqueoColors.Secondary,
            fontSize = screen.sp(11f),
            lineHeight = screen.sp(13f),
            maxLines = 1,
        )
    }
}

@Composable
private fun Message(text: String) {
    val screen = rememberScreenSize()
    Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = TanqueoColors.White,
            fontSize = screen.sp(13f),
            lineHeight = screen.sp(16f),
            textAlign = TextAlign.Center,
        )
    }
}
