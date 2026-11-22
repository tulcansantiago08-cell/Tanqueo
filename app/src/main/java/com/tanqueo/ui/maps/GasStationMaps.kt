package com.tanqueo.ui.maps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.tanqueo.domain.stations.GeoPoint
import java.util.Locale

/**
 * Google Maps (o cualquier app de mapas) en el reloj, solo si está instalada. Si no lo está, Tanqueo
 * no manda a la tienda ni al teléfono: usa su propio mapa, que funciona sin cuenta ni teléfono.
 */
object GasStationMaps {
    const val QUERY = "gasolineras"
    private const val GOOGLE_MAPS = "com.google.android.apps.maps"

    /** "geo:0,0?q=…" busca alrededor de la ubicación actual. */
    fun watchUri(): Uri = ("geo:0,0?q=" + Uri.encode(QUERY)).toUri()

    /** Una gasolinera de la lista, marcada en el mapa con su nombre. */
    fun placeWatchUri(point: GeoPoint, label: String): Uri {
        val coordinates = String.format(Locale.US, "%.6f,%.6f", point.latitude, point.longitude)
        return ("geo:$coordinates?q=" + Uri.encode("$coordinates($label)")).toUri()
    }

    /** Si hay en el reloj una app que abra "geo:". */
    fun isAvailable(context: Context): Boolean =
        context.packageManager.resolveActivity(Intent(Intent.ACTION_VIEW, watchUri()), PackageManager.MATCH_DEFAULT_ONLY) != null

    fun open(context: Context): Boolean = start(context, watchUri())

    fun openPlace(context: Context, point: GeoPoint, label: String): Boolean = start(context, placeWatchUri(point, label))

    /** Primero Google Maps; si no, la app de mapas que haya. [context] es la actividad: "atrás" vuelve a Tanqueo. */
    private fun start(context: Context, uri: Uri): Boolean {
        val view = Intent(Intent.ACTION_VIEW, uri)
        return tryStart(context, Intent(view).setPackage(GOOGLE_MAPS)) || tryStart(context, view)
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

/** Si el reloj tiene app de mapas. Se revisa al entrar a la pantalla. */
@Composable
fun rememberMapsAvailable(): Boolean {
    val context = LocalContext.current
    return remember(context) { GasStationMaps.isAvailable(context) }
}
