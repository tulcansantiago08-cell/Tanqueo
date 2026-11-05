package com.tanqueo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.tanqueo.ui.navigation.Routes
import com.tanqueo.ui.navigation.TanqueoNavHost
import com.tanqueo.ui.theme.TanqueoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** Pantalla pedida desde afuera (la tile o la notificación), pendiente de abrir. */
    private var pendingDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingDestination = intent.routeFromExtras()
        setContent {
            TanqueoTheme {
                val navController = rememberSwipeDismissableNavController()
                // Sin TimeText del sistema: cada pantalla trae su encabezado de hora y fecha.
                AppScaffold(timeText = {}) {
                    TanqueoNavHost(navController)
                }
                LaunchedEffect(pendingDestination) {
                    pendingDestination?.let { route ->
                        navController.navigate(route) { launchSingleTop = true }
                        pendingDestination = null
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingDestination = intent.routeFromExtras()
    }

    private fun Intent.routeFromExtras(): String? =
        if (getStringExtra(EXTRA_DESTINATION) == DESTINATION_REGISTER) Routes.REGISTER else null

    companion object {
        const val EXTRA_DESTINATION = "destino"
        const val DESTINATION_REGISTER = "registrar"
    }
}
