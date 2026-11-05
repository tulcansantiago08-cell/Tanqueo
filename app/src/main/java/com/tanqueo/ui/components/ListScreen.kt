package com.tanqueo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ScreenScaffold

/** Hora y fecha fijas arriba y una lista que se desplaza con el dedo, la corona o el bisel. */
@Composable
fun ListScreen(content: TransformingLazyColumnScope.() -> Unit) {
    val screen = rememberScreenSize()
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimeHeader()
            TransformingLazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(
                    start = screen.sideMargin,
                    end = screen.sideMargin,
                    top = 6.dp,
                    bottom = screen.dp(if (screen.isRound) 0.2f else 0.06f),
                ),
                content = content,
            )
        }
    }
}
