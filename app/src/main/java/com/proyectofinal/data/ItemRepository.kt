package com.proyectofinal.data

import kotlinx.coroutines.flow.Flow

interface ItemsRepository {
    fun getAllNotes(): Flow<List<Item>>
    fun getAllTasks(): Flow<List<Item>>
    suspend fun getItemById(id: Int): Item?
    suspend fun insert(item: Item)
    suspend fun update(item: Item)
    suspend fun delete(item: Item)
}