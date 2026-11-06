package com.tanqueo.ui.menu

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tanqueo.R
import com.tanqueo.ui.components.GlassPillButton
import com.tanqueo.ui.components.ListScreen
import com.tanqueo.ui.components.edgeAware

data class MenuActions(
    val onAddFillUp: () -> Unit,
    val onAsk: () -> Unit,
    val onUpdateKm: () -> Unit,
    val onHistory: () -> Unit,
    val onSummary: () -> Unit,
    val onBudget: () -> Unit,
    val onSettings: () -> Unit,
)

/** Todo lo que hace la app en un solo lugar y con su nombre escrito: nada depende de adivinar un ícono. */
@Composable
fun MenuScreen(actions: MenuActions) {
    val entries = listOf(
        Triple(R.string.action_register_fill_up, R.drawable.ic_add, actions.onAddFillUp),
        Triple(R.string.ask_action, R.drawable.ic_mic, actions.onAsk),
        Triple(R.string.action_update_km, R.drawable.ic_speed, actions.onUpdateKm),
        Triple(R.string.action_history, R.drawable.ic_history, actions.onHistory),
        Triple(R.string.action_month_summary, R.drawable.ic_gas, actions.onSummary),
        Triple(R.string.action_budget, R.drawable.ic_calculate, actions.onBudget),
        Triple(R.string.action_settings, R.drawable.ic_settings, actions.onSettings),
    )
    ListScreen {
        entries.forEach { (label, icon, onClick) ->
            item {
                GlassPillButton(
                    text = stringResource(label),
                    icon = icon,
                    onClick = onClick,
                    modifier = Modifier.edgeAware(this).fillMaxWidth(),
                )
            }
        }
    }
}
