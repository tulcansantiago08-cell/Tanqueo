package com.tanqueo.ui.components

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.domain.format.Formats
import com.tanqueo.ui.theme.TanqueoColors
import java.time.LocalDateTime
import kotlinx.coroutines.delay

/**
 * Hora grande y fecha corta ("19:07 MIÉ 4"). Va arriba en todas las pantallas, con una flecha para
 * regresar en todas menos el inicio: deslizar a la derecha también sirve, pero nada lo anuncia.
 */
@Composable
fun TimeHeader(modifier: Modifier = Modifier, showBack: Boolean = true) {
    val screen = rememberScreenSize()
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay((60 - value.second) * 1_000L)
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = screen.headerTopPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderTexts(now, screen)
        }
        if (showBack) BackArrow(screen, Modifier.align(Alignment.CenterStart))
    }
}

/** Atrás del sistema: así respeta lo que cada pantalla hace al regresar, como cerrar un panel. */
@Composable
private fun BackArrow(screen: ScreenSize, modifier: Modifier = Modifier) {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher ?: return
    val label = stringResource(R.string.action_back)
    val size = screen.dp(0.13f)
    Box(
        modifier = modifier
            // En lo redondo el círculo se angosta arriba: se pega a la hora para que no lo corte.
            .padding(start = screen.dp(if (screen.isRound) 0.143f else 0.1f))
            .size(size)
            .wrapContentSize(unbounded = true)
            .requiredSize(MinTouchSize)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = label) { dispatcher.onBackPressed() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chevron_left),
            contentDescription = label,
            tint = TanqueoColors.White,
            modifier = Modifier.size(size),
        )
    }
}

@Composable
private fun RowScope.HeaderTexts(now: LocalDateTime, screen: ScreenSize) {
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
