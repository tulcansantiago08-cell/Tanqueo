package com.tanqueo

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * En debug se certifica con un token registrado en Firebase console → App Check. Se fija desde
 * local.properties para que no cambie al reinstalar ni al limpiar los datos de la app.
 */
fun installAppCheck(context: Context) {
    // Sin google-services.json no hay Firebase: la app sigue con el lector local.
    if (FirebaseApp.getApps(context).isEmpty()) return
    val token = BuildConfig.APP_CHECK_DEBUG_TOKEN
    if (token.isNotBlank()) {
        val store = "com.google.firebase.appcheck.debug.store.${FirebaseApp.getInstance().persistenceKey}"
        context.getSharedPreferences(store, Context.MODE_PRIVATE).edit {
            putString("com.google.firebase.appcheck.debug.DEBUG_SECRET", token)
        }
    }
    Firebase.appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
}
