package com.doey.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.agent.FlowMacro
import com.doey.agent.FlowModeAdvancedEngine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacrosManagerScreen() {
    val scope = rememberCoroutineScope()
    var macros by remember { mutableStateOf(listOf<FlowMacro>()) }
    var executionResult by remember { mutableStateOf("") }
    var selectedMacroId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        macros = FlowModeAdvancedEngine.getAllMacros()
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
                    Text("Macros Guardados", color = Label1Light, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${macros.size} macros disponibles", color = Label3Light, fontSize = 10.sp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1Light),
            actions = {
                IconButton(onClick = {
                    scope.launch {
                        macros = FlowModeAdvancedEngine.getAllMacros()
                    }
                }) {
                    Icon(Icons.Default.Refresh, "Actualizar", tint = Label3Light)
                }
            }
        )

        // ── Lista de macros ────────────────────────────────────────────────────
        if (macros.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        null,
                        tint = Label3Light,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No hay macros guardados",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Label1Light,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Crea un macro desde el Modo Flujo para guardarlo aquí",
                        fontSize = 12.sp,
                        color = Label3Light,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(macros) { macro ->
                    MacroCard(
                        macro = macro,
                        isSelected = selectedMacroId == macro.id,
                        onSelect = { selectedMacroId = macro.id },
                        onExecute = {
                            executionResult = "✓ Macro '${macro.name}' ejecutado"
                            scope.launch {
                                kotlinx.coroutines.delay(3000)
                                executionResult = ""
                            }
                        },
                        onDelete = {
                            FlowModeAdvancedEngine.deleteMacro(macro.id)
                            scope.launch {
                                macros = FlowModeAdvancedEngine.getAllMacros()
                                selectedMacroId = null
                            }
                        }
                    )
                }
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
                }
            }
        }
    }
}

@Composable
private fun MacroCard(
    macro: FlowMacro,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onExecute: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val createdDate = dateFormat.format(Date(macro.createdAt))

    Surface(
        Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) Color(0xFFEADDFF) else Surface1Light,
                RoundedCornerShape(12.dp)
            ),
        color = if (isSelected) Color(0xFFEADDFF) else Surface1Light,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Purple else Color(0xFFE8DEF8)
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        macro.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Label1Light
                    )
                    Text(
                        macro.description,
                        fontSize = 11.sp,
                        color = Label3Light,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Creado: $createdDate",
                        fontSize = 9.sp,
                        color = Label3Light
                    )
                }
                Icon(
                    Icons.Default.Star,
                    null,
                    tint = if (isSelected) Purple else Color.Transparent,
                    modifier = Modifier.size(24.dp)
                )
            }

            // ── Acciones ───────────────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExecute,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ejecutar", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onSelect,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Editar", fontSize = 12.sp)
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Delete, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
