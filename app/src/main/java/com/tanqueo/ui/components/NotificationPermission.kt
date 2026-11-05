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

private fun Context.notificationsEnabled() = NotificationManagerCompat.from(this).areNotificationsEnabled()

/**
 * Pide el permiso de notificaciones al abrir la app si todavía no se ha concedido. Por debajo de
 * API 33 las notificaciones ya están activas y no se llega a pedir.
 */
@SuppressLint("InlinedApi")
@Composable
fun RequestNotificationPermissionOnce() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    var asked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!asked && !context.notificationsEnabled()) {
            asked = true
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
