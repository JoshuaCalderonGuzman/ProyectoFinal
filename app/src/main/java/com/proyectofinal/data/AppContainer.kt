package com.proyectofinal.data

import android.content.Context


//Inyecta el repositorio
interface AppContainer {
    val itemsRepository: ItemsRepository
}
//Crea y provee el repositorio de datos
class AppDataContainer(private val context: Context) : AppContainer {
    override val itemsRepository: ItemsRepository by lazy {
        OfflineItemsRepository(AppDatabase.getDatabase(context).itemDao())
    }
}