package com.tanqueo.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.theme.TanqueoColors

@Composable
fun EmptyHome(actions: HomeActions) {
    val screen = rememberScreenSize()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = screen.contentTop, bottom = screen.dp(0.06f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        CarIllustration(screen.dp(0.24f), glow = false, modifier = Modifier.alpha(0.28f))
        Text(
            text = stringResource(R.string.home_empty),
            color = TanqueoColors.Secondary,
            fontSize = screen.sp(13f),
            lineHeight = screen.sp(16f),
            textAlign = TextAlign.Center,
            modifier = Modifier.width(screen.dp(0.68f)),
        )
        HomeMainActions(actions)
    }
}
