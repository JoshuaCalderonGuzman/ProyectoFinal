package com.proyectofinal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyectofinal.navigation.AppNavigation
import com.proyectofinal.viewmodel.ItemViewModel
import com.proyectofinal.viewmodel.ItemViewModelFactory
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.proyectofinal.ui.utils.ContentType
import com.proyectofinal.ui.utils.NavigationType
import android.app.Application
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.provider.Settings
import android.app.AlarmManager
import android.content.Context
import com.proyectofinal.util.NotificationReceiver // Importar el Receiver para la clave
import android.util.Log // Importar para logging

class MainActivity : ComponentActivity() {

    // Estado para el ID del ítem que debe cargarse al iniciar (por notificación)
    // Se usa como campo de clase para poder actualizarlo en onNewIntent
    private var notificationItemId: Int = 0

    // 1. Crear el lanzador de permisos (para POST_NOTIFICATIONS)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Lógica de seguimiento para POST_NOTIFICATIONS
    }

    // 2. Lanzador para manejar el resultado de ir a la configuración (Alarma Exacta)
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Lógica de seguimiento después de que el usuario regrese de Ajustes
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 3. Obtener el ID del ítem que inicia la actividad (si viene de una notificación)
        notificationItemId = intent.getIntExtra(NotificationReceiver.NAV_ITEM_ID_KEY, 0)
        Log.d("MainActivity", "Activity started with notification item ID: $notificationItemId")

        // 4. Solicitar el permiso de notificación (Android 13+)
        requestNotificationPermission()

        // 5. SOLICITAR PERMISO DE ALARMA EXACTA (Android 12+)
        requestExactAlarmPermission()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val widthSize = windowSizeClass.widthSizeClass

            val navigationType = when (widthSize) {
                WindowWidthSizeClass.Compact -> NavigationType.BOTTOM_NAVIGATION
                WindowWidthSizeClass.Medium -> NavigationType.NAVIGATION_RAIL
                WindowWidthSizeClass.Expanded -> NavigationType.PERMANENT_NAVIGATION_DRAWER
                else -> NavigationType.BOTTOM_NAVIGATION
            }

            val contentType = if (widthSize == WindowWidthSizeClass.Expanded) {
                ContentType.LIST_AND_DETAIL
            } else {
                ContentType.LIST_ONLY
            }

            // Obtiene la aplicación personalizada
            val application = LocalContext.current.applicationContext as InventoryApplication
            val repository = application.container.itemsRepository

            // Factory con el repositorio y el objeto application inyectados
            val viewModel: ItemViewModel = viewModel(
                factory = ItemViewModelFactory(repository, application)
            )

            AppNavigation(
                viewModel = viewModel,
                windowSize = widthSize,
                navigationType = navigationType,
                contentType = contentType,
                startItemId = notificationItemId // <<-- ¡Asegúrate de añadir este parámetro en AppNavigation!
            )

            // Limpiar el ID después de pasarlo para evitar lanzamientos repetidos
            notificationItemId = 0
        }
    }

    /**
     * Maneja un nuevo Intent que llega a una Activity ya existente (p. ej. una segunda notificación).
     * CORRECCIÓN: Se usa la firma fun onNewIntent(intent: Intent) para evitar el error de compilación.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Volver a leer el ID del Intent entrante y actualizar el campo de clase
        // Como 'intent' es no nulo, leemos directamente el extra.
        notificationItemId = intent.getIntExtra(NotificationReceiver.NAV_ITEM_ID_KEY, 0)
        Log.d("MainActivity", "Received new Intent with item ID: $notificationItemId")

        // El nuevo setContent (Compose) se encargará de leer la variable actualizada
    }

    // Función para solicitar el permiso de notificaciones (Android 13+)
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Solicita al usuario que conceda el permiso SCHEDULE_EXACT_ALARM (Alarmas Exactas)
     * requerido en Android 12 (API 31) y superior para usar setExactAndAllowWhileIdle().
     */
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 (API 31)
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (!alarmManager.canScheduleExactAlarms()) {
                // Si el permiso NO está concedido, redirigimos al usuario a la configuración.
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                settingsLauncher.launch(intent)
            }
        }
    }
}