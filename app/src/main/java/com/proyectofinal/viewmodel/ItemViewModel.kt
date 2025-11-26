package com.proyectofinal.viewmodel

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
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
import java.io.File

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
    // ===============================================

    //Multimedia
    private val _photos = MutableStateFlow<List<Uri>>(emptyList())
    val photos: StateFlow<List<Uri>> = _photos.asStateFlow()

    private val _tempPhotoUri = MutableStateFlow<Uri?>(null)
    val tempPhotoUri: StateFlow<Uri?> = _tempPhotoUri.asStateFlow()

    // Variables para manejar videos
    private val _videos = MutableStateFlow<List<Uri>>(emptyList())
    val videos: StateFlow<List<Uri>> = _videos.asStateFlow()

    private val _tempVideoUri = MutableStateFlow<Uri?>(null)
    val tempVideoUri: StateFlow<Uri?> = _tempVideoUri.asStateFlow()

    // Variables para el visor de multimedia
    private val _selectedImage = MutableStateFlow<Uri?>(null)
    val selectedImage: StateFlow<Uri?> = _selectedImage.asStateFlow()

    private val _showImageViewer = MutableStateFlow(false)
    val showImageViewer: StateFlow<Boolean> = _showImageViewer.asStateFlow()

    private val _photoPaths = MutableStateFlow<List<String>>(emptyList())
    val photoPaths: StateFlow<List<String>> = _photoPaths.asStateFlow()

    private val _tempPhotoPath = MutableStateFlow<String?>(null) // Guardará la ruta temporal
    val tempPhotoPath: StateFlow<String?> = _tempPhotoPath.asStateFlow()
    // ...
    private val _videoPaths = MutableStateFlow<List<String>>(emptyList())
    val videoPaths: StateFlow<List<String>> = _videoPaths.asStateFlow()

    private val _tempVideoPath = MutableStateFlow<String?>(null) // Guardará la ruta temporal
    val tempVideoPath: StateFlow<String?> = _tempVideoPath.asStateFlow()

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
            dueDateTimestamp = null,
            photoPaths = emptyList(),
            videoPaths = emptyList()
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

        // === CARGAR MULTIMEDIA ===
        _photoPaths.value = item.photoPaths
        _videoPaths.value = item.videoPaths

        //Limpia Temporal
        _tempPhotoPath.value = null
        _tempPhotoUri.value = null
        _tempVideoPath.value = null
        _tempVideoUri.value = null
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
                    dueDateTimestamp = _dueDate.value, // Incluir el nuevo campo
                    photoPaths = _photoPaths.value,
                    videoPaths = _videoPaths.value
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
                deleteMediaFiles(item)
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
        _photos.value = emptyList()
        _videos.value = emptyList()
        _photoPaths.value = emptyList()
        _videoPaths.value = emptyList()
    }

    /** Genera una URI segura para tomar foto */
    fun createImageUri(): Uri {
        val context = getApplication<Application>().applicationContext
        val mediaFile = com.proyectofinal.providers.MiFileProviderMultimedia.getImageUri(context)

        // Guardamos la Uri para el launcher y la ruta para la persistencia
        _tempPhotoUri.value = mediaFile.contentUri
        _tempPhotoPath.value = mediaFile.relativePath // <--- GUARDAMOS LA RUTA RELATIVA
        return mediaFile.contentUri
    }

    /** Una vez tomada la foto desde UI */
    fun onPictureTaken(success: Boolean) {
        val path = _tempPhotoPath.value ?: return
        val uri = _tempPhotoUri.value ?: return
        if (success) {
            _photoPaths.value = _photoPaths.value + path
        }
        _tempPhotoPath.value = null // Limpiar temporal
        _tempPhotoUri.value = null
    }

    fun openImageByPath(relativePath: String) {
        val uri = getContentUriFromRelativePath(relativePath)
        _selectedImage.value = uri
        _showImageViewer.value = uri != null
    }

    //Abrir visor
    fun openImage(uri: Uri) {
        _selectedImage.value = uri
        _showImageViewer.value = true
    }

    //Cerrar visor
    fun closeImageViewer() {
        _showImageViewer.value = false
        _selectedImage.value = null
    }

    // Eliminar foto
    fun deleteSelectedImage() {
        val img = _selectedImage.value ?: return
        _photos.value = _photos.value.filter { it != img }
        _showImageViewer.value = false
    }

    fun createVideoUri(): Uri {
        val context = getApplication<Application>().applicationContext
        val mediaFile = com.proyectofinal.providers.MiFileProviderMultimedia.getVideoUri(context)        // Guardamos la Uri para el launcher y la ruta para la persistencia        _tempPhotoUri.value = mediaFile.contentUri
        _tempVideoUri.value = mediaFile.contentUri
        _tempVideoPath.value = mediaFile.relativePath // <--- GUARDAMOS LA RUTA RELATIVA
        return mediaFile.contentUri
    }

    /** Una vez grabado el video desde UI */
    fun onVideoRecorded(success: Boolean) {
        val path = _tempVideoPath.value ?: return
        val uri = _tempVideoUri.value ?: return
        if (success) {
            _videoPaths.value = _videoPaths.value + path
        }
        // Limpiar la URI temporal después de intentar la grabación
        _tempVideoUri.value = null
        _tempVideoPath.value = null
    }

    fun deleteSelectedMedia() {
        val uriToDelete = _selectedImage.value ?: return

        val relativePath = getRelativePathFromContentUri(uriToDelete) ?: return

        deleteMediaByPath(relativePath)

        _selectedImage.value = null
        _showImageViewer.value = false

    }
    fun deleteMediaByUri(uri: Uri) {
        //Convertir la URI de contenido a la ruta relativa
        val relativePath = getRelativePathFromContentUri(uri) ?: return
        //Elimina el archivo fisico
        //deleteSingleMediaFileByPath(relativePath)
        //Eliminar de la lista de persistencia
        deleteMediaByPath(relativePath)

        //Si la URI eliminada era la que estaba en el visor, limpiar la selección y cerrar el visor
        if (_selectedImage.value == uri) {
            _selectedImage.value = null
            _showImageViewer.value = false
        }

    }

    private fun deleteMediaByPath(relativePath: String) {
        if (_photoPaths.value.contains(relativePath)) {
            _photoPaths.value = _photoPaths.value.filter { it != relativePath }
        }else if (_videoPaths.value.contains(relativePath)) {
            _videoPaths.value = _videoPaths.value.filter { it != relativePath }
        }
    }
    fun getContentUriFromRelativePath(relativePath: String): Uri? {
        val context = getApplication<Application>().applicationContext

        // Reconstruye el archivo File
        val baseDir = context.filesDir
        val fileToRetrieve = File(baseDir, relativePath)

        if (!fileToRetrieve.exists()) return null

        val auth = context.packageName + ".fileprovidermultimedia"
        return FileProvider.getUriForFile(
            context,
            auth,
            fileToRetrieve
        )
    }

    private fun getRelativePathFromContentUri(contentUri: Uri): String? {
        val allPaths = _photoPaths.value + _videoPaths.value

        for (path in allPaths) {
            if (getContentUriFromRelativePath(path) == contentUri) {
                return path
            }
        }
        return null
    }
    private fun deleteMediaFiles(item: Item) {
        val context = getApplication<Application>().applicationContext
        val baseDir = context.filesDir

        // Combina todas las rutas a eliminar
        val allPaths = item.photoPaths + item.videoPaths

        allPaths.forEach { relativePath ->
            val fileToDelete = File(baseDir, relativePath)
            if (fileToDelete.exists()) {
                try {
                    fileToDelete.delete()
                    Log.d("ItemViewModel", "Archivo eliminado: $relativePath")
                } catch (e: Exception) {
                    Log.e("ItemViewModel", "Error al eliminar archivo $relativePath: ${e.message}")
                }
            }
        }
    }
    /*private fun deleteSingleMediaFileByPath(relativePath: String) {
        val context = getApplication<Application>().applicationContext
        val baseDir = context.filesDir

        val fileToDelete = File(baseDir, relativePath)
        if (fileToDelete.exists()) {
            try {
                fileToDelete.delete()
                Log.d("ItemViewModel", "Archivo multimedia físico eliminado: $relativePath")
            } catch (e: Exception) {
                Log.e("ItemViewModel", "Error al eliminar archivo físico $relativePath: ${e.message}")
            }
        }
    }*/


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