package com.proyectofinal


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyectofinal.data.AppDatabase
import com.proyectofinal.data.ItemRepository
import com.proyectofinal.navigation.AppNavigation // New
import com.proyectofinal.viewmodel.ItemViewModel
import com.proyectofinal.viewmodel.ItemViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val database = AppDatabase.getDatabase(LocalContext.current)
            val repository = ItemRepository(database.itemDao())
            val factory = ItemViewModelFactory(repository)
            val viewModel = viewModel<ItemViewModel>(factory = factory)
            AppNavigation(viewModel)
        }
    }
}


