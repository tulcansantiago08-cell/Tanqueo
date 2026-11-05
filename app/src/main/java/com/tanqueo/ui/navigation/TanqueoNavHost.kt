package com.tanqueo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import com.tanqueo.ui.home.HomeActions
import com.tanqueo.ui.home.HomeScreen
import com.tanqueo.ui.register.RegisterScreen

object Routes {
    const val HOME = "inicio"
    const val REGISTER = "registrar"
}

@Composable
fun TanqueoNavHost(navController: NavHostController) {
    SwipeDismissableNavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                HomeActions(
                    onAddFillUp = { navController.navigate(Routes.REGISTER) { launchSingleTop = true } },
                    onHistory = {},
                    onSummary = {},
                    onUpdateKm = {},
                    onSettings = {},
                ),
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(onSaved = { navController.popBackStack(Routes.HOME, inclusive = false) })
        }
    }
}
