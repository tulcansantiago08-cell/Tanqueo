package com.tanqueo.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tanqueo.MainActivity
import com.tanqueo.R
import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.ui.components.rangeFull
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class RefuelNotifier @Inject constructor(@param:ApplicationContext private val context: Context) {

    fun notify(estimate: RangeEstimate.Available) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return

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
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun openApp(register: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
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
