package com.proyectofinal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext // Necesario para el contexto de los Date/Time Pickers
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.proyectofinal.data.Item
import com.proyectofinal.viewmodel.ItemUiState
import com.proyectofinal.viewmodel.ItemViewModel
import java.util.Calendar // Necesario para manejar fechas
import android.app.DatePickerDialog // Necesario para DatePicker
import android.app.TimePickerDialog // Necesario para TimePicker
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.proyectofinal.providers.MiFileProviderMultimedia
import coil.compose.AsyncImage
import android.Manifest
import android.app.Activity
import android.content.Context
import com.proyectofinal.util.AudioRecorder


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotaScreen(
    viewModel: ItemViewModel, // ViewModel inyectado
    onBack: () -> Unit, // Acción de navegación (regreso)
    isFullScreen: Boolean = true,
    itemId: Int
) {
    // ESTADO GLOBAL DEL VIEWMODEL
    val uiState by viewModel.uiState.collectAsState()
    val currentItem by viewModel.currentItemState.collectAsState()

    // === ESTADOS DE EDICIÓN DESDE EL VIEWMODEL ===
    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val isTask by viewModel.isTask.collectAsState()
    val isCompleted by viewModel.isCompleted.collectAsState()
    // ⬇️ NUEVO: Estado para la Fecha Límite desde el ViewModel ⬇️
    val dueDateTimestamp by viewModel.dueDate.collectAsState()
    // =============================================





    // Manejamos estados globales de carga/error
    when (uiState) {
        is ItemUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        is ItemUiState.Error -> {
            val msg = (uiState as ItemUiState.Error).message
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: $msg", color = Color.Red)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadAllItems() }) {
                        Text("Reintentar")
                    }
                }
            }
            return
        }
        else -> Unit
    }

    // Objeto base para saber el ID (si es 0 es nuevo)
    // ⚠️ ATENCIÓN: Se asume que Item tiene un campo Long? llamado `dueDateTimestamp`
    val itemBase = currentItem ?: Item(
        id = 0,
        title = "",
        description = null,
        isTask = false,
        isCompleted = false,
        // timestamp se llena automáticamente
        dueDateTimestamp = null,              // <-- Añadir
        photoPaths = emptyList(),             // <-- Añadir
        videoPaths = emptyList(),              // <-- Añadir
        audioPaths = emptyList()              // <-- Añadir
    )    // Función para Guardar
    val saveAction = {
        val toSave = itemBase.copy(
            title = title.trim(),
            description = description.trim(),
            isTask = isTask,
            isCompleted = isTask && isCompleted, // Solo completado si es tarea
            // ⬇️ NUEVO: Guardar el timestamp de la fecha límite ⬇️
            dueDateTimestamp = dueDateTimestamp
        )
        viewModel.saveItem(toSave)
    }

    val saveAndBack = {
        saveAction()
        if (isFullScreen) onBack()
    }

    // ⬇️ LÓGICA DEL DATE/TIME PICKER ⬇️
    val context = LocalContext.current
    val showTimePicker = remember { mutableStateOf(false) }

    val onDateSelected: (year: Int, month: Int, day: Int) -> Unit = { year, month, day ->
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, 0, 0, 0) // Inicializa la hora
        viewModel.updateDueDate(calendar.timeInMillis)
        showTimePicker.value = true // Mostrar el selector de hora después de la fecha
    }

    val onTimeSelected: (hour: Int, minute: Int) -> Unit = { hour, minute ->
        // Si ya hay una fecha seleccionada, la actualiza con la hora
        dueDateTimestamp?.let { currentTimestamp ->
            val calendar = Calendar.getInstance().apply { timeInMillis = currentTimestamp }
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            viewModel.updateDueDate(calendar.timeInMillis)
        }
    }

    val onCalendarClick = {
        if (isTask) {
            val calendar = Calendar.getInstance()
            // Usa la fecha actual o la guardada
            dueDateTimestamp?.let { calendar.timeInMillis = it }

            DatePickerDialog(
                context,
                { _, year, month, day -> onDateSelected(year, month, day) },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        // No hace nada si no es tarea, para mantener el requisito de no cambiar funcionalidad
    }



    if (showTimePicker.value) {
        val calendar = Calendar.getInstance()
        dueDateTimestamp?.let { calendar.timeInMillis = it }

        TimePickerDialog(
            context,
            { _, hour, minute ->
                onTimeSelected(hour, minute)
                showTimePicker.value = false // Cerrar el selector de hora
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true // formato 24 horas
        ).show()
    }
    // ⬆️ FIN LÓGICA DEL DATE/TIME PICKER ⬆️

    if (isFullScreen) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.volver))
                        }
                    },
                    actions = {
                        IconButton(onClick = saveAndBack) {
                            Icon(Icons.Default.Done, contentDescription = stringResource(R.string.guardar))
                        }
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            NotaDetailContent(
                viewModel= viewModel,
                padding = padding,
                title = title,
                onTitleChange = { viewModel.updateTitle(it) },
                description = description,
                onDescriptionChange = { viewModel.updateDescription(it) },
                isTask = isTask,
                onTaskChange = { viewModel.updateIsTask(it) },
                isCompleted = isCompleted,
                onCompletedChange = { viewModel.updateIsCompleted(it) },
                // ⬇️ NUEVO: Pasar el callback para el botón de calendario ⬇️
                onCalendarClick = onCalendarClick,
                onSave = saveAndBack
            )
        }
    } else {
        NotaDetailContent(
            viewModel= viewModel,
            padding = PaddingValues(0.dp),
            title = title,
            onTitleChange = { viewModel.updateTitle(it) },
            description = description,
            onDescriptionChange = { viewModel.updateDescription(it) },
            isTask = isTask,
            onTaskChange = { viewModel.updateIsTask(it) },
            isCompleted = isCompleted,
            onCompletedChange = { viewModel.updateIsCompleted(it) },
            // ⬇️ NUEVO: Pasar el callback para el botón de calendario ⬇️
            onCalendarClick = onCalendarClick,
            onSave = {
                // Guardamos sin volver (el detalle sigue visible)
                saveAction()
            }
        )
    }
}

