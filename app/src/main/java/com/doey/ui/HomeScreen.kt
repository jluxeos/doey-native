package com.doey.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.agent.PipelineState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel) {
    val state     by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    var input     by remember { mutableStateOf("") }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(Modifier.fillMaxSize().background(Surface0)) {

        // ── Barra superior ────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🤖", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Doey", fontWeight = FontWeight.Bold, color = Label1)
                    if (state.isWakeWordActive) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(50), color = Color(0xFF1D3A1D)) {
                            Text("  ● wake  ", fontSize = 10.sp, color = Color(0xFF79FF79),
                                modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            },
            actions = {
                IconButton(onClick = { vm.toggleDrivingMode() }) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = "Driving",
                        tint = if (state.isDrivingMode) Purple else Label3
                    )
                }
                IconButton(onClick = { vm.clearHistory() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Label3)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1)
        )

        // ── Banner modo conducción ────────────────────────────────────────────
        AnimatedVisibility(state.isDrivingMode) {
            Surface(Modifier.fillMaxWidth(), color = PurpleDark) {
                Text("🚗  Driving Mode – Voice responses active",
                    Modifier.padding(8.dp), color = Color(0xFFEADDFF),
                    fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }

        // ── Banner de error ───────────────────────────────────────────────────
        state.errorMessage?.let { err ->
            Surface(Modifier.fillMaxWidth(), color = Color(0xFF93000A)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(err, color = ErrorRed, modifier = Modifier.weight(1f), fontSize = 13.sp)
                    IconButton(onClick = { vm.clearError() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = ErrorRed)
                    }
                }
            }
        }

        // ── Lista de mensajes ─────────────────────────────────────────────────
        LazyColumn(
            Modifier.weight(1f), listState,
            contentPadding    = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.messages.isEmpty() && state.pipelineState == PipelineState.IDLE) {
                item { EmptyState() }
            }
            items(state.messages, key = { it.id }) { msg -> ChatBubble(msg) }

            if (state.partialSpeech.isNotBlank()) {
                item { PartialBubble(state.partialSpeech) }
            }
            if (state.pipelineState == PipelineState.PROCESSING) {
                item { ThinkingBubble() }
            }
        }

        // ── Banner de estado ──────────────────────────────────────────────────
        AnimatedVisibility(
            state.pipelineState == PipelineState.LISTENING ||
            state.pipelineState == PipelineState.SPEAKING
        ) {
            Surface(Modifier.fillMaxWidth(),
                color = if (state.pipelineState == PipelineState.LISTENING) Color(0xFF1D3A1D) else Surface1) {
                Text(
                    if (state.pipelineState == PipelineState.LISTENING) "🎙️  Listening…" else "🔊  Speaking…",
                    Modifier.padding(8.dp), color = Label1, fontSize = 13.sp, textAlign = TextAlign.Center
                )
            }
        }

        // ── Barra de input ────────────────────────────────────────────────────
        Surface(color = Surface1) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value       = input,
                    onValueChange = { input = it },
                    modifier    = Modifier.weight(1f),
                    placeholder = { Text("Type a message…", color = Label3, fontSize = 14.sp) },
                    colors      = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Purple,
                        unfocusedBorderColor = Color(0xFF4A4458),
                        focusedTextColor     = Label1,
                        unfocusedTextColor   = Label1,
                        cursorColor          = Purple
                    ),
                    singleLine = true,
                    shape      = RoundedCornerShape(24.dp),
                    enabled    = state.pipelineState != PipelineState.PROCESSING
                )
                Spacer(Modifier.width(8.dp))

                // Micrófono / Stop
                FilledIconButton(
                    onClick  = {
                        when {
                            state.isListening -> vm.stopListening()
                            state.pipelineState == PipelineState.SPEAKING -> vm.stopSpeaking()
                            else -> vm.startListening()
                        }
                    },
                    enabled = state.pipelineState != PipelineState.PROCESSING,
                    colors  = IconButtonDefaults.filledIconButtonColors(
                        containerColor = when {
                            state.isListening -> Color(0xFF1D3A1D)
                            state.pipelineState == PipelineState.SPEAKING -> Color(0xFF3A1D1D)
                            else -> PurpleDark
                        }
                    )
                ) {
                    Icon(
                        when {
                            state.isListening -> Icons.Default.MicOff
                            state.pipelineState == PipelineState.SPEAKING -> Icons.Default.Stop
                            else -> Icons.Default.Mic
                        },
                        contentDescription = "Mic", tint = Label1
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Enviar
                FilledIconButton(
                    onClick  = { val t = input.trim(); if (t.isNotBlank()) { input = ""; vm.sendMessage(t) } },
                    enabled  = input.isNotBlank() && state.pipelineState != PipelineState.PROCESSING,
                    colors   = IconButtonDefaults.filledIconButtonColors(containerColor = Purple)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = OnPurple)
                }
            }
        }
    }
}

// ── Componentes ───────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        if (!isUser) Text("🤖 ", fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp))
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isUser) 16.dp else 4.dp,
                topEnd   = if (isUser) 4.dp else 16.dp,
                bottomStart = 16.dp, bottomEnd = 16.dp
            ),
            color = if (isUser) PurpleDark else Surface1,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(msg.text, Modifier.padding(12.dp),
                color = if (isUser) Color(0xFFEADDFF) else Label1,
                fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun PartialBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(shape = RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp), color = Surface2,
            modifier = Modifier.widthIn(max = 280.dp)) {
            Text(text, Modifier.padding(12.dp), color = Label3, fontSize = 14.sp, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
private fun ThinkingBubble() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
        Text("🤖 ", fontSize = 16.sp)
        Surface(shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp), color = Surface1) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Purple)
                Spacer(Modifier.width(8.dp))
                Text("Thinking…", color = Label3, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🤖", fontSize = 48.sp)
        Text("Hi, I'm Doey!", fontWeight = FontWeight.Bold, color = Label1, fontSize = 20.sp)
        Text("Your personal AI assistant.\nTap the mic or type to get started.",
            color = Label3, textAlign = TextAlign.Center, fontSize = 14.sp)
    }
}
