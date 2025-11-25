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
import com.proyectofinal.data.Item
import com.proyectofinal.ui.utils.ContentType
import com.proyectofinal.ui.utils.NavigationType
import com.proyectofinal.viewmodel.ItemUiState
import com.proyectofinal.viewmodel.ItemViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ItemViewModel,
    onNoteClick: (Int) -> Unit,   // Navega a la pantalla de edición
    onAddNewClick: () -> Unit,     // Navega a la pantalla de creación (id = 0)
    contentType: ContentType,
    navigationType: NavigationType
) {
    // Estado global del ViewModel (Loading / Empty / Success / Error)

    val uiState by viewModel.uiState.collectAsState()
    val currentItem by viewModel.currentItemState.collectAsState()
    //Estado local: id del ítem que está expandido
    var expandedItemId by rememberSaveable { mutableStateOf<Int?>(null) }

    val onAddAction: () -> Unit = {
        if (contentType == ContentType.LIST_AND_DETAIL) {
            // Caso Tablet: Inicia la creación en el panel de detalle
            viewModel.startNewItemCreation() //
        } else {
            // Caso Móvil: Navega a la pantalla completa de creación
            onAddNewClick()
        }
    }

    val listHeaderColor = if (contentType == ContentType.LIST_AND_DETAIL) {
        Color.Black // Modo Tablet: Fondo del 40% es blanco, Texto debe ser NEGRO.
    } else {
        Color.White // Modo Móvil: Fondo es oscuro (del darkColorScheme), Texto debe ser BLANCO.
    }

    MaterialTheme(
        colorScheme = darkColorScheme(),
        typography = Typography(
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
        ),
    ) {
        if (contentType == ContentType.LIST_AND_DETAIL) {
            // TABLET: Lista + Detalle
            Row(modifier = Modifier.fillMaxSize()) {
                // Lista (40%)
                Box(modifier = Modifier.weight(0.4f).fillMaxHeight().background(Color.White)){
                    HomeListContent(
                        uiState = uiState,
                        expandedItemId = expandedItemId,
                        onExpand = { expandedItemId = if (expandedItemId == it) null else it },
                        onNoteClick = onNoteClick,
                        onAddNewClick = onAddAction,
                        onDelete = { viewModel.deleteItem(it) },
                        onToggleComplete = { viewModel.toggleTaskCompletion(it) },
                        headerColor = listHeaderColor,
                        modifier = Modifier.fillMaxSize()

                    )
                    // Botón Flotante colocado DENTRO del Box (40%)
                    FloatingAddButton(
                        onClick = onAddAction,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)

                    )

                }


                // Detalle (60%)
                currentItem?.let { item ->
                    NotaDetailContent(
                        item = item,
                        onSave = { updatedItem ->
                            viewModel.saveItem(updatedItem)
                        },
                        onDelete = {
                            viewModel.deleteItem(item)
                            viewModel.clearCurrentItem()
                        },
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                    )
                } ?: Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Selecciona un elemento", color = Color.White.copy(alpha = 0.6f))
                }
            }
        }else{
            Scaffold(
                floatingActionButton = { FloatingAddButton(onAddAction) },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                HomeListContent(
                    uiState = uiState,
                    expandedItemId = expandedItemId,
                    onExpand = { expandedItemId = if (expandedItemId == it) null else it },
                    onNoteClick = onNoteClick,
                    onAddNewClick = onAddAction,
                    onDelete = { viewModel.deleteItem(it) },
                    onToggleComplete = { viewModel.toggleTaskCompletion(it) },
                    headerColor = listHeaderColor,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }


    }
}


/**
 * Envuelve la fila del ítem y su detalle animado.
 */
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
            onExpand = onExpand,
            onDelete = onDelete,
            onEdit = onEdit,
            onToggleComplete = onToggleComplete
        )
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            AdaptiveDetailView(item = item)
        }
    }
}

/**
 * Vista de detalle que se adapta al ancho disponible.
 */
