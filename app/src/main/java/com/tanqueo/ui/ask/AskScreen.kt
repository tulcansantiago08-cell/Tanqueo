package com.tanqueo.ui.ask

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.domain.calc.RangeLevel
import com.tanqueo.domain.usecase.AskAnswer
import com.tanqueo.ui.components.GlassCard
import com.tanqueo.ui.components.GlassPillButton
import com.tanqueo.ui.components.ListScreen
import com.tanqueo.ui.components.PrimaryPillButton
import com.tanqueo.ui.components.TimeHeader
import com.tanqueo.ui.components.edgeAware
import com.tanqueo.ui.components.rememberAlertPulse
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.components.rememberTextInputLauncher
import com.tanqueo.ui.theme.TanqueoColors

/** Asistente: se pregunta con voz o teclado y las cifras de la respuesta se calculan en el reloj. */
@Composable
fun AskScreen(
    onLogFillUp: (String) -> Unit,
    /** Abre la entrada de una vez; si se cancela, quedan a la vista los ejemplos. */
    listenOnOpen: Boolean = false,
    viewModel: AskViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val startInput = rememberTextInputLauncher(
        prompt = stringResource(R.string.ask_prompt),
        onResult = viewModel::ask,
        onUnavailable = viewModel::onInputUnavailable,
    )
    var alreadyListened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (listenOnOpen && !alreadyListened) {
            alreadyListened = true
            startInput()
        }
    }

    when (val current = state) {
        is AskUiState.Idle -> IdleContent(current.inputUnavailable, onAsk = startInput, onSample = viewModel::ask)
        is AskUiState.Thinking -> ThinkingContent()
        is AskUiState.Answered -> {
            val answer = current.answer
            if (answer is AskAnswer.LogFillUp) {
                LaunchedEffect(answer) {
                    viewModel.reset()
                    onLogFillUp(answer.text)
                }
            } else {
                BackHandler(onBack = viewModel::reset)
                ListScreen {
                    questionItem(current.question)
                    answerItems(answer)
                    item {
                        GlassPillButton(
                            text = stringResource(R.string.ask_again),
                            icon = R.drawable.ic_mic,
                            onClick = startInput,
                            modifier = Modifier.edgeAware(this),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleContent(inputUnavailable: Boolean, onAsk: () -> Unit, onSample: (String) -> Unit) {
    val samples = listOf(
        R.string.ask_sample_trip to R.string.ask_sample_trip_question,
        R.string.ask_sample_range to R.string.ask_sample_range_question,
        R.string.ask_sample_budget to R.string.ask_sample_budget_question,
        R.string.ask_sample_gallons to R.string.ask_sample_gallons_question,
        R.string.ask_sample_spending to R.string.ask_sample_spending_question,
    ).map { (label, question) -> stringResource(label) to stringResource(question) }

    ListScreen {
        if (inputUnavailable) {
            item { AnswerMessage(stringResource(R.string.ask_unavailable), Modifier.edgeAware(this)) }
        }
        item {
            Box(Modifier.edgeAware(this).fillMaxWidth(), contentAlignment = Alignment.Center) {
                PrimaryPillButton(text = stringResource(R.string.ask_action), onClick = onAsk)
            }
        }
        item {
            val screen = rememberScreenSize()
            Text(
                text = stringResource(R.string.ask_samples_title),
                color = TanqueoColors.Secondary,
                fontSize = screen.sp(11f),
                textAlign = TextAlign.Center,
                modifier = Modifier.edgeAware(this).fillMaxWidth().padding(top = 4.dp),
            )
        }
        samples.forEach { (label, question) ->
            item {
                GlassPillButton(
                    text = label,
                    icon = null,
                    onClick = { onSample(question) },
                    modifier = Modifier.edgeAware(this).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ThinkingContent() {
    val screen = rememberScreenSize()
    val pulse = rememberAlertPulse(RangeLevel.LOW)
    ScreenScaffold {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TimeHeader()
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_tip),
                    contentDescription = null,
                    tint = TanqueoColors.Accent,
                    modifier = Modifier
                        .size(screen.dp(0.2f))
                        .graphicsLayer { alpha = pulse },
                )
                Text(
                    text = stringResource(R.string.ask_thinking),
                    color = TanqueoColors.Secondary,
                    fontSize = screen.sp(12f),
                )
            }
        }
    }
}

private fun TransformingLazyColumnScope.questionItem(question: String) {
    item {
        val screen = rememberScreenSize()
        Text(
            text = stringResource(R.string.ask_quote, question),
            color = TanqueoColors.Secondary,
            fontSize = screen.sp(10f),
            lineHeight = screen.sp(12f),
            textAlign = TextAlign.Center,
            // Una línea: la pregunta ya se sabe, lo que importa es que la respuesta se vea sin desplazar.
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.edgeAware(this).fillMaxWidth().padding(horizontal = screen.dp(0.05f)),
        )
    }
}

/** Dato principal de una respuesta: etiqueta, cifra grande y una línea de detalle. */
@Composable
internal fun AnswerCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    valueColor: Color = TanqueoColors.White,
) {
    val screen = rememberScreenSize()
    GlassCard(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                color = TanqueoColors.Secondary,
                fontSize = screen.sp(11f),
                lineHeight = screen.sp(13f),
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            BasicText(
                text = value,
                style = TextStyle(color = valueColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(13.sp, screen.sp(25f), 1.sp),
                modifier = Modifier.fillMaxWidth(),
            )
            detail?.let {
                Text(
                    text = it,
                    color = TanqueoColors.White,
                    fontSize = screen.sp(11f),
                    lineHeight = screen.sp(14f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Aclaración de una respuesta: sin tarjeta, para que no compita con las cifras. */
@Composable
internal fun AnswerFootnote(text: String, modifier: Modifier = Modifier) {
    val screen = rememberScreenSize()
    Text(
        text = text,
        color = TanqueoColors.Secondary,
        fontSize = screen.sp(10f),
        lineHeight = screen.sp(13f),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = screen.dp(0.05f)),
    )
}

/** Mensaje sin cifras: lo que falta para poder responder. */
@Composable
internal fun AnswerMessage(text: String, modifier: Modifier = Modifier) {
    val screen = rememberScreenSize()
    GlassCard(modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = TanqueoColors.White,
            fontSize = screen.sp(12f),
            lineHeight = screen.sp(15f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}
