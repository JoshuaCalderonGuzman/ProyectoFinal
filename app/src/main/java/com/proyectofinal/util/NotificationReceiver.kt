package com.proyectofinal.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.proyectofinal.MainActivity
import com.proyectofinal.R

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "task_reminders_channel"
        const val NOTIFICATION_UNIQUE_ID_KEY = "notification_unique_id" // ID único del PendingIntent/Notificación (item.id + hash)
        const val NOTIFICATION_TITLE_KEY = "notification_title"
        const val NOTIFICATION_DESC_KEY = "notification_description"
        const val NAV_ITEM_ID_KEY = "navigation_item_id" // <<-- CLAVE PARA EL ID DEL ÍTEM PARA NAVEGACIÓN
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NOTIFICATION_DEBUG", "NOTIFICATION RECEIVER ACTIVADO")

        // 1. Extraer los datos de la tarea
        val pendingId = intent.getIntExtra(NOTIFICATION_UNIQUE_ID_KEY, 0)
        // Usamos el recurso si el extra es nulo
        val itemIdForNav = intent.getIntExtra(NAV_ITEM_ID_KEY, 0) // <<-- Extraer el ID para navegación
        val title = intent.getStringExtra(NOTIFICATION_TITLE_KEY) ?: context.getString(R.string.recordatorio_tarea)
        var description: String = intent.getStringExtra(NOTIFICATION_DESC_KEY) ?: context.getString(R.string.tienes_una_tarea_pendiente)

        Log.d("NOTIFICATION_DEBUG", " Datos recibidos:")
        Log.d("NOTIFICATION_DEBUG", "Unique ID (Pending) = $pendingId")
        Log.d("NOTIFICATION_DEBUG", "Item ID (Nav) = $itemIdForNav")
        Log.d("NOTIFICATION_DEBUG", "Título = $title")

        // 2. Crear el CANAL
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.canal_recordatorios)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = context.getString(R.string.descripcion_canal_recordatorios)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 3. Crear el INTENT DE NAVEGACIÓN a MainActivity
        val navIntent = Intent(context, MainActivity::class.java).apply {
            // Pasar el ID del ítem para que MainActivity sepa qué cargar
            putExtra(NAV_ITEM_ID_KEY, itemIdForNav)
            // Flags para evitar que se creen múltiples instancias de la Activity
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        // 4. Crear el PendingIntent que se disparará al tocar la notificación
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            pendingId, // Reutilizar el ID único de la notificación como requestCode
            navIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        // 5. Construir y mostrar la notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.proyectofinal.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent) // <<-- ASIGNAR EL INTENT DE NAVEGACIÓN
            .build()

        // Mostrar la notificación
        notificationManager.notify(pendingId, notification) // Usar el pendingId como ID de la notificación
        Log.d("NOTIFICATION_DEBUG", " Notificación mostrada correctamente (ID = $pendingId)")
    }
}