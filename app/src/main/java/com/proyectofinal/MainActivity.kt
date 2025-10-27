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
            // Setup the data/architecture dependencies at the highest level
            val context = LocalContext.current
            val database = AppDatabase.getDatabase(context)
            val repository = ItemRepository(database.itemDao())
            val viewModelFactory = ItemViewModelFactory(repository)

            //Pass the ViewModel to the root of the Navigation Graph
            AppContent(viewModelFactory)
        }
    }
}

@Composable
fun AppContent(viewModelFactory: ItemViewModelFactory) {
    // Obtain the ViewModel instance
    val viewModel: ItemViewModel = viewModel(factory = viewModelFactory)

    // Setup Navigation (Next step)
    AppNavigation(viewModel)
}

