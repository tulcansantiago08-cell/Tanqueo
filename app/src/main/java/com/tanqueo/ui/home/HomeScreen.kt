package com.tanqueo.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import com.tanqueo.R
import com.tanqueo.domain.usecase.Dashboard
import com.tanqueo.ui.components.RequestNotificationPermissionOnce
import com.tanqueo.ui.components.TimeHeader
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.theme.TanqueoColors
import kotlinx.coroutines.launch

data class HomeActions(
    val onAddFillUp: () -> Unit,
    val onHistory: () -> Unit,
    val onSummary: () -> Unit,
    val onMenu: () -> Unit,
    val onAsk: () -> Unit,
)

private const val PAGE_COUNT = 4

@Composable
fun HomeScreen(actions: HomeActions, viewModel: HomeViewModel = hiltViewModel()) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    // Se pide cuando ya hay un tanqueo: ahí el aviso de "toca tanquear" tiene sentido.
    if (dashboard?.lastFillUp != null) RequestNotificationPermissionOnce()
    ScreenScaffold {
        Box(Modifier.fillMaxSize()) {
            val data = dashboard
            when {
                data == null -> Unit
                data.lastFillUp == null -> EmptyHome(actions)
                else -> HomePager(data, actions)
            }
            // La hora queda fija arriba mientras las páginas se deslizan debajo.
            TimeHeader(Modifier.align(Alignment.TopCenter), showBack = false)
        }
    }
}

@Composable
private fun HomePager(dashboard: Dashboard, actions: HomeActions) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // La corona o el bisel también pasan de página.
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(pagerState),
        ) { page ->
            val isCurrent = pagerState.currentPage == page
            when (page) {
                0 -> VehiclePage(dashboard, isCurrent, actions)
                1 -> RangePage(dashboard, isCurrent, actions)
                2 -> MonthPage(dashboard, isCurrent, actions)
                else -> AskPage(actions)
            }
        }
        PageDots(
            current = pagerState.currentPage,
            count = PAGE_COUNT,
            onGoTo = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Puntos de página abajo, el actual en blanco, y flechas para pasar de página sin tener que deslizar. */
@Composable
private fun PageDots(current: Int, count: Int, onGoTo: (Int) -> Unit, modifier: Modifier = Modifier) {
    val screen = rememberScreenSize()
    Row(
        modifier = modifier.padding(bottom = screen.dp(if (screen.isRound) 0.025f else 0.02f)),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PageArrow(
            icon = R.drawable.ic_chevron_left,
            description = stringResource(R.string.action_previous_page),
            enabled = current > 0,
            onClick = { onGoTo(current - 1) },
        )
        repeat(count) { index ->
            Box(
                Modifier
                    .size(6.dp)
                    .background(
                        if (index == current) TanqueoColors.White else TanqueoColors.GlassBorder,
                        CircleShape,
                    ),
            )
        }
        PageArrow(
            icon = R.drawable.ic_chevron_right,
            description = stringResource(R.string.action_next_page),
            enabled = current < count - 1,
            onClick = { onGoTo(current + 1) },
        )
    }
}

/** Sin página hacia ese lado se vuelve invisible, para que los puntos no cambien de lugar. */
@Composable
private fun PageArrow(@DrawableRes icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    val size = rememberScreenSize().dp(0.09f)
    Box(
        modifier = Modifier
            .size(size)
            .wrapContentSize(unbounded = true)
            // Más ancha que alta: arriba están los botones de la página.
            .requiredSize(width = 44.dp, height = 30.dp)
            .alpha(if (enabled) 1f else 0f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = if (enabled) description else null,
            tint = TanqueoColors.White,
            modifier = Modifier.size(size * 1.3f),
        )
    }
}
