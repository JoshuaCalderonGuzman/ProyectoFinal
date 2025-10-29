package com.proyectofinal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
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
import com.proyectofinal.viewmodel.ItemViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ItemViewModel,
    onNoteClick: (Int) -> Unit, //Parámetro de navegación (Editar)
    onAddNewClick: () -> Unit //Parámetro de navegación (Crear)
) {
    //Recolectar datos en tiempo real de la base de datos a través del ViewModel
    val notes by viewModel.allNotes.collectAsState(initial = emptyList())
    val tasks by viewModel.allTasks.collectAsState(initial = emptyList())

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
            floatingActionButton = { FloatingAddButton(onAddNewClick) }, //Usar la acción de navegación
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
                SearchBar()

                Spacer(Modifier.height(16.dp))
                SectionHeader(title = stringResource(R.string.tareas), icon = Icons.Default.Refresh)

                //LISTA DE TAREAS DINÁMICA
                tasks.forEach { task ->
                    ItemRow(
                        item = task,
                        onDelete = { viewModel.deleteItem(task) },
                        onEdit = { onNoteClick(task.id) },
                        onToggleComplete = { viewModel.toggleTaskCompletion(task) }
                    )
                }

                Spacer(Modifier.height(16.dp))
                SectionHeader(title = stringResource(R.string.notas))

                // 2. LISTA DE NOTAS DINÁMICA
                notes.forEach { note ->
                    ItemRow(
                        item = note,
                        onDelete = { viewModel.deleteItem(note) }, // Acción de eliminar
                        onEdit = { onNoteClick(note.id) }, // Acción de editar
                        onToggleComplete = { /* No-op para notas */ }
                    )
                }
            }
        }
    }
}

// ... SearchBar y SectionHeader (sin cambios)

@Composable
fun SearchBar() {
    val textState = remember { mutableStateOf("") }
    OutlinedTextField(
        value = textState.value,
        onValueChange = { textState.value = it },
        placeholder = { Text(stringResource(R.string.buscar)) },
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(50.dp),
        singleLine = true
    )
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.refrescar),
                modifier = Modifier
                    .size(22.dp)
                    .clickable { /* Acción de refrescar */ }
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
            .clickable(onClick = onToggleComplete), // Clic para completar/descompletar
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Muestra el título
        Text(item.title, color = Color.White)

        Row {
            // Botón de eliminar
            IconButton(
                onClick = onDelete, // Usar la acción de eliminar
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
                onClick = onEdit, // Usar la acción de editar/navegar
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