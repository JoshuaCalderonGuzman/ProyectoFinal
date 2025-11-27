package com.proyectofinal.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String? = null,
    val isTask: Boolean = false,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    // ⬇️ DEBES AGREGAR ESTE CAMPO PARA LA FECHA LÍMITE ⬇️
    val dueDateTimestamp: Long?,

    val photoPaths: List<String>,
    val videoPaths: List<String>,
    val audioPaths: List<String>,
    val filePaths: List<String>


)