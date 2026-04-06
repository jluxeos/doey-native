package com.doey.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                    Text(currentNode?.label ?: "Modo Flujo")
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
                                            // 1. Si hay un comando → ejecutarlo
                                            if (option.command != null) {
                                                val result = FlowModeEngine.executeCommand(ctx, option.command)
                                                Toast.makeText(ctx, result.forUser, Toast.LENGTH_SHORT).show()
                                            }

                                            // 2. Si hay siguiente nodo → navegar
                                            if (option.nextNodeId != null) {
                                                val next = FlowModeEngine.getNodeById(ctx, option.nextNodeId, option.params)
                                                if (next != null) {
                                                    history = history + node
                                                    currentNode = next
                                                } else {
                                                    Toast.makeText(ctx, "No se pudo cargar el siguiente nodo", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            
                                            // 3. Si no hay ni comando ni siguiente nodo, avisar
                                            if (option.command == null && option.nextNodeId == null) {
                                                Toast.makeText(ctx, "Opción informativa: ${option.label}", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (option.params.isNotEmpty()) {
                                    Text(
                                        text = option.params.toString(),
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
