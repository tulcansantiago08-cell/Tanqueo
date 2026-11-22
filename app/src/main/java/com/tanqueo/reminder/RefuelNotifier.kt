package com.tanqueo.reminder

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tanqueo.MainActivity
import com.tanqueo.R
import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.model.VehicleType
import com.tanqueo.ui.components.rangeFull
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class RefuelNotifier @Inject constructor(@param:ApplicationContext private val context: Context) {

    @SuppressLint("MissingPermission") // Se revisa con areNotificationsEnabled().
    fun notify(vehicle: VehicleType, estimate: RangeEstimate.Available) {
        // Sirve en todas las versiones: por debajo de API 33 el permiso POST_NOTIFICATIONS no existe.
        val notifications = NotificationManagerCompat.from(context)
        if (!notifications.areNotificationsEnabled()) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(vehicle.reminderTitle))
            .setContentText(context.resources.rangeFull(estimate))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            // Se revisa cada 12 h: si el aviso sigue a la vista, solo se actualiza el texto sin volver a vibrar.
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp(vehicle, Destination.HOME))
            .addAction(R.drawable.ic_add, context.getString(R.string.action_add_fill_up), openApp(vehicle, Destination.REGISTER))
            .addAction(R.drawable.ic_map, context.getString(R.string.action_gas_stations), openApp(vehicle, Destination.GAS_STATIONS))
            .build()
        notifications.notify(vehicle.notificationId, notification)
    }

    /** Ya no hace falta tanquear ese vehículo: su aviso anterior no debe quedarse. */
    fun cancel(vehicle: VehicleType) = NotificationManagerCompat.from(context).cancel(vehicle.notificationId)

    /** Abre la app con ese vehículo activo, para que "+ Tanqueo" lo anote donde corresponde. */
    private fun openApp(vehicle: VehicleType, destination: Destination): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_VEHICLE, vehicle.name)
        destination.extra?.let { intent.putExtra(MainActivity.EXTRA_DESTINATION, it) }
        return PendingIntent.getActivity(
            context,
            vehicle.ordinal * Destination.entries.size + destination.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** A dónde lleva cada parte del aviso. */
    private enum class Destination(val extra: String?) {
        HOME(null),
        REGISTER(MainActivity.DESTINATION_REGISTER),
        GAS_STATIONS(MainActivity.DESTINATION_GAS_STATIONS),
    }

    private companion object {
        const val CHANNEL_ID = "aviso_tanqueo"

        /** El carro conserva el 1 que ya usaba el único aviso. */
        val VehicleType.notificationId: Int get() = 1 + ordinal

        val VehicleType.reminderTitle: Int
            get() = when (this) {
                VehicleType.CARRO -> R.string.reminder_title_car
                VehicleType.MOTO -> R.string.reminder_title_moto
            }
    }
}
