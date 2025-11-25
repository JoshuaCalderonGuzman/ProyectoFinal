package com.proyectofinal.viewmodel

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proyectofinal.data.Item
import com.proyectofinal.data.ItemsRepository
import com.proyectofinal.util.NotificationReceiver // Importar el Receptor
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

// Cambiado de ViewModel a AndroidViewModel para obtener el Context
class ItemViewModel(
    private val repository: ItemsRepository,
    application: Application // Agregado para acceder al Context
) : AndroidViewModel(application) {

    // Estado combinado para listas notas + tareas
    private val _uiState = MutableStateFlow<ItemUiState>(ItemUiState.Loading)
    val uiState: StateFlow<ItemUiState> = _uiState.asStateFlow()

    // Estado para el ítem actual (base de datos)
    private val _currentItemState = MutableStateFlow<Item?>(null)
    val currentItemState: StateFlow<Item?> = _currentItemState.asStateFlow()

    // === ESTADOS PARA EDICIÓN (Formulario) ===
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _isTask = MutableStateFlow(false)
    val isTask: StateFlow<Boolean> = _isTask.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    // NUEVO ESTADO: Fecha Límite (timestamp en milisegundos)
    private val _dueDate = MutableStateFlow<Long?>(null)
    val dueDate: StateFlow<Long?> = _dueDate.asStateFlow()
    // ================================================


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
    fun updateDueDate(newDueDate: Long?) { _dueDate.value = newDueDate } // Nuevo

    fun startNewItemCreation() {
        val newItem = Item(
            id = 0,
            title = "",
            description = null,
            isTask = false,
            isCompleted = false,
            dueDateTimestamp = null
        )
        _currentItemState.value = newItem
        updateFormState(newItem)
    }

    // Helper interno para sincronizar formulario
    private fun updateFormState(item: Item) {
        _title.value = item.title
        _description.value = item.description ?: ""
        _isTask.value = item.isTask
        _isCompleted.value = item.isCompleted
        _dueDate.value = item.dueDateTimestamp // Sincronizar
    }

    // === LÓGICA DE NOTIFICACIONES ===

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleNotification(item: Item) {
        val context = getApplication<Application>().applicationContext

        // Si la fecha es inválida/nula, no es tarea, está completada o la fecha ya pasó, cancela la alarma.
        if (item.dueDateTimestamp == null || !item.isTask || item.isCompleted || item.dueDateTimestamp <= System.currentTimeMillis()) {
            cancelNotification(item.id, context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            // Pasar los datos de la tarea
            putExtra(NotificationReceiver.NOTIFICATION_ID_KEY, item.id)
            putExtra(NotificationReceiver.NOTIFICATION_TITLE_KEY, item.title)
            putExtra(NotificationReceiver.NOTIFICATION_DESC_KEY, item.description ?: "")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id, // <--- Este es el Request Code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Agendar la alarma en el tiempo exacto
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            item.dueDateTimestamp,
            pendingIntent
        )
    }

    private fun cancelNotification(itemId: Int, context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            itemId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
        }
    }
    // ===================================


    fun saveItem(item: Item) {
        viewModelScope.launch {
            try {
                // Copia el item con los valores actuales del StateFlow del ViewModel
                val itemToSave = item.copy(
                    title = _title.value.trim(),
                    description = _description.value.trim(),
                    isTask = _isTask.value,
                    isCompleted = _isCompleted.value,
                    dueDateTimestamp = _dueDate.value // Incluir el nuevo campo
                )

                if (itemToSave.id == 0) {
                    // newId ahora es de tipo Long, por lo que .toInt() es válido.
                    val newId = repository.insert(itemToSave)
                    scheduleNotification(itemToSave.copy(id = newId.toInt()))
                } else {
                    repository.update(itemToSave)
                    scheduleNotification(itemToSave) // Actualiza o reagenda
                }

                loadAllItems()
                clearCurrentItem()
            } catch (e: Exception) {
                _uiState.value = ItemUiState.Error("Error al guardar: ${e.message}")
            }
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            try {
                repository.delete(item)
                cancelNotification(item.id, getApplication<Application>().applicationContext) // Cancelar la alarma
                loadAllItems()
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
                    repository.update(updatedTask)

                    // Si se marca como completada, se cancela; si se desmarca, se reagenda.
                    if (updatedTask.isCompleted) {
                        cancelNotification(updatedTask.id, getApplication<Application>().applicationContext)
                    } else {
                        scheduleNotification(updatedTask.copy(dueDateTimestamp = _dueDate.value))
                    }
                }
                loadAllItems()
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
        _dueDate.value = null // Limpiar
    }
}


// Factory (NOTA: DEBE SER ACTUALIZADO EN TU CÓDIGO)
class ItemViewModelFactory(
    private val repository: ItemsRepository,
    private val application: Application // Necesario para AndroidViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ItemViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}