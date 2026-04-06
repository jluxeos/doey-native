package com.doey.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
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
import com.doey.agent.FlowModeEngine
import com.doey.agent.FlowNode
import com.doey.agent.FlowOption
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowModeScreen(vm: MainViewModel) {
    val scope = rememberCoroutineScope()
    var currentNode by remember { mutableStateOf<FlowNode?>(null) }
    var commandPath by remember { mutableStateOf(listOf<String>()) }
    var selectedParams by remember { mutableStateOf(mapOf<String, String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var executionResult by remember { mutableStateOf("") }

    // Cargar nodos raíz al iniciar
    LaunchedEffect(Unit) {
        currentNode = FlowNode(
            id = "root",
            label = "Modo Flujo",
            description = "Selecciona una acción",
            options = FlowModeEngine.getRootNodes()
        )
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
                    Text("Modo Flujo", color = Label1Light, fontWeight = FontWeight.Bold)
                    Text("Sin conexión • Sin IA", color = Label3Light, fontSize = 11.sp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1Light),
            actions = {
                IconButton(onClick = {
                    currentNode = FlowNode(
                        id = "root",
                        label = "Modo Flujo",
                        description = "Selecciona una acción",
                        options = FlowModeEngine.getRootNodes()
                    )
                    commandPath = emptyList()
                    selectedParams = emptyMap()
                    executionResult = ""
                }) {
                    Icon(Icons.Default.Refresh, "Reiniciar", tint = Label3Light)
                }
            }
        )

        // ── Ruta de comandos ──────────────────────────────────────────────────
        if (commandPath.isNotEmpty()) {
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    commandPath.forEachIndexed { index, step ->
                        Text(
                            step,
                            fontSize = 12.sp,
                            color = Label1Light,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(Purple, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                        if (index < commandPath.size - 1) {
                            Text(" → ", fontSize = 12.sp, color = Label3Light, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                }
            }
        }

        // ── Descripción del nodo actual ───────────────────────────────────────
        currentNode?.let { node ->
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                color = Color(0xFFF3E5F5),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        node.label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Label1Light
                    )
                    if (node.description.isNotEmpty()) {
                        Text(
                            node.description,
                            fontSize = 12.sp,
                            color = Label3Light,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // ── Opciones horizontales dinámicas ───────────────────────────────────
        currentNode?.let { node ->
            if (node.options.isNotEmpty()) {
                LazyRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(node.options.size) { index ->
                        val option = node.options[index]
                        FlowOptionButton(
                            option = option,
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    selectedParams = selectedParams + option.params
                                    commandPath = commandPath + option.label

                                    if (option.nextNodeId != null) {
                                        val nextNode = FlowModeEngine.getNodeById(
                                            vm.app,
                                            option.nextNodeId
                                        )
                                        if (nextNode != null) {
                                            currentNode = nextNode
                                        }
                                    } else {
                                        // Ejecutar acción final
                                        executionResult = executeFlowAction(
                                            vm,
                                            option,
                                            selectedParams
                                        )
                                        commandPath = emptyList()
                                        selectedParams = emptyMap()
                                        currentNode = FlowNode(
                                            id = "root",
                                            label = "Modo Flujo",
                                            description = "Selecciona una acción",
                                            options = FlowModeEngine.getRootNodes()
                                        )
                                    }
                                    isLoading = false
                                }
                            }
                        )
                    }
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

        Spacer(Modifier.weight(1f))

        // ── Estado de carga ───────────────────────────────────────────────────
        if (isLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Purple, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun FlowOptionButton(
    option: FlowOption,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(48.dp)
            .widthIn(min = 100.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Purple),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            option.label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = OnPurple,
            textAlign = TextAlign.Center
        )
    }
}

private fun executeFlowAction(
    vm: MainViewModel,
    option: FlowOption,
    params: Map<String, String>
): String {
    // Aquí irían las acciones reales del dispositivo
    return when {
        params.containsKey("package") -> "App abierta"
        params.containsKey("action") -> "Acción ejecutada"
        params.containsKey("level") -> "Configuración actualizada"
        else -> "Acción completada"
    }
}
