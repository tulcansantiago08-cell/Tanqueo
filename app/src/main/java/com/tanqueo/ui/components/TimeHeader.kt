package com.tanqueo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.tanqueo.domain.format.Formats
import com.tanqueo.ui.theme.TanqueoColors
import java.time.LocalDateTime
import kotlinx.coroutines.delay

/** Hora grande y fecha corta ("19:07 MIÉ 4"). Va arriba en todas las pantallas. */
@Composable
fun TimeHeader(modifier: Modifier = Modifier) {
    val screen = rememberScreenSize()
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay((60 - value.second) * 1_000L)
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = screen.headerTopPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = Formats.time(now),
            color = TanqueoColors.White,
            fontSize = screen.sp(22f),
            lineHeight = screen.sp(25f),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text = Formats.headerDate(now.toLocalDate()),
            color = TanqueoColors.Secondary,
            fontSize = screen.sp(11f),
            lineHeight = screen.sp(13f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier
                .alignByBaseline()
                .padding(start = 6.dp),
        )
    }
}
