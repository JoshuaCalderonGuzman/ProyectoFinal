package com.proyectofinal

import android.app.Application
import com.proyectofinal.data.AppDataContainer
import com.proyectofinal.data.AppContainer

class InventoryApplication : Application() {
    lateinit var container: AppContainer
    //Crea el AppDataContrainer
    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}