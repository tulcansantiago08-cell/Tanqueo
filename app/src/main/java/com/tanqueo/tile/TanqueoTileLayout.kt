package com.tanqueo.tile

import android.content.ComponentName
import android.content.Context
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.ActionBuilders.stringExtra
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.layout.box
import androidx.wear.protolayout.layout.column
import androidx.wear.protolayout.material3.ButtonColors
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.ProgressIndicatorColors
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.circularProgressIndicator
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.layoutString
import com.tanqueo.MainActivity
import com.tanqueo.R
import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.calc.RangeLevel
import com.tanqueo.domain.format.Formats
import com.tanqueo.domain.usecase.Dashboard
import com.tanqueo.ui.components.rangeDays
import com.tanqueo.ui.components.rangeKm

private val White = LayoutColor(0xFFFFFFFF.toInt())
private val Secondary = LayoutColor(0xFF9A9AA5.toInt())
private val Accent = LayoutColor(0xFF7C5CFF.toInt())
private val Track = LayoutColor(0x24FFFFFF)
private val Amber = LayoutColor(0xFFFFB020.toInt())
private val Red = LayoutColor(0xFFFF5A4F.toInt())

private val colors = ColorScheme(
    primary = Accent,
    onPrimary = White,
    onSurface = White,
    onSurfaceVariant = Secondary,
    background = LayoutColor(0xFF000000.toInt()),
)

private const val ARC_START_DEGREES = -128f
private const val ARC_END_DEGREES = 128f
private const val ARC_STROKE_DP = 6f

/** Arco de progreso por el borde, km y días al centro, gasto del mes y el botón de registrar. */
fun tanqueoTileLayout(
    context: Context,
    deviceParameters: DeviceParameters,
    dashboard: Dashboard,
): LayoutElement = materialScope(
    context = context,
    deviceConfiguration = deviceParameters,
    allowDynamicTheme = false,
    defaultColorScheme = colors,
) {
    val estimate = dashboard.estimate as? RangeEstimate.Available
    val level = estimate?.let { RangeLevel.of(it.kmRemaining) } ?: RangeLevel.NORMAL
    val month = context.getString(R.string.tile_month_spend, Formats.cop(dashboard.month.totalSpentCop))
    val layout = primaryLayout(
        titleSlot = { text(month.layoutString, typography = Typography.BODY_SMALL, color = Secondary) },
        mainSlot = { mainContent(context, estimate, level) },
        bottomSlot = {
            textEdgeButton(
                onClick = clickable(
                    action = launchAction(
                        ComponentName(context, MainActivity::class.java),
                        mapOf(MainActivity.EXTRA_DESTINATION to stringExtra(MainActivity.DESTINATION_REGISTER)),
                    ),
                ),
                colors = ButtonColors(containerColor = Accent, labelColor = White),
                labelContent = { text(context.getString(R.string.action_add_fill_up).layoutString) },
            )
        },
    )
    box(
        circularProgressIndicator(
            staticProgress = estimate?.tankFraction?.toFloat() ?: 0f,
            startAngleDegrees = ARC_START_DEGREES,
            endAngleDegrees = ARC_END_DEGREES,
            strokeWidth = ARC_STROKE_DP,
            colors = ProgressIndicatorColors(indicatorColor = level.arcColor(), trackColor = Track),
        ),
        layout,
        width = expand(),
        height = expand(),
    )
}

private fun RangeLevel.arcColor(): LayoutColor = when (this) {
    RangeLevel.NORMAL -> Accent
    RangeLevel.LOW -> Amber
    RangeLevel.CRITICAL -> Red
}

private fun MaterialScope.mainContent(
    context: Context,
    estimate: RangeEstimate.Available?,
    level: RangeLevel,
): LayoutElement {
    val res = context.resources
    if (estimate == null) {
        return text(
            res.getString(R.string.range_insufficient).layoutString,
            typography = Typography.BODY_MEDIUM,
            color = White,
            maxLines = 3,
        )
    }
    val km = text(
        res.rangeKm(estimate.kmRemaining).layoutString,
        typography = Typography.DISPLAY_SMALL,
        color = if (level == RangeLevel.NORMAL) White else level.arcColor(),
    )
    val days = estimate.daysRemaining ?: return km
    return column(
        km,
        text(res.rangeDays(days).layoutString, typography = Typography.BODY_MEDIUM, color = Secondary),
    )
}
