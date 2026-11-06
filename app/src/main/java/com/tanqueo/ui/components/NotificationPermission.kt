package com.tanqueo.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

private const val PERMISSION_PREFS = "permisos"
private const val KEY_ASKED = "notificaciones_pedidas"

private fun Context.notificationsEnabled() = NotificationManagerCompat.from(this).areNotificationsEnabled()

/**
 * Pide el permiso de notificaciones una sola vez en la vida de la app: cerrar el diálogo sin elegir
 * no debe hacerlo reaparecer en cada apertura. Después se activa desde Ajustes. Por debajo de API 33
 * las notificaciones ya están activas y no se llega a pedir.
 */
@SuppressLint("InlinedApi")
@Composable
fun RequestNotificationPermissionOnce() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ASKED, false) && !context.notificationsEnabled()) {
            prefs.edit().putBoolean(KEY_ASKED, true).apply()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

data class NotificationAccess(val enabled: Boolean, val request: () -> Unit)

/**
 * Estado del permiso, revisado al volver a la pantalla. El primer intento muestra el diálogo del
 * sistema; si ya no sirve (el usuario lo negó), el siguiente abre los ajustes de la app.
 */
@SuppressLint("InlinedApi")
@Composable
fun rememberNotificationAccess(): NotificationAccess {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(context.notificationsEnabled()) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        enabled = context.notificationsEnabled()
    }
    LifecycleResumeEffect(Unit) {
        enabled = context.notificationsEnabled()
        onPauseOrDispose {}
    }
    return NotificationAccess(enabled) {
        if (!asked) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        }
    }
}
