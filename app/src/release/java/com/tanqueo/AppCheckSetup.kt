package com.tanqueo

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/** En release, Play Integrity certifica que las peticiones vienen de la app instalada desde Play. */
fun installAppCheck(context: Context) {
    if (FirebaseApp.getApps(context).isEmpty()) return
    Firebase.appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
}
