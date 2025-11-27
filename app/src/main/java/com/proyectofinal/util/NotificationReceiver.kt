package com.proyectofinal.util // Puedes ponerlo en un paquete 'util'

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.proyectofinal.R // Asegúrate de tener tu archivo R

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "task_reminders_channel"
        const val NOTIFICATION_ID_KEY = "notification_id"
        const val NOTIFICATION_TITLE_KEY = "notification_title"
        const val NOTIFICATION_DESC_KEY = "notification_description"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 1. Extraer los datos de la tarea del Intent
        val notificationId = intent.getIntExtra(NOTIFICATION_ID_KEY, 0)
        val title = intent.getStringExtra(NOTIFICATION_TITLE_KEY) ?: context.getString(R.string.recordatorio_tarea)
        var description: String = intent.getStringExtra(NOTIFICATION_DESC_KEY) ?: context.getString(R.string.tienes_una_tarea_pendiente)

        // 2. Crear y mostrar la notificación
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear el canal de notificación (obligatorio en Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.canal_recordatorios)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = context.getString(R.string.descripcion_canal_recordatorios)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Construir la notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.proyectofinal.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Se cierra al tocarla
            .build()

        // Mostrar la notificación
        notificationManager.notify(notificationId, notification)
    }
}