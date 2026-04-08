package com.doey.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.agent.SettingsStore
import kotlinx.coroutines.launch

/**
 * MacrosScreen — Doey 23.4.9 Ultra (Tau Version)
 *
 * Permite crear, editar y ejecutar macros (comandos rápidos predefinidos).
 * Un macro es un atajo de texto que se expande a un comando completo.
 * Ejemplo: "cena" → "busca en YouTube recetas de pasta carbonara"
 */
data class DoeyMacro(
    val id: String = java.util.UUID.randomUUID().toString(),
    val trigger: String,
    val command: String,
    val icon: String = "⚡",
    val category: String = "General"
)

@Composable
fun MacrosScreen(vm: MainViewModel) {
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()

    var macros       by remember { mutableStateOf(getDefaultMacros()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editMacro    by remember { mutableStateOf<DoeyMacro?>(null) }
    var searchQuery  by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TauBg)
    ) {
        // ── Header ──────────────────────────────────────────────────────────────
        Surface(
            color = TauSurface1,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Macros",
                            color = TauText1,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${macros.size} comandos rápidos",
                            color = TauText3,
                            fontSize = 13.sp
                        )
                    }
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = TauAccent,
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, "Añadir macro", modifier = Modifier.size(22.dp))
                    }
                }
                // Búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar macro...", color = TauText3, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TauText3) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = doeyFieldColors(),
                    singleLine = true
                )
            }
        }

        // ── Lista de macros ──────────────────────────────────────────────────────
        val filtered = macros.filter {
            searchQuery.isBlank() ||
            it.trigger.contains(searchQuery, ignoreCase = true) ||
            it.command.contains(searchQuery, ignoreCase = true)
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Star,
                        null,
                        tint = TauText3,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (searchQuery.isBlank()) "No hay macros aún" else "Sin resultados",
                        color = TauText3,
                        fontSize = 16.sp
                    )
                    if (searchQuery.isBlank()) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { showAddDialog = true }) {
                            Text("Crear tu primer macro", color = TauAccent)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { macro ->
                    MacroCard(
                        macro = macro,
                        onExecute = { vm.sendMessage(macro.command) },
                        onEdit    = { editMacro = macro },
                        onDelete  = { macros = macros.filter { m -> m.id != macro.id } }
                    )
                }
            }
        }
    }

    // ── Diálogo: Añadir/Editar macro ────────────────────────────────────────────
    if (showAddDialog || editMacro != null) {
        MacroDialog(
            initial = editMacro,
            onDismiss = { showAddDialog = false; editMacro = null },
            onSave = { newMacro ->
                macros = if (editMacro != null) {
                    macros.map { if (it.id == newMacro.id) newMacro else it }
                } else {
                    macros + newMacro
                }
                showAddDialog = false
                editMacro = null
            }
        )
    }
}

@Composable
private fun MacroCard(
    macro: DoeyMacro,
    onExecute: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = TauSurface1,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TauAccent.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(macro.icon, fontSize = 18.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            macro.trigger,
                            color = TauText1,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            macro.category,
                            color = TauAccentLight,
                            fontSize = 11.sp
                        )
                    }
                }
                Row {
                    IconButton(onClick = onExecute, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.PlayArrow, "Ejecutar", tint = TauGreen, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, "Editar", tint = TauText2, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, "Eliminar", tint = TauRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = TauSurface3)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Comando:",
                        color = TauText3,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TauSurface2
                    ) {
                        Text(
                            macro.command,
                            color = TauText2,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroDialog(
    initial: DoeyMacro?,
    onDismiss: () -> Unit,
    onSave: (DoeyMacro) -> Unit
) {
    var trigger  by remember { mutableStateOf(initial?.trigger ?: "") }
    var command  by remember { mutableStateOf(initial?.command ?: "") }
    var icon     by remember { mutableStateOf(initial?.icon ?: "⚡") }
    var category by remember { mutableStateOf(initial?.category ?: "General") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TauSurface1,
        title = {
            Text(
                if (initial == null) "Nuevo Macro" else "Editar Macro",
                color = TauText1,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    label = { Text("Nombre / Disparador") },
                    placeholder = { Text("ej: cena, música trabajo...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = doeyFieldColors(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("Comando completo") },
                    placeholder = { Text("ej: pon música relajante en Spotify") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors = doeyFieldColors(),
                    maxLines = 4
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = icon,
                        onValueChange = { icon = it.take(2) },
                        label = { Text("Icono") },
                        modifier = Modifier.width(80.dp),
                        colors = doeyFieldColors(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Categoría") },
                        modifier = Modifier.weight(1f),
                        colors = doeyFieldColors(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (trigger.isNotBlank() && command.isNotBlank()) {
                        onSave(
                            DoeyMacro(
                                id       = initial?.id ?: java.util.UUID.randomUUID().toString(),
                                trigger  = trigger.trim(),
                                command  = command.trim(),
                                icon     = icon.ifBlank { "⚡" },
                                category = category.ifBlank { "General" }
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TauAccent)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TauText2)
            }
        }
    )
}

private fun getDefaultMacros() = listOf(
    DoeyMacro(
        trigger  = "Buenos días",
        command  = "Buenos días! Dime el clima de hoy y recuérdame mis tareas pendientes",
        icon     = "🌅",
        category = "Rutinas"
    ),
    DoeyMacro(
        trigger  = "Modo trabajo",
        command  = "Pon música de concentración en Spotify y activa el modo No Molestar",
        icon     = "💼",
        category = "Productividad"
    ),
    DoeyMacro(
        trigger  = "Cena",
        command  = "Busca en YouTube un tutorial de recetas fáciles para cenar hoy",
        icon     = "🍽️",
        category = "Hogar"
    ),
    DoeyMacro(
        trigger  = "Ir a casa",
        command  = "Abre Google Maps y navega a casa",
        icon     = "🏠",
        category = "Navegación"
    )
)
