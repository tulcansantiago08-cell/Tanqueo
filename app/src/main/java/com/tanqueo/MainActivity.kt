package com.tanqueo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.tanqueo.ui.navigation.TanqueoNavHost
import com.tanqueo.ui.theme.TanqueoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TanqueoTheme {
                // Sin TimeText del sistema: cada pantalla trae su encabezado de hora y fecha.
                AppScaffold(timeText = {}) {
                    TanqueoNavHost(rememberSwipeDismissableNavController())
                }
            }
        }
    }
}
