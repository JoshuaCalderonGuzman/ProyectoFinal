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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

object Routes {
    const val HOME = "home"
    const val NOTA_DETAIL = "nota_detail/{itemId}"

    fun createNotaDetailRoute(itemId: Int) = "nota_detail/$itemId"
}

@Composable
fun AppNavigation(
    viewModel: ItemViewModel,
    windowSize: WindowWidthSizeClass,
    navigationType: NavigationType,
    startItemId: Int = 0, // ID que viene de la notificación
    contentType: ContentType
) {
    val navController = rememberNavController()

    // Estado para evitar bucles de navegación
    var initialNavigationHandled by rememberSaveable { mutableStateOf(false) }

    // === LÓGICA DE NOTIFICACIÓN (REPLICA LA LÓGICA DE "EDITAR") ===
    LaunchedEffect(startItemId) {
        if (startItemId > 0 && !initialNavigationHandled) {

            // 1. Cargar el ítem (Igual que hace el botón editar)
            viewModel.loadItemById(startItemId)

            // 2. Determinar la navegación según el tipo de pantalla
            if (contentType == ContentType.LIST_ONLY) {
                // MÓVIL: Navegar a la pantalla de detalle
                navController.navigate(Routes.createNotaDetailRoute(startItemId)) {
                    // Mantenemos el HOME en el historial para que al dar "Atrás" vuelvas a la lista
                    popUpTo(Routes.HOME) { inclusive = false }
                }
            }

            initialNavigationHandled = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        // --- PANTALLA PRINCIPAL ---
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                contentType = contentType,
                navigationType = navigationType,

                // ESTA ES LA FUNCIÓN "EDITAR" ORIGINAL
                onNoteClick = { itemId ->
                    viewModel.loadItem(itemId) // Carga datos
                    if (contentType == ContentType.LIST_ONLY) {
                        navController.navigate(Routes.createNotaDetailRoute(itemId)) // Navega
                    }
                },

                onAddNewClick = {
                    viewModel.clearCurrentItem()
                    if (contentType == ContentType.LIST_ONLY) {
                        navController.navigate(Routes.createNotaDetailRoute(0))
                    }
                }
            )
        }

        // --- PANTALLA DETALLE ---
        composable(
            route = Routes.NOTA_DETAIL,
            arguments = listOf(navArgument("itemId") { type = NavType.IntType })
        ) { backStackEntry ->

            val itemId = backStackEntry.arguments?.getInt("itemId") ?: 0

            // Asegurar carga de datos si llegamos por navegación normal
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