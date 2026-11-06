package com.tanqueo.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tanqueo.R
import com.tanqueo.ui.components.GlassPillButton
import com.tanqueo.ui.components.PrimaryPillButton
import com.tanqueo.ui.components.ScreenSize
import com.tanqueo.ui.components.rememberScreenSize

/** Espacio común de las páginas: debajo de la hora y encima de los puntos. */
@Composable
internal fun PageColumn(
    screen: ScreenSize,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaddingValues(top = screen.contentTop, bottom = screen.dp(if (screen.isRound) 0.1f else 0.07f))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        content = content,
    )
}

/** La acción de todos los días y la puerta a todo lo demás, con su nombre escrito. */
@Composable
internal fun HomeMainActions(actions: HomeActions, modifier: Modifier = Modifier) {
    val screen = rememberScreenSize()
    // En lo redondo esta altura es angosta: los dos botones juntos deben caber dentro del círculo.
    val padding = screen.dp(if (screen.isRound) 0.03f else 0.05f)
    val height = screen.dp(if (screen.isRound) 0.165f else 0.19f)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(screen.dp(0.015f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassPillButton(
            text = stringResource(R.string.action_menu),
            icon = null,
            onClick = actions.onMenu,
            horizontalPadding = padding,
            minHeight = height,
        )
        PrimaryPillButton(
            text = stringResource(R.string.action_add_fill_up),
            onClick = actions.onAddFillUp,
            horizontalPadding = padding,
            minHeight = height,
            textSize = 14f,
        )
    }
}
