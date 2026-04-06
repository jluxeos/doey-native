package com.doey.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.agent.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowModeAdvancedScreen(vm: MainViewModel) {
    val scope = rememberCoroutineScope()
    var currentPath by remember { mutableStateOf(listOf<String>()) }
    var currentActions by remember { mutableStateOf(listOf<FlowAction>()) }
    var searchQuery by remember { mutableStateOf("") }
    var executionResult by remember { mutableStateOf("") }
    var showMacroDialog by remember { mutableStateOf(false) }
    var macroName by remember { mutableStateOf("") }

    // Cargar categorías al iniciar
    LaunchedEffect(Unit) {
        scope.launch {
            currentActions = FlowModeAdvancedEngine.getRootCategories().mapIndexed { idx, cat ->
                FlowAction(
                    id = "cat_$idx",
                    name = cat,
                    category = "Categoría",
                    type = FlowActionType.AUTOMATION,
                    icon = cat.first().toString()
                )
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Surface0Light)
    ) {
        // ── Barra superior ────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Column {
                    Text("Modo Flujo Pro", color = Label1Light, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Automatización Offline • ${currentActions.size} acciones", color = Label3Light, fontSize = 10.sp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1Light),
            actions = {
                IconButton(onClick = {
                    currentPath = emptyList()
                    searchQuery = ""
                    scope.launch {
                        currentActions = FlowModeAdvancedEngine.getRootCategories().mapIndexed { idx, cat ->
                            FlowAction(
                                id = "cat_$idx",
                                name = cat,
                                category = "Categoría",
                                type = FlowActionType.AUTOMATION,
                                icon = cat.first().toString()
                            )
                        }
                    }
                }) {
                    Icon(Icons.Default.Home, "Inicio", tint = Label3Light)
                }
                IconButton(onClick = { showMacroDialog = true }) {
                    Icon(Icons.Default.Save, "Guardar Macro", tint = Label3Light)
                }
            }
        )

        // ── Buscador global ───────────────────────────────────────────────────
        Surface(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            color = Surface2Light,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = Label3Light, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    textStyle = TextStyle(color = Label1Light, fontSize = 14.sp),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text("Buscar acción...", color = Label3Light, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Label3Light, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ── Breadcrumbs (Migas de pan) ────────────────────────────────────────
        if (currentPath.isNotEmpty()) {
            Surface(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    currentPath.forEachIndexed { index, step ->
                        Text(
                            step,
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(Purple, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        if (index < currentPath.size - 1) {
                            Icon(Icons.Default.ChevronRight, null, tint = Label3Light, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {
                        if (currentPath.isNotEmpty()) {
                            currentPath = currentPath.dropLast(1)
                        }
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Undo, null, tint = Purple, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // ── Lista de acciones filtradas ────────────────────────────────────────
        val filteredActions = if (searchQuery.isEmpty()) {
            currentActions
        } else {
            currentActions.filter { action ->
                action.name.contains(searchQuery, ignoreCase = true) ||
                action.category.contains(searchQuery, ignoreCase = true)
            }
        }

        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredActions) { action ->
                FlowActionCard(
                    action = action,
                    onClick = {
                        scope.launch {
                            if (action.subActions.isNotEmpty()) {
                                currentPath = currentPath + action.name
                                currentActions = action.subActions
                            } else {
                                // Ejecutar acción
                                executionResult = "✓ ${action.name} ejecutada"
                            }
                        }
                    }
                )
            }
        }

        // ── Resultado de ejecución ────────────────────────────────────────────
        AnimatedVisibility(
            visible = executionResult.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        executionResult,
                        fontSize = 12.sp,
                        color = Color(0xFF1B5E20),
                        modifier = Modifier.weight(1f)
                    )
                    LaunchedEffect(executionResult) {
                        kotlinx.coroutines.delay(3000)
                        executionResult = ""
                    }
                }
            }
        }
    }

    // ── Diálogo para guardar macro ─────────────────────────────────────────────
    if (showMacroDialog) {
        AlertDialog(
            onDismissRequest = { showMacroDialog = false },
            title = { Text("Guardar como Macro") },
            text = {
                Column {
                    Text("Nombre del macro:", fontSize = 12.sp, color = Label3Light)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = macroName,
                        onValueChange = { macroName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ej: Mi flujo favorito") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (macroName.isNotEmpty()) {
                        val macro = FlowMacro(
                            id = System.currentTimeMillis().toString(),
                            name = macroName,
                            description = currentPath.joinToString(" → "),
                            actions = currentActions
                        )
                        FlowModeAdvancedEngine.saveMacro(macro)
                        macroName = ""
                        showMacroDialog = false
                        executionResult = "✓ Macro '${macro.name}' guardado"
                    }
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMacroDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun FlowActionCard(
    action: FlowAction,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Surface1Light),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8DEF8))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    action.icon,
                    fontSize = 24.sp,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        action.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Label1Light
                    )
                    if (action.category.isNotEmpty() && action.category != "Categoría") {
                        Text(
                            action.category,
                            fontSize = 10.sp,
                            color = Label3Light
                        )
                    }
                }
            }
            if (action.subActions.isNotEmpty()) {
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = Purple,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
