package com.doey.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.doey.agent.FlowModeEngine
import com.doey.agent.FlowNode
import com.doey.agent.FlowOption
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowModeScreen(vm: MainViewModel) {

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentNode by remember { mutableStateOf<FlowNode?>(null) }
    var history by remember { mutableStateOf(listOf<FlowNode>()) }
    var isLoading by remember { mutableStateOf(false) }

    // Inicializar con root
    LaunchedEffect(Unit) {
        currentNode = FlowModeEngine.getRootNodes().firstOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(currentNode?.label ?: "Más Opciones", fontWeight = FontWeight.Bold)
                        Text("Modo Flujo Offline", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                },
                navigationIcon = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = {
                            currentNode = history.last()
                            history = history.dropLast(1)
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Banner informativo
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Estás en el panel extendido del Modo Flujo. Aquí puedes acceder a todas las acciones predefinidas que funcionan sin internet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                currentNode?.let { node ->
                    items(node.options) { option ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    scope.launch {
                                        isLoading = true
                                        try {
                                            if (option.command != null) {
                                                val result = FlowModeEngine.executeCommand(ctx, option.command)
                                                Toast.makeText(ctx, result.forUser, Toast.LENGTH_SHORT).show()
                                            }

                                            if (option.nextNodeId != null) {
                                                val next = FlowModeEngine.getNodeById(ctx, option.nextNodeId, option.params)
                                                if (next != null) {
                                                    history = history + node
                                                    currentNode = next
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (option.params.isNotEmpty() && option.nextNodeId != null) {
                                    Text(
                                        text = "Navegar a ${option.nextNodeId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
