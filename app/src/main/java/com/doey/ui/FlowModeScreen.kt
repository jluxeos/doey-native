package com.doey.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    // Inicializar con root
    LaunchedEffect(Unit) {
        currentNode = FlowModeEngine.getRootNodes().firstOrNull()
    }

    Column(Modifier.fillMaxSize()) {

        TopAppBar(
            title = {
                Text(currentNode?.label ?: "Modo Flujo")
            }
        )

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

                                    // Si hay siguiente nodo → navegar
                                    if (option.nextNodeId != null) {
                                        val next = FlowModeEngine.getNodeById(ctx, option.nextNodeId)

                                        if (next != null) {
                                            history = history + node
                                            currentNode = next
                                        }
                                    }
                                    // Si no hay siguiente nodo → ejecutar acción
                                    else {
                                        option.action?.invoke(ctx, option.params)

                                        Toast
                                            .makeText(ctx, "Acción ejecutada", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }
                            }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(option.label)
                        }
                    }
                }
            }

            // Botón regresar
            if (history.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            currentNode = history.last()
                            history = history.dropLast(1)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⬅ Regresar")
                    }
                }
            }
        }
    }
}