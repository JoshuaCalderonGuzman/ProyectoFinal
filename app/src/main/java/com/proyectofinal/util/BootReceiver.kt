package com.proyectofinal.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.proyectofinal.InventoryApplication // Asegúrate de que esta sea tu clase Application
import com.proyectofinal.viewmodel.ItemViewModel

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Dispositivo Reiniciado. Reprogramando recordatorios.")

            // 1. Obtener acceso al Repositorio a través de la clase Application
            val app = context.applicationContext as? InventoryApplication

            // Verificación importante: El sistema puede llamar esto antes de que Application esté lista
            if (app != null) {
                val repository = app.container.itemsRepository

                // 2. Crear una instancia del ViewModel solo para la lógica de reprogramación
                // Usamos el constructor directamente para llamar a la función de forma asíncrona.
                val tempViewModel = ItemViewModel(repository, app)
                tempViewModel.rescheduleAllReminders()
            } else {
                Log.e("BootReceiver", "No se pudo obtener la clase Application para reprog. de recordatorios.")
            }
        }
    }
}