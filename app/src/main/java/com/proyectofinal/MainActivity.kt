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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Obtiene la aplicación personalizada
            val application = LocalContext.current.applicationContext as InventoryApplication
            val repository = application.container.itemsRepository

            // Factory con el repositorio inyectado
            val viewModel: ItemViewModel = viewModel(
                factory = ItemViewModelFactory(repository)
            )

            AppNavigation(viewModel = viewModel)
        }
    }
}


