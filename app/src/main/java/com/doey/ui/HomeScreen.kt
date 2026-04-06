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
import com.doey.agent.FriendlyMessagesProvider
import kotlinx.coroutines.delay

import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel, nav: NavController) {
    val state     by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    var input     by remember { mutableStateOf("") }
    var voiceEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(Modifier.fillMaxSize().background(Surface0Light)) {

        // ── Barra superior ────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DoeyIcon(modifier = Modifier, size = 24.dp, tint = Purple)
                    Spacer(Modifier.width(8.dp))
                    Text("Doey", fontWeight = FontWeight.Bold, color = Label1Light)
                    if (state.isWakeWordActive) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(50), color = Color(0xFFDCF5DC)) {
                            Text("  ● activo  ", fontSize = 10.sp, color = Color(0xFF1A7A1A),
                                modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            },
            actions = {
                IconButton(onClick = { nav.navigate(Screen.AutoMode.route) }) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = "Modo Auto",
                        tint = Label3Light
                    )
                }
                IconButton(onClick = { nav.navigate(Screen.FlowMode.route) }) {
                    Icon(
                        Icons.Default.AccountTree,
                        contentDescription = "Modo Flujo",
                        tint = Label3Light
                    )
                }
                IconButton(onClick = { vm.clearHistory() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Label3Light)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1Light)
        )

        // ── Banner modo auto ──────────────────────────────────────────────────
        AnimatedVisibility(state.isDrivingMode) {
            Surface(Modifier.fillMaxWidth(), color = Color(0xFFEADDFF)) {
                Text("🚗  Modo Auto – Interfaz vehicular activada",
                    Modifier.padding(8.dp), color = Color(0xFF21005D),
                    fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }

             // ── Banner de error ─────────────────────────────────────────────
        state.errorMessage?.let { err ->
            // Auto-dismiss en 3 segundos
            LaunchedEffect(err) {
                delay(3000L)
                vm.clearError()
            }
            
            Surface(Modifier.fillMaxWidth(), color = Color(0xFFF9DEDC)) {
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
                item { FriendlyMessageBubble() }
            }
        }

        // ── Banner de estado ──────────────────────────────────────────────────
        AnimatedVisibility(
            state.pipelineState == PipelineState.LISTENING ||
            state.pipelineState == PipelineState.SPEAKING
        ) {
            Surface(Modifier.fillMaxWidth(),
                color = if (state.pipelineState == PipelineState.LISTENING) Color(0xFFDCF5DC) else Surface1Light) {
                Text(
                    if (state.pipelineState == PipelineState.LISTENING) "🎙️  Escuchando…" else "🔊  Hablando…",
                    Modifier.padding(8.dp), color = Label1Light, fontSize = 13.sp, textAlign = TextAlign.Center
                )
            }
        }

        // ── Barra de input ────────────────────────────────────────────────────
        Surface(color = Surface1Light) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value       = input,
                    onValueChange = { input = it },
                    modifier    = Modifier.weight(1f),
                    placeholder = { Text("Escribe un mensaje…", color = Label3Light, fontSize = 14.sp) },
                    colors      = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Purple,
                        unfocusedBorderColor = Label3Light,
                        focusedTextColor     = Label1Light,
                        unfocusedTextColor   = Label1Light,
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
                            state.isListening -> Color(0xFFDCF5DC)
                            state.pipelineState == PipelineState.SPEAKING -> Color(0xFFF9DEDC)
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
                        contentDescription = "Micrófono", tint = OnPurple
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Control TTS (Lectura en voz alta)
                FilledIconButton(
                    onClick  = { voiceEnabled = !voiceEnabled },
                    enabled  = state.pipelineState != PipelineState.PROCESSING,
                    colors   = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (voiceEnabled) Purple else Color(0xFF9C27B0)
                    )
                ) {
                    Icon(
                        if (voiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = if (voiceEnabled) "Desactivar lectura en voz" else "Activar lectura en voz",
                        tint = OnPurple
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Enviar
                FilledIconButton(
                    onClick  = { val t = input.trim(); if (t.isNotBlank()) { input = ""; vm.sendMessage(t, voiceEnabled) } },
                    enabled  = input.isNotBlank() && state.pipelineState != PipelineState.PROCESSING,
                    colors   = IconButtonDefaults.filledIconButtonColors(containerColor = Purple)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar", tint = OnPurple)
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
                topStart    = if (isUser) 16.dp else 4.dp,
                topEnd      = if (isUser) 4.dp else 16.dp,
                bottomStart = 16.dp, bottomEnd = 16.dp
            ),
            color = if (isUser) Color(0xFFEADDFF) else Surface1Light,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            if (isUser) {
                // Mensajes del usuario: texto plano
                Text(msg.text, Modifier.padding(12.dp),
                    color = Color(0xFF21005D),
                    fontSize = 14.sp, lineHeight = 20.sp)
            } else {
                // Respuestas de la IA: renderizar Markdown
                MarkdownText(
                    text      = msg.text,
                    color     = Label1Light,
                    fontSize  = 14.sp,
                    lineHeight = 20.sp,
                    modifier  = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun PartialBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(shape = RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp), color = Surface2Light,
            modifier = Modifier.widthIn(max = 280.dp)) {
            Text(text, Modifier.padding(12.dp), color = Label3Light, fontSize = 14.sp, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
private fun FriendlyMessageBubble() {
    var message by remember { mutableStateOf(FriendlyMessagesProvider.getStartMessage()) }
    var messageIndex by remember { mutableStateOf(0) }
    
    // Cambiar mensaje cada 3-4 segundos
    LaunchedEffect(messageIndex) {
        delay(3500L + (Math.random() * 1000).toLong())
        message = FriendlyMessagesProvider.getWaitingMessage()
        messageIndex++
    }
    
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
        Text("🤖 ", fontSize = 16.sp)
        Surface(shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp), color = Surface1Light) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Purple)
                Spacer(Modifier.width(8.dp))
                Text(message, color = Label2Light, fontSize = 13.sp, fontStyle = FontStyle.Italic)
            }
        }
    }
}

@Composable
private fun ThinkingBubble() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
        Text("🤖 ", fontSize = 16.sp)
        Surface(shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp), color = Surface1Light) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Purple)
                Spacer(Modifier.width(8.dp))
                Text("Procesando…", color = Label3Light, fontSize = 13.sp)
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
        Text("¡Hola, soy Doey!", fontWeight = FontWeight.Bold, color = Label1Light, fontSize = 20.sp)
        Text("Tu asistente de IA personal.\nToca el micrófono o escribe para comenzar.",
            color = Label3Light, textAlign = TextAlign.Center, fontSize = 14.sp)
    }
}