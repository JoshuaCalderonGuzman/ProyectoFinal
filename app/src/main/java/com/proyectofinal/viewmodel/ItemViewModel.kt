package com.proyectofinal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proyectofinal.data.Item
import com.proyectofinal.data.ItemRepository
import kotlinx.coroutines.launch

class ItemViewModel(private val repository: ItemRepository) : ViewModel() {
    //Manejar el estado de las pantallas en el viewmodel, obj uistate
    val allNotes = repository.allNotes
    val allTasks = repository.allTasks

    var currentItem: Item? = null

    fun loadItem(itemId: Int) = viewModelScope.launch {
        currentItem = repository.getItemById(itemId)
    }

    fun saveItem(item: Item) = viewModelScope.launch {
        if (item.id == 0) {
            repository.insert(item) // Add
        } else {
            repository.update(item) // Edit
        }
    }

    fun deleteItem(item: Item) = viewModelScope.launch {
        repository.delete(item) // Delete
    }

    fun toggleTaskCompletion(task: Item) = viewModelScope.launch {
        repository.toggleTaskCompletion(task)
    }
}

class ItemViewModelFactory(private val repository: ItemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ItemViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}