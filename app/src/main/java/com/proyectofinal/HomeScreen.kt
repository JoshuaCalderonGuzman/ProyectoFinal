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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
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
            floatingActionButton = { FloatingAddButton() },
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
                    text = "HOME",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))
                SearchBar()

                Spacer(Modifier.height(16.dp))
                SectionHeader(title = "Tareas", icon = Icons.Default.Refresh)

                ItemRow(title = "Tarea 1")
                ItemRow(title = "Tarea 2")
                ItemRow(title = "Tarea n")

                Spacer(Modifier.height(16.dp))
                SectionHeader(title = "Notas")

                ItemRow(title = "Nota 1")
                ItemRow(title = "Nota 2")
                ItemRow(title = "Nota 3")
                ItemRow(title = "Nota n")
            }
        }
    }
}

@Composable
fun SearchBar() {
    val textState = remember { mutableStateOf("") }
    OutlinedTextField(
        value = textState.value,
        onValueChange = { textState.value = it },
        placeholder = { Text("Buscar") },
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
                contentDescription = null,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { /* Acción */ }
            )
        }
    }
}

@Composable
fun ItemRow(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .background(Color.Gray, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(4.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White)

        Row {
            IconButton(
                onClick = { /* Eliminar */ },
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = { /* Editar */ },
                modifier = Modifier
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Editar",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun FloatingAddButton() {
    FloatingActionButton(
        onClick = { /* Agregar nueva nota/tarea */ },
        shape = CircleShape,
        containerColor = Color.White,
        contentColor = Color.Black,
        modifier = Modifier.size(60.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = "Agregar", tint = Color.Black)
    }
}