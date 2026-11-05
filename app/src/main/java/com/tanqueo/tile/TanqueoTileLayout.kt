package com.tanqueo.tile

import android.content.ComponentName
import android.content.Context
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.ActionBuilders.stringExtra
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.layout.column
import androidx.wear.protolayout.material3.ButtonColors
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
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
import com.tanqueo.domain.format.Formats
import com.tanqueo.domain.usecase.Dashboard
import com.tanqueo.ui.components.rangeDays
import com.tanqueo.ui.components.rangeKm

private val White = LayoutColor(0xFFFFFFFF.toInt())
private val Secondary = LayoutColor(0xFFB9B9C2.toInt())
private val Accent = LayoutColor(0xFF7C5CFF.toInt())

private val colors = ColorScheme(
    primary = Accent,
    onPrimary = White,
    onSurface = White,
    onSurfaceVariant = Secondary,
    background = LayoutColor(0xFF000000.toInt()),
)

/** km y días restantes, gasto del mes y el botón "+ Tanqueo" que abre directamente el registro. */
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
    primaryLayout(
        titleSlot = {
            text(context.getString(R.string.app_name).layoutString, color = Secondary)
        },
        mainSlot = { mainContent(context, dashboard) },
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
}

private fun MaterialScope.mainContent(context: Context, dashboard: Dashboard): LayoutElement {
    val res = context.resources
    val month = res.getString(R.string.tile_month_spend, Formats.cop(dashboard.month.totalSpentCop))
    return when (val estimate = dashboard.estimate) {
        RangeEstimate.Insufficient -> column(
            text(
                res.getString(R.string.range_insufficient).layoutString,
                typography = Typography.BODY_MEDIUM,
                color = White,
                maxLines = 3,
            ),
            text(month.layoutString, typography = Typography.BODY_SMALL, color = Secondary),
        )
        is RangeEstimate.Available -> {
            val km = text(
                res.rangeKm(estimate.kmRemaining).layoutString,
                typography = Typography.DISPLAY_SMALL,
                color = White,
            )
            val monthText = text(month.layoutString, typography = Typography.BODY_SMALL, color = Secondary)
            val days = estimate.daysRemaining
            if (days != null) {
                column(
                    km,
                    text(res.rangeDays(days).layoutString, typography = Typography.BODY_MEDIUM, color = White),
                    monthText,
                )
            } else {
                column(km, monthText)
            }
        }
    }
}