// === COMPONENTES AUXILIARES (Sin Cambios) ===

@Composable
fun ArchivoCard(nombre: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Text(text = nombre, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun AddArchivoButton() {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.agregar_archivos))
    }
}

@Composable
fun TareaItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 3.dp,
            shadowElevation = 2.dp,
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = text,
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CircularCheckbox(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
            color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = if (checked) 2.dp else 0.dp
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.seleccionado),
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun NotaDetailContent(
    viewModel: ItemViewModel,
    padding: PaddingValues,
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    isTask: Boolean,
    onTaskChange: (Boolean) -> Unit,
    isCompleted: Boolean,
    onCompletedChange: (Boolean) -> Unit,
    // ⬇️ NUEVO: Callback para el botón de calendario ⬇️
    onCalendarClick: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val photos by viewModel.photos.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val selectedImage by viewModel.selectedImage.collectAsState()
    val showImageViewer by viewModel.showImageViewer.collectAsState()
    val photoPaths by viewModel.photoPaths.collectAsState()
    val videoPaths by viewModel.videoPaths.collectAsState()
    val audioPaths by viewModel.audioPaths.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    //Inicia,os el grabador
    val audioRecorder = remember(context) { AudioRecorder(context) }

    val photoUris = remember(photoPaths) {
        photoPaths.mapNotNull { path -> viewModel.getContentUriFromRelativePath(path) }
    }
    val videoUris = remember(videoPaths) {
        videoPaths.mapNotNull { path -> viewModel.getContentUriFromRelativePath(path) }
    }
    val audioUris = remember(audioPaths) {
        audioPaths.mapNotNull { path -> viewModel.getContentUriFromRelativePath(path) }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        viewModel.onPictureTaken(success)
    }


    val recordVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success ->
        viewModel.onVideoRecorded(success)
    }
    val pendingAction = remember { mutableStateOf<(() -> Unit)?>(null) }

    val cameraAndAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true

        if (cameraGranted && audioGranted) {
            pendingAction.value?.invoke()
            pendingAction.value = null
        } else {
            val denied = if (!cameraGranted && !audioGranted) "Cámara y Audio" else if (!cameraGranted) "Cámara" else "Audio"
            Toast.makeText(context, "Permisos de $denied denegados.", Toast.LENGTH_LONG).show()
            pendingAction.value = null
        }
    }

    val checkCameraAndAudioPermissionsAndLaunch: (action: () -> Unit) -> Unit = { action ->
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        pendingAction.value = action

        if (hasCameraPermission && hasAudioPermission) {
            action()
        } else {
            cameraAndAudioPermissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }
    if (selectedImage != null) {
        val selectedUri = selectedImage!!

        // Determinar si la URI seleccionada es un video o una imagen (por su extensión o por dónde se almacena)
        // La forma más fácil de diferenciar aquí es si la URI está en la lista de videos.


        if (showImageViewer) { // Es una foto, usa el visor de Compose
            ImageViewer(
                imageUri = selectedUri,
                onClose = { viewModel.closeImageViewer() }
            )
        }
    }



    Column(
        modifier = Modifier
            .padding(padding)
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.Start
    ) {
        // **Caja de Texto: Título**
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = {
                Text(
                    text = stringResource(R.string.placeholder_title),
                    color = LocalContentColor.current.copy(alpha = 0.7f)
                )
            },
            textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // **Caja de Texto: Descripción**
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = {
                Text(
                    text = stringResource(R.string.placeholder_description),
                    color = LocalContentColor.current.copy(alpha = 0.7f)
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            maxLines = 10
        )

        Spacer(modifier = Modifier.height(16.dp))


        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.archivos),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        var showMediaMenu by remember { mutableStateOf(false) }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(photoPaths.size) { index ->
                val path = photoPaths[index]
                val uri = photoUris.getOrNull(index)
                if (uri != null) {
                    FotoCard(
                        uri = uri,
                        onClick = { viewModel.openImage(uri) },
                        onDelete = { deletedUri -> viewModel.deleteMediaByUri(deletedUri) } // Pasa el callback
                    )
                }
            }
            items(videoPaths.size) { index ->
                val path = videoPaths[index]
                val uri = videoUris.getOrNull(index)
                if (uri != null) {
                    VideoCard(
                        uri = uri,
                        onClick = { playVideo(context, uri, viewModel) },
                        onDelete = { deletedUri -> viewModel.deleteMediaByUri(deletedUri) }
                    )
                }
            }
            items (audioPaths.size){ index ->
                val uri = audioUris.getOrNull(index)
                if (uri != null) {
                    AudioCard(
                        uri = uri,
                        onClick = { playAudio(context, uri) },
                        onDelete = { deletedUri -> viewModel.deleteMediaByUri(deletedUri) }
                    )
                }

            }

            item {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        // Ripple moderno de Material3 (sin deprecaciones)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(
                                bounded = false,           // ripple circular que sale del centro
                                radius = 38.dp             // tamaño perfecto para tu botón de 76.dp
                            ),
                            onClick = { showMediaMenu = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Añadir multimedia",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )

                    DropdownMenu(
                        expanded = showMediaMenu,
                        onDismissRequest = { showMediaMenu = false },
                        modifier = Modifier
                            .width(240.dp)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { MenuItemText(Icons.Default.PhotoLibrary, "Galería") },
                            onClick = { showMediaMenu = false }
                        )
                        DropdownMenuItem(
                            text = { MenuItemText(Icons.Default.PhotoCamera, "Tomar foto") },
                            onClick = { showMediaMenu = false
                                checkCameraAndAudioPermissionsAndLaunch { // Pasa la acción de lanzamiento
                                    val uri = viewModel.createImageUri()
                                    takePictureLauncher.launch(uri)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { MenuItemText(Icons.Default.Videocam, "Grabar vídeo") },
                            onClick = { showMediaMenu = false
                                checkCameraAndAudioPermissionsAndLaunch { // Pasa la acción de lanzamiento
                                    val uri = viewModel.createVideoUri()
                                    recordVideoLauncher.launch(uri)
                                }}
                        )
                        DropdownMenuItem(
                            text = { MenuItemText(Icons.Default.Mic, "Grabar audio") },
                            onClick = { showMediaMenu = false
                                if (isRecording) {
                                    try {
                                        audioRecorder.stop()
                                        viewModel.stopAudioRecording(true)
                                    }catch (e: Exception){
                                        Log.e("Audio", "Error al detener: ${e.message}")
                                        viewModel.stopAudioRecording(false) // Falló al detener
                                        Toast.makeText(context, "Error al detener la grabación.", Toast.LENGTH_LONG).show()
                                    }
                                }else{
                                    checkCameraAndAudioPermissionsAndLaunch { // Requiere RECORD_AUDIO
                                        try {
                                            // 1. Creamos la estructura de archivo (Usando el nombre correcto: getAudioUri)
                                            val mediaFile =
                                                com.proyectofinal.providers.MiFileProviderMultimedia.getAudioUri(
                                                    context
                                                )

                                            // 2. Obtenemos la ruta ABSOLUTA para MediaRecorder
                                            // NOTA: context.filesDir.resolve(relativePath) construye la ruta absoluta
                                            val absolutePath =
                                                context.filesDir.resolve(mediaFile.relativePath).absolutePath

                                            // 3. Iniciar la grabación con la ruta absoluta
                                            audioRecorder.start(absolutePath)

                                            // 4. Actualizar el ViewModel
                                            viewModel.startAudioRecording(mediaFile.relativePath)

                                        } catch (e: Exception) {
                                            Log.e("Audio", "Error al iniciar: ${e.message}")
                                            viewModel.stopAudioRecording(false) // Resetear estado si falla
                                            Toast.makeText(
                                                context,
                                                "Error al iniciar la grabación. ¿Micrófono en uso?",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { MenuItemText(Icons.Default.AttachFile, "Archivo") },
                            onClick = { showMediaMenu = false }
                        )
                    }
                }
            }
        }


        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox: Es Tarea
            CircularCheckbox(
                text = stringResource(R.string.tarea),
                checked = isTask,
                onCheckedChange = onTaskChange
            )

            // Checkbox: Tarea Completa (solo si es tarea)
            if (isTask) {
                CircularCheckbox(
                    text = stringResource(R.string.completado),
                    checked = isCompleted,
                    onCheckedChange = onCompletedChange
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            TareaItem(
                icon = Icons.Default.DateRange,
                text = stringResource(R.string.calendario),
                // ⬇️ MODIFICADO: Llamada al nuevo callback ⬇️
                onClick = onCalendarClick
            )
        }

        // Botón de guardar (solo visible si no hay TopBar, e.g. tablet mode o landscape sin scaffold topbar)
        if (!padding.calculateTopPadding().value.isNaN() && padding.calculateTopPadding() == 0.dp) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSave,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Done, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.guardar))
            }
        }
    }
    if (showImageViewer && selectedImage != null) {
        ImageViewer(
            imageUri = selectedImage!!,
            onClose = { viewModel.closeImageViewer() }
        )
    }
}

@Composable
private fun MenuItemText(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
@Composable
fun VideoCard(
    uri: Uri,
    onClick: () -> Unit,
    onDelete: (Uri) -> Unit // NUEVO: Callback para eliminar
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box( // Contenedor para el video y el botón de eliminar
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            // Usamos AsyncImage para obtener el thumbnail del video si es posible
            AsyncImage(
                model = uri,
                contentDescription = "Video grabado",
                modifier = Modifier
                    .fillMaxSize() // La imagen rellena el Box
                    .clickable { onClick() }
            )
            // Ícono de reproducción sobre el thumbnail
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Reproducir Video",
                tint = Color.White,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(4.dp)
                    .zIndex(1f) // Asegura que esté sobre la imagen, pero debajo de la 'X'
            )

            // Botón de eliminar ('X')
            IconButton(
                onClick = { onDelete(uri) }, // Llama a onDelete con la URI
                modifier = Modifier
                    .align(Alignment.TopStart) // Posición: Superior izquierda
                    .size(24.dp) // Tamaño del botón (ajusta si es necesario)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape) // Fondo semitransparente
                    .zIndex(2f) // Asegura que esté por encima del thumbnail y del botón de Play
                    .padding(2.dp) // Pequeño padding interno para el icono
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Eliminar video",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp) // Tamaño del icono
                )
            }
        }
        Text(
            text = "Video",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
@Composable
fun FotoCard(
    uri: Uri,
    onClick: () -> Unit,
    onDelete: (Uri) -> Unit // NUEVO: Callback para eliminar
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box( // Contenedor para la imagen y el botón de eliminar
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "Foto tomada",
                modifier = Modifier
                    .fillMaxSize() // La imagen rellena el Box
                    .clickable { onClick() }
            )

            // Botón de eliminar (la 'X')
            IconButton(
                onClick = { onDelete(uri) }, // Llama a onDelete con la URI
                modifier = Modifier
                    .align(Alignment.TopStart) // Posición: Superior izquierda
                    .size(24.dp) // Tamaño del botón (ajusta si es necesario)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape) // Fondo semitransparente
                    .zIndex(2f) // Asegura que esté por encima de la imagen
                    .padding(2.dp) // Pequeño padding interno para el icono
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Eliminar foto",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp) // Tamaño del icono
                )
            }
        }
        Text(
            text = "Foto",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun AudioCard(
    uri: Uri,
    onClick: () -> Unit,
    onDelete: (Uri) -> Unit // NUEVO: Callback para eliminar
){
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable{ onClick() },
            contentAlignment = Alignment.Center
        ){
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Reproducir Audio",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = { onDelete(uri) }, // Llama a onDelete con la URI
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .zIndex(2f)
                    .padding(2.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Eliminar audio", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Text(
            text = "Audio",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
@Composable
fun ImageViewer(
    imageUri: Uri,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {

        // Imagen grande
        AsyncImage(
            model = imageUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        // CERRAR
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .zIndex(10f)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar",
                tint = Color.White,
                modifier = Modifier.size(42.dp))
        }


    }
}

fun playVideo(context: Context, uri: Uri, viewModel: ItemViewModel) {
    // Crear el Intent para ver el contenido (reproducción)
    val playIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/*")
        // Permiso esencial para que la app externa pueda leer el archivo seguro
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    // Intentar lanzar la actividad
    try {
        context.startActivity(playIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se encontró un reproductor de video.", Toast.LENGTH_SHORT).show()
        // No es necesario loguear aquí si ya lo haces en el ViewModel/otra parte
    }

    // Limpiar cualquier estado de visor que pudiera haberse activado por error
    viewModel.closeImageViewer()
}

fun playAudio(context: Context, uri: Uri ) {
        val playIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "audio/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try{
            context.startActivity(playIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se encontró un reproductor de audio.", Toast.LENGTH_SHORT).show()
        }
}





