package com.proyectofinal.data

import androidx.room.TypeConverter
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson

class Converters {
    private val gson = Gson()

    // Convierte una lista de strings (rutas) a un solo String JSON para la base de datos
    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return gson.toJson(list)
    }

    // Convierte el String JSON de la base de datos de vuelta a List<String> (rutas)
    @TypeConverter
    fun toStringList(data: String): List<String> {
        // Define el tipo de dato que Gson debe esperar
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(data, listType)
    }
}