@Composable
fun AdaptiveDetailView(item: Item) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            )
            .padding(16.dp)
    ) {
        val isLargeScreen = maxWidth > 600.dp

        if (isLargeScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(0.3f)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = item.description ?: "",
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.weight(0.7f)
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.description ?: "",
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Fila que muestra el título, checkbox (si es tarea) y botones.
 */
@Composable
fun ItemRow(
    item: Item,
    onExpand: () -> Unit,
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
            .clickable(onClick = onExpand)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox solo para tareas
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
            Spacer(Modifier.width(8.dp))
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

//COMPONENTES AUXILIARES

@Composable
fun SearchBar() {
    val textState = androidx.compose.runtime.remember { mutableStateOf("") }
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
fun SectionHeader(title: String, icon: ImageVector? = null, headerColor: Color) {

    Row(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(title, style = MaterialTheme.typography.titleMedium, color = headerColor)
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.refrescar),
                modifier = Modifier
                    .size(22.dp)
                    .clickable { },
                tint = headerColor
            )
        }
    }
}

@Composable
fun FloatingAddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = Color.White,
        contentColor = Color.Black,
        modifier = modifier.size(60.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.agregar), tint = Color.Black)
    }
}

@Composable
private fun HomeListContent(
    uiState: ItemUiState,
    expandedItemId: Int?,
    onExpand: (Int) -> Unit,
    onNoteClick: (Int) -> Unit,
    onAddNewClick: () -> Unit,
    onDelete: (Item) -> Unit,
    onToggleComplete: (Item) -> Unit,
    modifier: Modifier = Modifier,
    headerColor: Color
) {
    Column(modifier = modifier.padding(12.dp)) {
        Text(
            text = stringResource(R.string.titulo),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
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
                    Text(stringResource(R.string.nohay), color = Color.White.copy(alpha = 0.7f))
                }
            }
            is ItemUiState.Success -> {
                val tasks = uiState.items.filter { it.isTask }
                val notes = uiState.items.filter { !it.isTask }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (tasks.isNotEmpty()) {
                        item { SectionHeader(title = stringResource(R.string.tareas), icon = Icons.Default.Refresh,headerColor = headerColor) }
                        items(tasks) { task ->
                            ExpandableItem(
                                item = task,
                                isExpanded = expandedItemId == task.id,
                                onExpand = { onExpand(task.id) },
                                onDelete = { onDelete(task) },
                                onEdit = { onNoteClick(task.id) },
                                onToggleComplete = { onToggleComplete(task) }
                            )
                        }
                    }
                    if (notes.isNotEmpty()) {
                        item { SectionHeader(title = stringResource(R.string.notas),headerColor = headerColor) }
                        items(notes) { note ->
                            ExpandableItem(
                                item = note,
                                isExpanded = expandedItemId == note.id,
                                onExpand = { onExpand(note.id) },
                                onDelete = { onDelete(note) },
                                onEdit = { onNoteClick(note.id) },
                                onToggleComplete = { }
                            )
                        }
                    }
                }
            }
            is ItemUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${uiState.message}", color = Color.Red)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { }) { Text("Reintentar") }
                    }
                }
            }
        }
    }
}

@Composable
fun NotaDetailContent(
    item: Item,
    onSave: (Item) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(item.title) }
    var description by remember { mutableStateOf(item.description ?: "") }
    var isTask by remember { mutableStateOf(item.isTask) }
    var isCompleted by remember { mutableStateOf(item.isCompleted) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.placeholder_title)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.placeholder_description)) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            maxLines = 10
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isTask, onCheckedChange = { isTask = it })
            Text(stringResource(R.string.tarea),
                        color = Color.White)
            Spacer(Modifier.width(16.dp))
            if (isTask) {
                Checkbox(checked = isCompleted, onCheckedChange = { isCompleted = it })
                Text(stringResource(R.string.completado),
                            color = Color.White)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text(stringResource(R.string.eliminar), color = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val updated = item.copy(
                    title = title,
                    description = description,
                    isTask = isTask,
                    isCompleted = isCompleted && isTask
                )
                onSave(updated)
            }) {
                Text(stringResource(R.string.guardar))
            }
        }
    }
}