package com.tanqueo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.ui.theme.TanqueoColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CHECK_DRAW_MILLIS = 340
private const val HOLD_MILLIS = 620L

/** Check violeta que se dibuja solo; al terminar llama a [onFinished] para volver a Inicio. */
@Composable
fun SavedConfirmation(onFinished: () -> Unit) {
    val screen = rememberScreenSize()
    val scale = remember { Animatable(0.6f) }
    val check = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
        check.animateTo(1f, tween(CHECK_DRAW_MILLIS, delayMillis = 90, easing = FastOutSlowInEasing))
        delay(HOLD_MILLIS)
        onFinished()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(screen.dp(0.42f))
                .softGlow(strength = 0.5f, radiusFactor = 0.85f)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        ) {
            val stroke = size.minDimension * 0.075f
            drawCircle(
                color = TanqueoColors.Accent,
                radius = size.minDimension / 2 - stroke / 2,
                style = Stroke(stroke),
            )
            val path = Path().apply {
                moveTo(size.width * 0.29f, size.height * 0.52f)
                lineTo(size.width * 0.44f, size.height * 0.67f)
                lineTo(size.width * 0.72f, size.height * 0.36f)
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * check.value, partial, true)
            drawPath(
                path = partial,
                color = TanqueoColors.Accent,
                style = Stroke(stroke * 1.25f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        Text(
            text = stringResource(R.string.saved_confirmation),
            color = TanqueoColors.White,
            fontSize = screen.sp(15f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}
