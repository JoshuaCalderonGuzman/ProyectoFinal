package com.proyectofinal.data

import kotlinx.coroutines.flow.Flow

class ItemRepository(private val itemDao: ItemDao) {

    //Listar notas/tareas
    val allNotes: Flow<List<Item>> = itemDao.getAllNotes()
    val allTasks: Flow<List<Item>> = itemDao.getAllTasks()

    //Agregar (Nota/Tarea)
    suspend fun insert(item: Item) {
        itemDao.insert(item)
    }

    //Editar (Nota/Tarea)
    suspend fun update(item: Item) {
        itemDao.update(item)
    }

    //Eliminar (Nota/Tarea)
    suspend fun delete(item: Item) {
        itemDao.delete(item)
    }

    //Ver (Nota/Tarea)
    suspend fun getItemById(id: Int): Item? {
        return itemDao.getItemById(id)
    }

    suspend fun toggleTaskCompletion(task: Item) {
        if (task.isTask) {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            itemDao.update(updatedTask)
        }
    }
}