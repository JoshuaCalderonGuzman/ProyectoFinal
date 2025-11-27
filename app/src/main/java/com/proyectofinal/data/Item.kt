package com.proyectofinal.data

import androidx.room.ColumnInfo
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
    @ColumnInfo(name = "reminderTimestamps") // Nuevo nombre para claridad
    val reminderTimestamps: List<Long> = emptyList(),

    val dueDateTimestamp: Long?,

    val photoPaths: List<String>,
    val videoPaths: List<String>,
    val audioPaths: List<String>,
    val filePaths: List<String>,




)