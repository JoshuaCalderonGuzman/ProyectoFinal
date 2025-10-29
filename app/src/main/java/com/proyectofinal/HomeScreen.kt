package com.proyectofinal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyectofinal.R  // ← IMPORTANTE: Para stringResource
import com.proyectofinal.data.Item
import com.proyectofinal.viewmodel.ItemUiState
import com.proyectofinal.viewmodel.ItemViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ItemViewModel,
    onNoteClick: (Int) -> Unit, // Parámetro de navegación (Editar)
    onAddNewClick: () -> Unit   // Parámetro de navegación (Crear)
) {
    // UN SOLO ESTADO: uiState del ViewModel (reemplaza allNotes y allTasks)
    val uiState by viewModel.uiState.collectAsState()

    MaterialTheme(
        colorScheme = darkColorScheme(),
        typography = Typography(
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
        ),
        shapes = Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(12.dp)
        )
    ) {
        Scaffold(
            floatingActionButton = { FloatingAddButton(onAddNewClick) }, // Acción de navegación
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.titulo),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))
                SearchBar() // Barra de búsqueda (aún no funcional)

                Spacer(Modifier.height(16.dp))

                // === MANEJO CENTRALIZADO DEL ESTADO DE LA UI ===
                when (uiState) {
                    is ItemUiState.Loading -> {
                        // Pantalla de carga
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }

                    is ItemUiState.Empty -> {
                        // No hay elementos
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.nohay),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 16.sp
                            )
                        }
                    }

                    is ItemUiState.Success -> {
                        // Lista combinada (tareas + notas)
                        val allItems = (uiState as ItemUiState.Success).items
                        val tasks = allItems.filter { it.isTask }
                        val notes = allItems.filter { !it.isTask }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // === SECCIÓN TAREAS ===
                            if (tasks.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = stringResource(R.string.tareas),
                                        icon = Icons.Default.Refresh
                                    )
                                }
                                items(tasks) { task ->
                                    ItemRow(
                                        item = task,
                                        onDelete = { viewModel.deleteItem(task) },
                                        onEdit = { onNoteClick(task.id) },
                                        onToggleComplete = { viewModel.toggleTaskCompletion(task) }
                                    )
                                }
                            }

                            // === SECCIÓN NOTAS ===
                            if (notes.isNotEmpty()) {
                                item {
                                    SectionHeader(title = stringResource(R.string.notas))
                                }
                                items(notes) { note ->
                                    ItemRow(
                                        item = note,
                                        onDelete = { viewModel.deleteItem(note) },
                                        onEdit = { onNoteClick(note.id) },
                                        onToggleComplete = { /* No-op para notas */ }
                                    )
                                }
                            }
                        }
                    }

                    is ItemUiState.Error -> {
                        // Error con opción de reintento
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Error: ${(uiState as ItemUiState.Error).message}",
                                    color = Color.Red,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { viewModel.loadAllItems() }) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }

                    is ItemUiState.CurrentItemLoaded -> {
                        // Este estado se maneja en la pantalla de detalle/edición
                        // Aquí lo ignoramos (no se muestra en Home)
                    }
                }
            }
        }
    }
}

// === COMPONENTES AUXILIARES (sin cambios en funcionalidad) ===

@Composable
fun SearchBar() {
    // Estado local para el texto de búsqueda (aún no filtra)
    val textState = remember { mutableStateOf("") }
    OutlinedTextField(
        value = textState.value,
        onValueChange = { textState.value = it },
        placeholder = { Text(stringResource(R.string.buscar)) },
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(50.dp),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun SectionHeader(title: String, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.refrescar),
                modifier = Modifier
                    .size(22.dp)
                    .clickable { /* Acción de refrescar (pendiente) */ },
                tint = Color.White
            )
        }
    }
}

// Componente ItemRow actualizado para recibir el objeto Item y acciones
@Composable
fun ItemRow(
    item: Item,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onToggleComplete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            // Cambiar color basado en la finalización de la tarea
            .background(
                if (item.isTask && item.isCompleted) Color(0xFF388E3C) else Color.Gray,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = item.isTask, onClick = onToggleComplete), // Solo tareas se completan
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Muestra el título
        Text(
            text = item.title,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        Row {
            // Botón de eliminar
            IconButton(
                onClick = onDelete, // Acción de eliminar
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.eliminar),
                    tint = Color.White
                )
            }

            // Botón de editar
            IconButton(
                onClick = onEdit, // Acción de editar/navegar
                modifier = Modifier
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.editar),
                    tint = Color.White
                )
            }
        }
    }
}

// Componente FloatingAddButton actualizado para recibir y ejecutar la acción
@Composable
fun FloatingAddButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick, // Acción de navegación
        shape = CircleShape,
        containerColor = Color.White,
        contentColor = Color.Black,
        modifier = Modifier.size(60.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.agregar), tint = Color.Black)
    }
}