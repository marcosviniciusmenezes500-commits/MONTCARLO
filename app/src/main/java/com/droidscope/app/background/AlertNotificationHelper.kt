package com.droidscope.app.background

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun notifyHighTemperature(tempC: Double, thresholdC: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Alerta Térmico — DroidScope")
            .setContentText(
                String.format(
                    Locale.US,
                    "Bateria a %.1f °C (limite configurado: %.0f °C).",
                    tempC,
                    thresholdC
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TEMP, notification)
        }
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas de Hardware e Bateria",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificações de temperatura elevada e anomalias detectadas em segundo plano."
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "droidscope_alerts"
        private const val NOTIFICATION_ID_TEMP = 1001
    }
}
