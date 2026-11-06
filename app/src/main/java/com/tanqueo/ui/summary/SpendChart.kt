package com.tanqueo.ui.summary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.domain.calc.MonthSpend
import com.tanqueo.domain.format.Formats
import com.tanqueo.ui.components.GlassCard
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.theme.TanqueoColors
import java.time.format.TextStyle
import java.util.Locale

private val spanish = Locale.forLanguageTag("es-CO")

/**
 * Gasto por mes en barras. Una sola serie sobre un solo eje: el mes actual lleva el acento y los demás
 * van en gris, con su inicial debajo. Al ser un reloj no hay tooltip: se rotula solo la cifra mayor, y
 * los lectores de pantalla reciben todos los valores.
 */
@Composable
fun SpendChart(months: List<MonthSpend>, modifier: Modifier = Modifier) {
    val screen = rememberScreenSize()
    val highest = months.maxOfOrNull { it.totalCop } ?: 0L
    val resources = LocalResources.current
    val description = months.joinToString(", ") { spend ->
        val name = spend.month.month.getDisplayName(TextStyle.FULL_STANDALONE, spanish)
        resources.getString(R.string.chart_item, name, Formats.cop(spend.totalCop))
    }
    val chartDescription = stringResource(R.string.chart_description, description)

    GlassCard(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) { contentDescription = chartDescription },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.chart_title),
                color = TanqueoColors.Secondary,
                fontSize = screen.sp(11f),
                lineHeight = screen.sp(13f),
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.chart_highest, Formats.cop(highest)),
                color = TanqueoColors.White,
                fontSize = screen.sp(11f),
                lineHeight = screen.sp(14f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            val barColor = TanqueoColors.ChartBar
            val currentColor = TanqueoColors.Accent
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(screen.dp(0.2f))
                    .padding(top = 3.dp),
            ) {
                if (months.isEmpty()) return@Canvas
                val gap = 2.dp.toPx()
                val slot = size.width / months.size
                val barWidth = (slot - gap).coerceAtMost(14.dp.toPx())
                val minHeight = 2.dp.toPx()
                months.forEachIndexed { index, spend ->
                    val fraction = if (highest > 0) spend.totalCop.toFloat() / highest else 0f
                    val height = if (spend.totalCop > 0) (size.height * fraction).coerceAtLeast(minHeight) else minHeight
                    drawRoundRect(
                        color = if (index == months.lastIndex) currentColor else barColor,
                        topLeft = Offset(slot * index + (slot - barWidth) / 2, size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(minOf(4.dp.toPx(), barWidth / 2)),
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                months.forEach { spend ->
                    Text(
                        text = spend.month.month.getDisplayName(TextStyle.NARROW_STANDALONE, spanish).uppercase(spanish),
                        color = TanqueoColors.Secondary,
                        fontSize = screen.sp(9f),
                        lineHeight = screen.sp(11f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
