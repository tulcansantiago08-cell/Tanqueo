package com.tanqueo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.ScreenScaffold
import com.tanqueo.domain.usecase.Dashboard
import com.tanqueo.ui.components.RequestNotificationPermissionOnce
import com.tanqueo.ui.components.TimeHeader
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.theme.TanqueoColors

data class HomeActions(
    val onAddFillUp: () -> Unit,
    val onHistory: () -> Unit,
    val onSummary: () -> Unit,
    val onUpdateKm: () -> Unit,
    val onSettings: () -> Unit,
    val onBudget: () -> Unit,
)

private const val PAGE_COUNT = 3

@Composable
fun HomeScreen(actions: HomeActions, viewModel: HomeViewModel = hiltViewModel()) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    RequestNotificationPermissionOnce()
    ScreenScaffold {
        Box(Modifier.fillMaxSize()) {
            val data = dashboard
            when {
                data == null -> Unit
                data.lastFillUp == null -> EmptyHome(actions)
                else -> HomePager(data, actions)
            }
            // La hora queda fija arriba mientras las páginas se deslizan debajo.
            TimeHeader(Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun HomePager(dashboard: Dashboard, actions: HomeActions) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
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
                else -> MonthPage(dashboard, isCurrent, actions)
            }
        }
        PageDots(
            current = pagerState.currentPage,
            count = PAGE_COUNT,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Puntos de página abajo: el actual en blanco. */
@Composable
private fun PageDots(current: Int, count: Int, modifier: Modifier = Modifier) {
    val screen = rememberScreenSize()
    Row(
        modifier = modifier.padding(bottom = screen.dp(if (screen.isRound) 0.025f else 0.02f)),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
    }
}
