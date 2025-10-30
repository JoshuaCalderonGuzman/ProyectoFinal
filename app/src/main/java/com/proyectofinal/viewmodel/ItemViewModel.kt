package com.proyectofinal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proyectofinal.data.Item
import com.proyectofinal.data.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

//Sealed class para representar los estados de la UI
sealed class ItemUiState {
    object Loading : ItemUiState()
    data class Success(val items: List<Item>) : ItemUiState()
    data class Error(val message: String) : ItemUiState()
    object Empty : ItemUiState()
}
//manejar los estados del viewmodel, inventory
class ItemViewModel(private val repository: ItemRepository) : ViewModel() {

    // Estado combinado para listas notas + tareas
    private val _uiState = MutableStateFlow<ItemUiState>(ItemUiState.Loading)
    val uiState: StateFlow<ItemUiState> = _uiState.asStateFlow()

    // Estado para el ítem actual editar/ver detalle
    private val _currentItemState = MutableStateFlow<Item?>(null)
    val currentItemState: StateFlow<Item?> = _currentItemState.asStateFlow()

    // Cargar todas las notas y tareas
    init {
        loadAllItems()
    }

    fun loadAllItems() {
        viewModelScope.launch {
            try {
                repository.allNotes.collect { notes ->
                    repository.allTasks.collect { tasks ->
                        val allItems = notes + tasks
                        _uiState.value = if (allItems.isEmpty()) {
                            ItemUiState.Empty
                        } else {
                            ItemUiState.Success(allItems)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ItemUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun loadItem(itemId: Int) {
        viewModelScope.launch {
            try {
                val item = repository.getItemById(itemId)
                if (item != null) {
                    _currentItemState.value = item

                } else {
                    _uiState.value = ItemUiState.Error("Elemento no encontrado")
                }
            } catch (e: Exception) {
                _uiState.value = ItemUiState.Error(e.message ?: "Error al cargar")
            }
        }
    }

    fun saveItem(item: Item) {
        viewModelScope.launch {
            try {
                if (item.id == 0) {
                    repository.insert(item)
                } else {
                    repository.update(item)
                }
                // Recargar lista después de guardar
                loadAllItems()
                _currentItemState.value = null // Limpiar detalle
            } catch (e: Exception) {
                _uiState.value = ItemUiState.Error("Error al guardar: ${e.message}")
            }
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            try {
                repository.delete(item)
                loadAllItems() // Actualizar lista
                if (_currentItemState.value?.id == item.id) {
                    _currentItemState.value = null
                }
            } catch (e: Exception) {
                _uiState.value = ItemUiState.Error("Error al eliminar: ${e.message}")
            }
        }
    }

    fun toggleTaskCompletion(task: Item) {
        viewModelScope.launch {
            try {
                repository.toggleTaskCompletion(task)
                loadAllItems() // Refrescar estado
            } catch (e: Exception) {
                _uiState.value = ItemUiState.Error("Error al actualizar tarea")
            }
        }
    }

    // Limpiar estado de ítem actual
    fun clearCurrentItem() {
        _currentItemState.value = null
    }
}

// Factory
class ItemViewModelFactory(private val repository: ItemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ItemViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}