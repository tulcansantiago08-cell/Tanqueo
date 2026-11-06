package com.tanqueo.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import com.tanqueo.ui.ask.AskScreen
import com.tanqueo.ui.budget.BudgetScreen
import com.tanqueo.ui.history.FillUpDetailScreen
import com.tanqueo.ui.history.FillUpDetailViewModel
import com.tanqueo.ui.history.HistoryScreen
import com.tanqueo.ui.home.HomeActions
import com.tanqueo.ui.home.HomeScreen
import com.tanqueo.ui.menu.MenuActions
import com.tanqueo.ui.menu.MenuScreen
import com.tanqueo.ui.odometer.UpdateKmScreen
import com.tanqueo.ui.register.RegisterScreen
import com.tanqueo.ui.settings.SettingsScreen
import com.tanqueo.ui.summary.SummaryScreen

object Routes {
    const val HOME = "inicio"
    const val REGISTER = "registrar"
    private const val REGISTER_ARG = "dictado"
    const val REGISTER_PATTERN = "registrar?$REGISTER_ARG={$REGISTER_ARG}"
    const val REGISTER_ARG_NAME = REGISTER_ARG
    private const val ASK_ARG = "escuchar"
    const val ASK_PATTERN = "preguntar?$ASK_ARG={$ASK_ARG}"
    const val ASK_ARG_NAME = ASK_ARG
    const val MENU = "menu"
    const val HISTORY = "historial"
    const val DETAIL = "historial/{${FillUpDetailViewModel.ARG_ID}}"
    const val SUMMARY = "resumen"
    const val UPDATE_KM = "actualizar_km"
    const val SETTINGS = "ajustes"
    const val BUDGET = "cuanto_me_alcanza"

    /** Abre el asistente con la entrada ya lista: preguntar toma un solo toque. */
    fun askNow() = "preguntar?$ASK_ARG=true"

    fun detail(id: Long) = "historial/$id"

    /** Abre el registro con una frase ya dicha, para que el usuario solo revise. */
    fun registerWith(text: String) = "registrar?$REGISTER_ARG=${Uri.encode(text)}"
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
                    onMenu = { go(Routes.MENU) },
                    onAsk = { go(Routes.askNow()) },
                ),
            )
        }
        composable(Routes.MENU) {
            MenuScreen(
                MenuActions(
                    onAddFillUp = { go(Routes.REGISTER) },
                    onAsk = { go(Routes.askNow()) },
                    onUpdateKm = { go(Routes.UPDATE_KM) },
                    onHistory = { go(Routes.HISTORY) },
                    onSummary = { go(Routes.SUMMARY) },
                    onBudget = { go(Routes.BUDGET) },
                    onSettings = { go(Routes.SETTINGS) },
                ),
            )
        }
        composable(
            route = Routes.REGISTER_PATTERN,
            arguments = listOf(
                navArgument(Routes.REGISTER_ARG_NAME) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            RegisterScreen(
                initialDictation = entry.arguments?.getString(Routes.REGISTER_ARG_NAME),
                onSaved = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(
            route = Routes.ASK_PATTERN,
            arguments = listOf(
                navArgument(Routes.ASK_ARG_NAME) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            AskScreen(
                listenOnOpen = entry.arguments?.getBoolean(Routes.ASK_ARG_NAME) == true,
                onLogFillUp = { text ->
                    navController.navigate(Routes.registerWith(text)) {
                        popUpTo(Routes.ASK_PATTERN) { inclusive = true }
                    }
                },
            )
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
        composable(Routes.BUDGET) { BudgetScreen() }
    }
}
