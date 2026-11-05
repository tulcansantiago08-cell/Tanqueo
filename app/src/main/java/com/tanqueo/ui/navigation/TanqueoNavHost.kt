package com.tanqueo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import com.tanqueo.ui.history.FillUpDetailScreen
import com.tanqueo.ui.history.FillUpDetailViewModel
import com.tanqueo.ui.history.HistoryScreen
import com.tanqueo.ui.home.HomeActions
import com.tanqueo.ui.home.HomeScreen
import com.tanqueo.ui.odometer.UpdateKmScreen
import com.tanqueo.ui.register.RegisterScreen
import com.tanqueo.ui.settings.SettingsScreen
import com.tanqueo.ui.summary.SummaryScreen

object Routes {
    const val HOME = "inicio"
    const val REGISTER = "registrar"
    const val HISTORY = "historial"
    const val DETAIL = "historial/{${FillUpDetailViewModel.ARG_ID}}"
    const val SUMMARY = "resumen"
    const val UPDATE_KM = "actualizar_km"
    const val SETTINGS = "ajustes"

    fun detail(id: Long) = "historial/$id"
}

@Composable
fun TanqueoNavHost(navController: NavHostController) {
    fun go(route: String) = navController.navigate(route) { launchSingleTop = true }

    SwipeDismissableNavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                HomeActions(
                    onAddFillUp = { go(Routes.REGISTER) },
                    onHistory = { go(Routes.HISTORY) },
                    onSummary = { go(Routes.SUMMARY) },
                    onUpdateKm = { go(Routes.UPDATE_KM) },
                    onSettings = { go(Routes.SETTINGS) },
                ),
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(onSaved = { navController.popBackStack(Routes.HOME, inclusive = false) })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(onOpen = { go(Routes.detail(it)) })
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument(FillUpDetailViewModel.ARG_ID) { type = NavType.LongType }),
        ) {
            FillUpDetailScreen(onDeleted = { navController.popBackStack() })
        }
        composable(Routes.SUMMARY) { SummaryScreen() }
        composable(Routes.UPDATE_KM) {
            UpdateKmScreen(onSaved = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) { SettingsScreen() }
    }
}
