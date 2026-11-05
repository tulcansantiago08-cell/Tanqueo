package com.tanqueo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import com.tanqueo.ui.home.HomeActions
import com.tanqueo.ui.home.HomeScreen

object Routes {
    const val HOME = "inicio"
}

@Composable
fun TanqueoNavHost(navController: NavHostController) {
    SwipeDismissableNavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                HomeActions(
                    onAddFillUp = {},
                    onHistory = {},
                    onSummary = {},
                    onUpdateKm = {},
                    onSettings = {},
                ),
            )
        }
    }
}
