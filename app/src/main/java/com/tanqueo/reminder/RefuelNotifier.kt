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
import com.tanqueo.ui.components.rangeFull
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class RefuelNotifier @Inject constructor(@param:ApplicationContext private val context: Context) {

    @SuppressLint("MissingPermission") // Se revisa con areNotificationsEnabled().
    fun notify(estimate: RangeEstimate.Available) {
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
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.resources.rangeFull(estimate))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp(register = false))
            .addAction(R.drawable.ic_add, context.getString(R.string.action_add_fill_up), openApp(register = true))
            .build()
        notifications.notify(NOTIFICATION_ID, notification)
    }

    private fun openApp(register: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        if (register) intent.putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DESTINATION_REGISTER)
        return PendingIntent.getActivity(
            context,
            if (register) 1 else 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val CHANNEL_ID = "aviso_tanqueo"
        const val NOTIFICATION_ID = 1
    }
}
