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
    val itemBase = currentItem ?: Item(id = 0, title = "", description = "", isTask = false, isCompleted = false /*, dueDateTimestamp = null*/)

    // Función para Guardar
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

        Text(
            text = stringResource(R.string.archivos),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(2) {
                ArchivoCard(stringResource(R.string.anotacion))
            }
            item {
                AddArchivoButton()
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

            TareaItem(
                icon = Icons.Default.Call,
                text = stringResource(R.string.audio),
                onClick = { /* Acción para Audio */ }
            )

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
}