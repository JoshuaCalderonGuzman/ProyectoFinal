package com.proyectofinal.viewmodel

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateOf
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private val _reminders = MutableStateFlow<List<Long>>(emptyList())
    val reminders: StateFlow<List<Long>> = _reminders.asStateFlow()
    private val _dueDate = MutableStateFlow<Long?>(null)
    val dueDate: StateFlow<Long?> = _dueDate.asStateFlow()
    // ===============================================

    //Multimedia

    // Variables para manejar Foto
    private val _photos = MutableStateFlow<List<Uri>>(emptyList())
    val photos: StateFlow<List<Uri>> = _photos.asStateFlow()
    private val _tempPhotoUri = MutableStateFlow<Uri?>(null)

    // Variables para manejar videos
    private val _videos = MutableStateFlow<List<Uri>>(emptyList())
    val videos: StateFlow<List<Uri>> = _videos.asStateFlow()

    private val _tempVideoUri = MutableStateFlow<Uri?>(null)

    // Variables para el visor de multimedia
    private val _selectedImage = MutableStateFlow<Uri?>(null)
    val selectedImage: StateFlow<Uri?> = _selectedImage.asStateFlow()

    private val _showImageViewer = MutableStateFlow(false)
    val showImageViewer: StateFlow<Boolean> = _showImageViewer.asStateFlow()

    private val _photoPaths = MutableStateFlow<List<String>>(emptyList())
    val photoPaths: StateFlow<List<String>> = _photoPaths.asStateFlow()

    private val _tempPhotoPath = MutableStateFlow<String?>(null) // Guardará la ruta temporal

    private val _videoPaths = MutableStateFlow<List<String>>(emptyList())
    val videoPaths: StateFlow<List<String>> = _videoPaths.asStateFlow()
    private val _tempVideoPath = MutableStateFlow<String?>(null) // Guardará la ruta temporal

    //Audio
    private val _audioPaths = MutableStateFlow<List<String>>(emptyList())
    val audioPaths: StateFlow<List<String>> = _audioPaths.asStateFlow()

    private val _tempAudioUri = MutableStateFlow<Uri?>(null)
    val tempAudioUri: StateFlow<Uri?> = _tempAudioUri.asStateFlow()

    private val _tempAudioPath = MutableStateFlow<String?>(null)
    val tempAudioPath: StateFlow<String?> = _tempAudioPath.asStateFlow()
    private val _isRecording = MutableStateFlow(false)

    private val _filePaths = MutableStateFlow<List<String>>(emptyList())
    val filePaths: StateFlow<List<String>> = _filePaths.asStateFlow()

    private val _tempFileUri = MutableStateFlow<Uri?>(null)
    val tempFileUri: StateFlow<Uri?> = _tempFileUri.asStateFlow()
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    var searchQuery = mutableStateOf("")
        private set


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
    fun loadItemById(id: Int) = loadItem(id)


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
            videoPaths = emptyList(),
            audioPaths = emptyList(),
            filePaths = emptyList(),
            reminderTimestamps = _reminders.value
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
        _reminders.value = item.reminderTimestamps.sorted() // Sincronizar y ordenar
        _dueDate.value = item.dueDateTimestamp // Sincronizar

        // === CARGAR MULTIMEDIA ===
        _photoPaths.value = item.photoPaths
        _videoPaths.value = item.videoPaths
        _audioPaths.value = item.audioPaths
        _filePaths.value = item.filePaths

        //Limpia Temporal
        _tempPhotoPath.value = null
        _tempPhotoUri.value = null
        _tempVideoPath.value = null
        _tempVideoUri.value = null
        _tempAudioPath.value = null
        _tempAudioUri.value = null
    }

    // === LÓGICA DE NOTIFICACIONES ===

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleNotification(item: Item) {

        val context = getApplication<Application>().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // -------------------------------
        // 🔍 LOG 1: Inicio
        // -------------------------------
        Log.d("NOTIFICATION_DEBUG", "----------------------------------------")
        Log.d("NOTIFICATION_DEBUG", "INICIANDO scheduleNotification() para itemId=${item.id}")
        Log.d("NOTIFICATION_DEBUG", "Recordatorios totales: ${item.reminderTimestamps.size}")
        Log.d("NOTIFICATION_DEBUG", "----------------------------------------")

        // Cancelar alarmas previas
        item.reminderTimestamps.forEach { timestamp ->
            val pendingId = item.id + timestamp.hashCode()
            Log.d("NOTIFICATION_DEBUG", "Cancelando alarma previa -> pendingId=$pendingId, fecha=${Date(timestamp)}")
            cancelNotification(pendingId)
        }

        item.reminderTimestamps.forEach { timestamp ->

            // -------------------------------
            // 🔍 LOG 2: validar timestamp
            // -------------------------------
            Log.d("NOTIFICATION_DEBUG", "Procesando timestamp=$timestamp (${Date(timestamp)})")

            if (timestamp <= System.currentTimeMillis()) {
                Log.d("NOTIFICATION_DEBUG", "⏭️ Saltado: El recordatorio es del pasado.")
                return@forEach
            }

            val pendingId = item.id + timestamp.hashCode()
            Log.d("NOTIFICATION_DEBUG", "⚙️ Generando PendingIntent -> pendingId=$pendingId")

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra(NotificationReceiver.NOTIFICATION_ID_KEY, item.id)
                putExtra(NotificationReceiver.NOTIFICATION_TITLE_KEY, item.title)
                putExtra(NotificationReceiver.NOTIFICATION_DESC_KEY, item.description ?: "")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                pendingId,   // ID único por recordatorio
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // -------------------------------
            // 🔍 LOG 3: programando alarma
            // -------------------------------
            Log.d(
                "NOTIFICATION_DEBUG",
                "⏰ PROGRAMANDO alarma: " +
                        "itemId=${item.id}, " +
                        "pendingId=$pendingId, " +
                        "fecha=${Date(timestamp)}, " +
                        "horaExacta=${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))}"
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timestamp,
                pendingIntent
            )
        }

        Log.d("NOTIFICATION_DEBUG", "✔ scheduleNotification() FINALIZADO para itemId=${item.id}")
        Log.d("NOTIFICATION_DEBUG", "----------------------------------------")
    }


    // ===================================
    @SuppressLint("UnspecifiedImmutableFlag")
    fun cancelNotification(itemId: Int) { //  AHORA ES PÚBLICA Y NO NECESITA CONTEXTO EXTERNO
        val context = getApplication<Application>().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            // Usa el ID del item como ID de la notificación para cancelarla
            putExtra(NotificationReceiver.NOTIFICATION_ID_KEY, itemId)
        }

        // El PendingIntent debe ser IDÉNTICO al que se usó para establecer la alarma (mismo requestCode: itemId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            itemId, // CRUCIAL: Usar el itemId como requestCode
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d("ItemViewModel", "Recordatorio para Item ID $itemId cancelado.")
    }

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
                    videoPaths = _videoPaths.value,
                    audioPaths = _audioPaths.value,
                    filePaths = _filePaths.value,
                    reminderTimestamps = _reminders.value
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
                cancelNotification(item.id) // ANTES: cancelNotification(item.id, getApplication().applicationContext)
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
                        cancelNotification(updatedTask.id) // ANTES: cancelNotification(updatedTask.id, getApplication().applicationContext)                    } else {
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

        // Reset campos básicos
        _title.value = ""
        _description.value = ""
        _isTask.value = false
        _isCompleted.value = false
        _dueDate.value = null

        // Reset multimedia
        _photos.value = emptyList()
        _videos.value = emptyList()
        _audioPaths.value = emptyList()
        _filePaths.value = emptyList()
        _photoPaths.value = emptyList()
        _videoPaths.value = emptyList()

        _reminders.value = emptyList()

        // limpiar temporales
        _tempPhotoUri.value = null
        _tempVideoUri.value = null
        _tempAudioUri.value = null
        _tempFileUri.value = null
        _tempPhotoPath.value = null
        _tempVideoPath.value = null
        _tempAudioPath.value = null
    }
    //FOTO
    // Genera una URI segura para tomar foto
    fun createImageUri(): Uri {
        val context = getApplication<Application>().applicationContext
        val mediaFile = com.proyectofinal.providers.MiFileProviderMultimedia.getImageUri(context)

        // Guardamos la Uri para el launcher y la ruta para la persistencia
        _tempPhotoUri.value = mediaFile.contentUri
        _tempPhotoPath.value = mediaFile.relativePath // <--- GUARDAMOS LA RUTA RELATIVA
        return mediaFile.contentUri
    }

    // Una vez tomada la foto desde UI
    fun onPictureTaken(success: Boolean) {
        val path = _tempPhotoPath.value ?: return
        val uri = _tempPhotoUri.value ?: return
        if (success) {
            _photoPaths.value = _photoPaths.value + path
        }
        _tempPhotoPath.value = null // Limpiar temporal
        _tempPhotoUri.value = null
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


    //VIDEO
    fun createVideoUri(): Uri {
        val context = getApplication<Application>().applicationContext
        val mediaFile = com.proyectofinal.providers.MiFileProviderMultimedia.getVideoUri(context)        // Guardamos la Uri para el launcher y la ruta para la persistencia        _tempPhotoUri.value = mediaFile.contentUri
        _tempVideoUri.value = mediaFile.contentUri
        _tempVideoPath.value = mediaFile.relativePath // <--- GUARDAMOS LA RUTA RELATIVA
        return mediaFile.contentUri
    }

    /// Una vez grabado el video desde UI
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
    //AUDIO
    fun startAudioRecording(relativePath: String) {
        _tempAudioPath.value = relativePath // Guarda la ruta temporal
        _isRecording.value = true
    }

    fun stopAudioRecording(success: Boolean) {
        if (success) {
            val path = _tempAudioPath.value ?: return
            // El archivo ya fue guardado en disco por MediaRecorder, solo actualizamos el path
            _audioPaths.value = _audioPaths.value + path
        }
        // Limpiar estados
        _isRecording.value = false
        _tempAudioPath.value = null
        _tempAudioUri.value = null
    }
    //ARCHIVOS
    fun onFileSelected(uri: Uri?) {
        if (uri == null) return

        val context = getApplication<Application>().applicationContext

        try{
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)        }catch (e: SecurityException){
            Log.e("ItemViewModel", "Error al tomar permiso de persistencia: ${e.message}")
            return
        }
        //Solo guardamos el URI de Contenido como String
        val uriString = uri.toString()
        //Guardar el String URI en la lista
        _filePaths.value = _filePaths.value + uriString
        //Limpiar temporal
        _tempFileUri.value = null

    }
    //NOTIFICACION

    fun updateItem(item: Item, newDueDateTimestamp: Long? = null) = viewModelScope.launch {

        // 1. Crear una copia del Item con el nuevo timestamp (null para eliminar)
        val itemToUpdate = item.copy(dueDateTimestamp = newDueDateTimestamp)

        // 2. Guardar el Item actualizado en la base de datos (IMPORTANTE para la UI)
        repository.update(itemToUpdate) // Esto debe setear dueDateTimestamp a NULL en Room
        updateFormState(itemToUpdate)
        // 3. Lógica de Notificación
        if (newDueDateTimestamp == null) {
            // Si el timestamp es null, CANCELAR la alarma
            cancelNotification(item.id)
        } else {
            // Si el timestamp tiene un valor, (re)programar la alarma
            scheduleNotification(itemToUpdate)
        }
    }



    //DELETS
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
        }else if(_videoPaths.value.contains(relativePath)) {
            _videoPaths.value = _videoPaths.value.filter { it != relativePath }
        }else if (_audioPaths.value.contains(relativePath)) {
            _audioPaths.value = _audioPaths.value.filter { it != relativePath }
        }else{
            deleteFileByPath(relativePath)
        }
    }
    fun getContentUriFromRelativePath(relativePath: String): Uri? {
        val context = getApplication<Application>().applicationContext

        try {
            val uri = Uri.parse(relativePath)
            if (uri.scheme == "content" || uri.scheme == "file") {
                return uri
            }
        }catch (e: Exception){
            Log.e("ItemViewModel", "Error al parsear URI: ${e.message}")
        }

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
        if (_filePaths.value.contains(contentUri.toString())) {
            return contentUri.toString() // Devolvemos el URI como String/Path
        }

        val allPaths = _photoPaths.value + _videoPaths.value + _audioPaths.value

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
        val allPaths = item.photoPaths + item.videoPaths + item.audioPaths

        item.filePaths.forEach { uriString ->
            try {
                val uri = Uri.parse(uriString)
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.releasePersistableUriPermission(uri, flags)
                Log.d("ItemViewModel", "Permiso persistente liberado para: $uriString")
            } catch (e: Exception) {
                Log.e("ItemViewModel", "Error al liberar permiso para URI: ${e.message}")
            }
        }

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

    private fun deleteFileByPath(uriString: String) {
        //Si la ruta es un URI de archivo, lo eliminamos de la lista
        if (_filePaths.value.contains(uriString)){
            val context = getApplication<Application>().applicationContext
            try{
                val uri = Uri.parse(uriString)
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.releasePersistableUriPermission(uri, flags)
            }catch (e: Exception){
                Log.e("ItemViewModel", "Error al liberar permiso de persistencia: ${e.message}")
            }

            _filePaths.value = _filePaths.value.filter { it != uriString }
        }
    }
    fun addReminder(timestamp: Long) {
        // 1. Agregar a la lista
        _reminders.value = _reminders.value + timestamp

        // 2. Si el item YA existe en BD → programar de inmediato
        val item = _currentItemState.value
        if (item != null && item.id > 0) {
            val updated = item.copy(reminderTimestamps = _reminders.value)
            scheduleNotification(updated)
        }

    }

    fun removeReminder(timestamp: Long) {
        _reminders.value = _reminders.value.filter { it != timestamp }
    }

    fun setSearchQuery(value: String) {
        searchQuery.value = value
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