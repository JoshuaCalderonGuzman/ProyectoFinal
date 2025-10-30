package com.proyectofinal.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.proyectofinal.HomeScreen
import com.proyectofinal.NotaScreen
import com.proyectofinal.viewmodel.ItemViewModel
import com.proyectofinal.ui.utils.ContentType
import com.proyectofinal.ui.utils.NavigationType
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.LaunchedEffect

object Routes {
    // La ruta de la pantalla principal
    const val HOME = "home"

    // itemId = 0 significa que es un nuevo elemento.
    // itemId > 0 significa que es un elemento existente para editar.
    const val NOTA_DETAIL = "nota_detail/{itemId}"

    // Función helper para construir la ruta con el ID
    fun createNotaDetailRoute(itemId: Int) = "nota_detail/$itemId"
}

@Composable
fun AppNavigation(viewModel: ItemViewModel,
                  windowSize: WindowWidthSizeClass,
                  navigationType: NavigationType,
                  contentType: ContentType) {
    val navController = rememberNavController() // Controlador de navegación

    NavHost(
        navController = navController,
        startDestination = Routes.HOME // La pantalla inicial
    ) {
        // Ruta de la Pantalla Principal (HOME)
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                contentType = contentType,
                navigationType = navigationType,
                // Función para navegar a un ítem existente (nota o tarea)
                onNoteClick = { itemId ->
                    viewModel.loadItem(itemId)
                    if (contentType == ContentType.LIST_ONLY) {
                        navController.navigate(Routes.createNotaDetailRoute(itemId))
                    }
                },
                // Función para navegar y crear un nuevo ítem (se pasa ID = 0)
                onAddNewClick = {
                    viewModel.clearCurrentItem()
                    if (contentType == ContentType.LIST_ONLY) {
                        navController.navigate(Routes.createNotaDetailRoute(0))
                    }
                }
            )
        }

        // 2. Ruta de la Pantalla de Detalle (NOTA_DETAIL)
        composable(
            route = Routes.NOTA_DETAIL,
            // Definición del argumento 'itemId' como entero
            arguments = listOf(navArgument("itemId") { type = NavType.IntType })
        ) { backStackEntry ->

            // Extrae el itemId de los argumentos, por defecto 0 si no existe
            val itemId = backStackEntry.arguments?.getInt("itemId") ?: 0
            LaunchedEffect(itemId) {
                if (itemId > 0) viewModel.loadItem(itemId)
            }

            if (contentType == ContentType.LIST_ONLY) {
                NotaScreen(
                    itemId = itemId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }

        }
    }
}