package com.tanqueo.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Text
import com.tanqueo.domain.format.Formats
import com.tanqueo.ui.theme.TanqueoColors
import java.time.LocalDateTime
import kotlinx.coroutines.delay

/** Hora grande y fecha en español. Va arriba en todas las pantallas. */
@Composable
fun TimeHeader(modifier: Modifier = Modifier) {
    val screen = rememberScreenSize()
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay((60 - value.second) * 1_000L)
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = screen.dp(if (screen.isRound) 0.035f else 0.015f)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = Formats.time(now),
            color = TanqueoColors.White,
            fontSize = screen.sp(25f),
            lineHeight = screen.sp(27f),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = Formats.shortDate(now.toLocalDate()),
            color = TanqueoColors.Secondary,
            fontSize = screen.sp(11f),
            lineHeight = screen.sp(13f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
