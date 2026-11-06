package com.tanqueo.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.ui.components.RoundIconButton
import com.tanqueo.ui.components.pillGlow
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.theme.TanqueoColors

@Composable
fun AskPage(actions: HomeActions) {
    val screen = rememberScreenSize()
    PageColumn(screen) {
        PageTitle(stringResource(R.string.home_title_ask), screen)
        Text(
            text = stringResource(R.string.home_ask_hint),
            color = TanqueoColors.Secondary,
            fontSize = screen.sp(12f),
            lineHeight = screen.sp(15f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = screen.dp(0.1f)),
        )
        RoundIconButton(
            icon = R.drawable.ic_mic,
            contentDescription = stringResource(R.string.ask_action),
            size = screen.dp(0.26f),
            onClick = actions.onAsk,
            accent = true,
            modifier = Modifier.pillGlow(screen.dp(0.05f)),
        )
    }
}
