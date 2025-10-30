package com.proyectofinal


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyectofinal.data.AppDatabase
import com.proyectofinal.data.ItemsRepository
import com.proyectofinal.navigation.AppNavigation // New
import com.proyectofinal.viewmodel.ItemViewModel
import com.proyectofinal.viewmodel.ItemViewModelFactory
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.proyectofinal.ui.utils.ContentType
import com.proyectofinal.ui.utils.NavigationType

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

            // Factory con el repositorio inyectado
            val viewModel: ItemViewModel = viewModel(
                factory = ItemViewModelFactory(repository)
            )

            AppNavigation(viewModel = viewModel,windowSize = widthSize,
                navigationType = navigationType,
                contentType = contentType)
        }
    }
}


