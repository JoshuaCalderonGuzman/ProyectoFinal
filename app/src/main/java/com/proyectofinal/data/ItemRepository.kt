package com.proyectofinal.data

import kotlinx.coroutines.flow.Flow

interface ItemsRepository {
    fun getAllNotes(): Flow<List<Item>>
    fun getAllTasks(): Flow<List<Item>>
    suspend fun getItemById(id: Int): Item?

    // ⬇️ MODIFICADO: Ahora devuelve Long (el ID generado) ⬇️
    suspend fun insert(item: Item): Long

    suspend fun update(item: Item)
    suspend fun delete(item: Item)
}