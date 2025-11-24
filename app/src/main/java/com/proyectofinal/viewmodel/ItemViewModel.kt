package com.proyectofinal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proyectofinal.data.Item
import com.proyectofinal.data.ItemsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

//Sealed class para representar los estados de la UI
sealed class ItemUiState {
    object Loading : ItemUiState()
    data class Success(val items: List<Item>) : ItemUiState()
    data class Error(val message: String) : ItemUiState()
    object Empty : ItemUiState()
}

//manejar los estados del viewmodel, inventory
class ItemViewModel(private val repository: ItemsRepository) : ViewModel() {

    // Estado combinado para listas notas + tareas
    private val _uiState = MutableStateFlow<ItemUiState>(ItemUiState.Loading)
    val uiState: StateFlow<ItemUiState> = _uiState.asStateFlow()

    // Estado para el ítem actual (base de datos)
    private val _currentItemState = MutableStateFlow<Item?>(null)
    val currentItemState: StateFlow<Item?> = _currentItemState.asStateFlow()

    // === NUEVOS ESTADOS PARA EDICIÓN (Formulario) ===
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _isTask = MutableStateFlow(false)
    val isTask: StateFlow<Boolean> = _isTask.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()
    // ================================================

    // Cargar todas las notas y tareas
    init {
        loadAllItems()
    }

    fun loadAllItems() {
        viewModelScope.launch {
            try {
                combine(
                    repository.getAllNotes(),
                    repository.getAllTasks()
                ) { notes, tasks ->
                    notes + tasks
                }.collect { allItems ->
                    _uiState.value = if (allItems.isEmpty()) {
                        ItemUiState.Empty
                    } else {
                        ItemUiState.Success(allItems)
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
                    // Actualizamos los estados del formulario con los datos cargados
                    updateFormState(item)
                } else {
                    _uiState.value = ItemUiState.Error("Elemento no encontrado")
                }
            } catch (e: Exception) {
                _uiState.value = ItemUiState.Error(e.message ?: "Error al cargar")
            }
        }
    }

    // Métodos para actualizar los campos desde la UI
    fun updateTitle(newTitle: String) { _title.value = newTitle }
    fun updateDescription(newDesc: String) { _description.value = newDesc }
    fun updateIsTask(isTask: Boolean) { _isTask.value = isTask }
    fun updateIsCompleted(completed: Boolean) { _isCompleted.value = completed }

    // Helper interno para sincronizar formulario
    private fun updateFormState(item: Item) {
        _title.value = item.title
        _description.value = item.description ?: ""
        _isTask.value = item.isTask
        _isCompleted.value = item.isCompleted
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
                clearCurrentItem() // Limpiar detalle
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
                    clearCurrentItem()
                }
            } catch (e: Exception) {
                _uiState.value = ItemUiState.Error("Error al eliminar: ${e.message}")
            }
        }
    }

    fun toggleTaskCompletion(task: Item) {
        viewModelScope.launch {
            try {
                if (task.isTask) {
                    val updatedTask = task.copy(isCompleted = !task.isCompleted)
                    repository.update(updatedTask) // Usa update() directamente
                }
                loadAllItems() // Refresca lista
            } catch (e: Exception) {
                _uiState.value = ItemUiState.Error("Error al actualizar tarea: ${e.message}")
            }
        }
    }

    // Limpiar estado de ítem actual y formulario
    fun clearCurrentItem() {
        _currentItemState.value = null
        _title.value = ""
        _description.value = ""
        _isTask.value = false
        _isCompleted.value = false
    }
}

// Factory
class ItemViewModelFactory(private val repository: ItemsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ItemViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}