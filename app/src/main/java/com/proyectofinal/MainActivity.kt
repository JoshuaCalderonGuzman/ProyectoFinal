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
import android.app.Application // Asegurarse de importar Application
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts


class MainActivity : ComponentActivity() {

    // 1. Crear el lanzador de permisos (Debe estar fuera de onCreate/setContent)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Aquí puedes poner lógica de seguimiento si el permiso fue concedido o denegado
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Solicitar el permiso de notificación al iniciar la Activity
        requestNotificationPermission()

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
                // ⬇️ CORRECCIÓN del error 'No value passed for parameter application' ⬇️
                factory = ItemViewModelFactory(repository, application)
            )

            AppNavigation(viewModel = viewModel,windowSize = widthSize,
                navigationType = navigationType,
                contentType = contentType)
        }
    }

    // 3. Función para solicitar el permiso de notificaciones (Android 13+)
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU = Android 13 (API 33)
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}