package com.tanqueo

import android.content.Context
import android.content.Intent
import com.tanqueo.domain.format.AppLocale
import com.tanqueo.ui.language.LanguageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.wear.compose.material3.AppScaffold
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.tanqueo.domain.model.VehicleType
import com.tanqueo.ui.navigation.Routes
import com.tanqueo.ui.navigation.TanqueoNavHost
import com.tanqueo.ui.theme.TanqueoTheme
import com.tanqueo.ui.welcome.StartState
import com.tanqueo.ui.welcome.StartViewModel
import com.tanqueo.ui.welcome.WelcomeScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** Pantalla pedida desde afuera (la tile o la notificación), pendiente de abrir. */
    private var pendingDestination by mutableStateOf<String?>(null)

    /** Vehículo que trae un aviso: se deja activo antes de abrir nada. */
    private var pendingVehicle by mutableStateOf<VehicleType?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Al volver a crearse tras un cambio de idioma, fechas y números siguen al nuevo.
        AppLocale.language = LanguageManager.current(this)
        if (savedInstanceState == null) readExtras(intent)
        setContent {
            TanqueoTheme {
                val navController = rememberSwipeDismissableNavController()
                val start: StartViewModel = hiltViewModel()
                val startState by start.state.collectAsStateWithLifecycle()
                // Sin TimeText del sistema: cada pantalla trae su encabezado de hora y fecha.
                AppScaffold(timeText = {}) {
                    when (startState) {
                        StartState.LOADING -> Unit
                        StartState.CHOOSE_VEHICLE -> WelcomeScreen(onChoose = start::choose)
                        StartState.READY -> TanqueoNavHost(navController)
                    }
                }
                // Lo pedido desde la tile o el aviso espera a que la app esté lista.
                LaunchedEffect(pendingVehicle) {
                    pendingVehicle?.let { vehicle ->
                        start.select(vehicle)
                        pendingVehicle = null
                    }
                }
                LaunchedEffect(pendingDestination, startState, pendingVehicle) {
                    if (startState != StartState.READY || pendingVehicle != null) return@LaunchedEffect
                    pendingDestination?.let { route ->
                        pendingDestination = null
                        navController.navigate(route) { launchSingleTop = true }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readExtras(intent)
    }

    private fun readExtras(intent: Intent) {
        pendingVehicle = intent.getStringExtra(EXTRA_VEHICLE)?.let { name -> VehicleType.entries.firstOrNull { it.name == name } }
        pendingDestination = intent.routeFromExtras()
    }

    private fun Intent.routeFromExtras(): String? = when (getStringExtra(EXTRA_DESTINATION)) {
        DESTINATION_REGISTER -> Routes.REGISTER
        DESTINATION_GAS_STATIONS -> Routes.GAS_STATIONS
        else -> null
    }

    companion object {
        const val EXTRA_DESTINATION = "destino"
        const val DESTINATION_REGISTER = "registrar"
        const val DESTINATION_GAS_STATIONS = "gasolineras"
        const val EXTRA_VEHICLE = "vehiculo"
    }
}
