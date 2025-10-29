package com.proyectofinal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.proyectofinal.R
import com.proyectofinal.data.Item
import com.proyectofinal.viewmodel.ItemUiState
import com.proyectofinal.viewmodel.ItemViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ItemViewModel,
    onNoteClick: (Int) -> Unit, // Navegación a la pantalla de edición
    onAddNewClick: () -> Unit   // Navegación a la pantalla de creación
) {
    val uiState by viewModel.uiState.collectAsState()

    // Estado para rastrear el ID del ítem actualmente expandido (null si ninguno)
    var expandedItemId by rememberSaveable { mutableStateOf<Int?>(null) }

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
            floatingActionButton = { FloatingAddButton(onAddNewClick) },
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

                when (uiState) {
                    is ItemUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                    is ItemUiState.Empty -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.nohay),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 16.sp
                            )
                        }
                    }
                    is ItemUiState.Success -> {
                        val allItems = (uiState as ItemUiState.Success).items
                        val tasks = allItems.filter { it.isTask }
                        val notes = allItems.filter { !it.isTask }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (tasks.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = stringResource(R.string.tareas),
                                        icon = Icons.Default.Refresh
                                    )
                                }
                                items(tasks) { task ->
                                    val isExpanded = expandedItemId == task.id
                                    ExpandableItem(
                                        item = task,
                                        isExpanded = isExpanded,
                                        onExpand = {
                                            // Cambia el estado: si ya estaba expandido, lo colapsa (null), si no, lo expande
                                            expandedItemId = if (isExpanded) null else task.id
                                        },
                                        onDelete = { viewModel.deleteItem(task) },
                                        onEdit = { onNoteClick(task.id) },
                                        onToggleComplete = { viewModel.toggleTaskCompletion(task) }
                                    )
                                }
                            }

                            if (notes.isNotEmpty()) {
                                item {
                                    SectionHeader(title = stringResource(R.string.notas))
                                }
                                items(notes) { note ->
                                    val isExpanded = expandedItemId == note.id
                                    ExpandableItem(
                                        item = note,
                                        isExpanded = isExpanded,
                                        onExpand = {
                                            expandedItemId = if (isExpanded) null else note.id
                                        },
                                        onDelete = { viewModel.deleteItem(note) },
                                        onEdit = { onNoteClick(note.id) },
                                        onToggleComplete = { /* No action for notes */ }
                                    )
                                }
                            }
                        }
                    }
                    is ItemUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                }
            }
        }
    }
}

//Envuelve ItemRow y la vista de detalle
@Composable
fun ExpandableItem(
    item: Item,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onToggleComplete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ItemRow(
            item = item,
            onExpand = onExpand, // Ahora ItemRow toma la acción de expansión
            onDelete = onDelete,
            onEdit = onEdit,
            onToggleComplete = onToggleComplete
        )
        // Muestra u oculta el contenido con animación
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            AdaptiveDetailView(item = item)
        }
    }
}

//La vista que adapta su diseño
@Composable
fun AdaptiveDetailView(item: Item) {
    //BoxWithConstraints nos permite conocer el tamaño disponible para el composable
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth(0.9f) // Usa el mismo ancho que ItemRow
            .padding(top = 0.dp)
            .background(
                // Un color de fondo más sutil
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            )
            .padding(16.dp)
    ) {
        //Se define un punto de corte. 600dp es común para diferenciar teléfonos de tablets.
        val isLargeScreen = this.maxWidth > 600.dp

        if (isLargeScreen) {
            //DISEÑO ADAPTABLE: PANTALLA GRANDE/HORIZONTAL (Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                //Título como encabezado, ocupando un poco menos de la mitad
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(0.3f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                //Contenido, ocupando más de la mitad.
                Text(
                    text = item.description ?: "", // CORRECCIÓN: Manejo de String?
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.weight(0.7f)
                )
            }
        } else {
            // DISEÑO ADAPTABLE: PANTALLA PEQUEÑA/VERTICAL (Column)
            Column(modifier = Modifier.fillMaxWidth()) {
                // Título
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Contenido
                Text(
                    text = item.description ?: "", // CORRECCIÓN: Manejo de String?
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ==========================================================
// COMPONENTE EXISTENTE MODIFICADO: ItemRow
// ==========================================================
@Composable
fun ItemRow(
    item: Item,
    onExpand: () -> Unit, // NUEVO: Acción para expandir/colapsar
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onToggleComplete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .background(
                if (item.isTask && item.isCompleted) Color(0xFF388E3C) else Color.Gray,
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onExpand) // El clic principal es ahora para expandir
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Si es una tarea, se muestra el Checkbox para completar.
        if (item.isTask) {
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color.White,
                    uncheckedColor = Color.White.copy(alpha = 0.7f),
                    checkmarkColor = Color.Gray
                ),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = item.title,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        Row {
            IconButton(
                onClick = onDelete,
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
            // Botón de editar/navegar se mantiene igual
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(28.dp)
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


// === COMPONENTES AUXILIARES SIN CAMBIOS ===

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

@Composable
fun FloatingAddButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = Color.White,
        contentColor = Color.Black,
        modifier = Modifier.size(60.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.agregar), tint = Color.Black)
    }
}