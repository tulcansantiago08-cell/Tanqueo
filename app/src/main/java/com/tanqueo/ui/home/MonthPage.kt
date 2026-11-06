package com.tanqueo.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.domain.format.Formats
import com.tanqueo.domain.usecase.Dashboard
import com.tanqueo.ui.components.GlassCard
import com.tanqueo.ui.components.GlassPillButton
import com.tanqueo.ui.components.labelRes
import com.tanqueo.ui.components.rememberCountUp
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.theme.TanqueoColors
import java.time.ZoneId

@Composable
fun MonthPage(dashboard: Dashboard, isCurrent: Boolean, actions: HomeActions) {
    val screen = rememberScreenSize()
    val spent = rememberCountUp(dashboard.month.totalSpentCop.toFloat(), isCurrent)
    val last = dashboard.lastFillUp

    PageColumn(screen) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.action_view_summary),
                    onClick = actions.onSummary,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_title_month),
                    color = TanqueoColors.Secondary,
                    fontSize = screen.sp(12f),
                    lineHeight = screen.sp(14f),
                    maxLines = 1,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = TanqueoColors.Secondary,
                    modifier = Modifier.size(screen.dp(0.075f)),
                )
            }
            BasicText(
                text = Formats.cop(spent.toLong()),
                style = TextStyle(
                    color = TanqueoColors.Accent,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp,
                ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(14.sp, screen.sp(29f), 1.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = screen.dp(0.08f)),
            )
        }
        if (last != null) {
            val date = last.at.atZone(ZoneId.systemDefault()).toLocalDate()
            GlassCard(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = screen.sideMargin),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = screen.dp(if (screen.isRound) 0.02f else 0.03f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.home_last_fill_up_date, Formats.shortDate(date)),
                        color = TanqueoColors.Secondary,
                        fontSize = screen.sp(11f),
                        lineHeight = screen.sp(13f),
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(
                            R.string.home_last_fill_up_detail,
                            Formats.oneDecimal(last.gallons),
                            stringResource(last.fuelType.labelRes),
                        ),
                        color = TanqueoColors.White,
                        fontSize = screen.sp(13f),
                        lineHeight = screen.sp(16f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }
        GlassPillButton(
            text = stringResource(R.string.action_view_history),
            icon = null,
            onClick = actions.onHistory,
        )
    }
}
