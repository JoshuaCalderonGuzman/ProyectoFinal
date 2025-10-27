package com.proyectofinal

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import com.proyectofinal.data.Item // Asegúrate de importar Item
import com.proyectofinal.viewmodel.ItemViewModel // Asegúrate de importar ItemViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotaScreen(
    itemId: Int, // ID del item (0 para nuevo)
    viewModel: ItemViewModel, // ViewModel inyectado
    onBack: () -> Unit // Acción de navegación (regreso)
) {
    // El ítem actual cargado por el ViewModel (puede ser null si es nuevo)
    val initialItem = viewModel.currentItem

    // 1. ESTADOS EDITABLES
    // Usar valores del item existente o predeterminados si es nuevo
    var title by remember { mutableStateOf(initialItem?.title ?: "Titulo de la nota") }
    var description by remember { mutableStateOf(initialItem?.description ?: "Escriba aquí la descripción...") }
    var isTask by remember { mutableStateOf(initialItem?.isTask ?: false) }
    var isCompleted by remember { mutableStateOf(initialItem?.isCompleted ?: false) }

    // 2. LÓGICA DE GUARDADO
    val onSaveAction: () -> Unit = {
        val itemToSave = Item(
            id = itemId, // 0 para nuevo, ID existente para editar
            title = title,
            // Guardar solo si el texto no es el placeholder (o solo el texto si es diferente)
            description = description.takeIf { it != "Escriba aquí la descripción..." } ?: "",
            isTask = isTask,
            isCompleted = isCompleted
        )
        viewModel.saveItem(itemToSave) // Ejecuta la lógica de Insert/Update
        onBack() // Regresa a la pantalla anterior
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) { // Acción regresar (usa onBack)
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onSaveAction) { // Acción guardar (usa onSaveAction)
                        Icon(Icons.Default.Done, contentDescription = "Guardar")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
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
                onValueChange = { title = it },
                label = { Text("Titulo de la nota") },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // **Caja de Texto: Descripción**
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false) // Permite múltiples líneas
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Archivos",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(2) {
                    ArchivoCard("Anotacion")
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
                    text = "Tarea",
                    checked = isTask,
                    onCheckedChange = { isTask = it } // Toggles Task mode
                )

                // Checkbox: Tarea Completa (SOLO visible si es Tarea)
                if (isTask) {
                    CircularCheckbox(
                        text = "Completa",
                        checked = isCompleted,
                        onCheckedChange = { isCompleted = it } // Toggles completion status
                    )
                }

                TareaItem(
                    icon = Icons.Default.Call,
                    text = "Audio",
                    onClick = { /* Acción para Audio */ }
                )

                TareaItem(
                    icon = Icons.Default.DateRange,
                    text = "Calendario",
                    onClick = { /* Acción para calendario */ }
                )
            }
        }
    }
}

// ... El resto de los Composable helpers (ArchivoCard, AddArchivoButton, TareaItem, CircularCheckbox)

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
        Icon(Icons.Outlined.Add, contentDescription = "Agregar archivo")
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
                    contentDescription = "Seleccionado",
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