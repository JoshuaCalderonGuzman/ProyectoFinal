package com.proyectofinal.data

import kotlinx.coroutines.flow.Flow

class OfflineItemsRepository(private val itemDao: ItemDao) : ItemsRepository {
    override fun getAllNotes() = itemDao.getAllNotes()
    override fun getAllTasks() = itemDao.getAllTasks()
    override suspend fun getItemById(id: Int) = itemDao.getItemById(id)
    override suspend fun insert(item: Item) = itemDao.insert(item)
    override suspend fun update(item: Item) = itemDao.update(item)
    override suspend fun delete(item: Item) = itemDao.delete(item)
}