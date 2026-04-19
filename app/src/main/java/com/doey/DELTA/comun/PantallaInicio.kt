package com.doey.DELTA.comun

import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.doey.MainViewModel
import com.doey.NEXUS.FlowModeEngine
import com.doey.NEXUS.FlowNode
import com.doey.NEXUS.FlowOption
import com.doey.NEXUS.PipelineState
import com.doey.VEIL.FriendlyModeService
import com.doey.DELTA.core.*

@Composable
fun HomeScreen(vm: MainViewModel, nav: NavController) {
    val state     by vm.uiState.collectAsState()
    val listState  = rememberLazyListState()
    var input      by remember { mutableStateOf("") }
    val scope      = rememberCoroutineScope()
    val settings   = remember { vm.getSettings() }
    val ctx        = LocalContext.current

    // Detectar si el dispositivo está en modo ahorro de energía o tiene poca RAM
    val isLowPower = remember {
        val pm = ctx.getSystemService(android.content.Context.POWER_SERVICE) as? PowerManager
        val isPowerSave = pm?.isPowerSaveMode ?: false
        val ramMb = with(android.app.ActivityManager.MemoryInfo()) {
            (ctx.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager)
                .getMemoryInfo(this); (totalMem / 1024 / 1024).toInt()
        }
        isPowerSave || ramMb <= 3000  // ≤3 GB RAM → modo ligero
    }

    var flowExpanded  by remember { mutableStateOf(false) }
    var currentNode   by remember { mutableStateOf<FlowNode?>(null) }
    var currentParams by remember { mutableStateOf(mapOf<String, String>()) }
    var flowFeedback  by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val theme = settings.getTheme()
        updateGlassTheme(theme)
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            if (isLowPower)
                listState.scrollToItem(state.messages.size - 1)
            else
                listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    LaunchedEffect(flowFeedback) {
        if (flowFeedback != null) { delay(2500); flowFeedback = null }
    }

    Box(Modifier.fillMaxSize()) {
        DeltaBackground()

        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(8.dp))

            // ── Chat ─────────────────────────────────────────────────
            LazyColumn(
                modifier         = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                state            = listState,
                verticalArrangement  = Arrangement.spacedBy(12.dp),
                contentPadding   = PaddingValues(vertical = 12.dp)
            ) {
                if (state.messages.isEmpty()) {
                    item { DeltaEmptyState() }
                }
                items(state.messages) { msg ->
                    DeltaChatBubble(msg)
                }
            }

            // ── Feedback de acción rápida ─────────────────────────────
            AnimatedVisibility(
                visible = flowFeedback != null,
                enter   = if (isLowPower) EnterTransition.None else fadeIn(tween(200)) + slideInVertically { it / 2 },
                exit    = if (isLowPower) ExitTransition.None  else fadeOut(tween(150)) + slideOutVertically { it / 2 }
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeltaAccent.copy(alpha = 0.12f))
                        .border(1.dp, DeltaAccent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text       = flowFeedback ?: "",
                        color      = DeltaAccentSoft,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── Modo Flujo — carrusel ────────────────────────────────
            AnimatedVisibility(
                visible = flowExpanded,
                enter   = if (isLowPower) EnterTransition.None else fadeIn(tween(200)) + expandVertically(),
                exit    = if (isLowPower) ExitTransition.None  else fadeOut(tween(150)) + shrinkVertically()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(DeltaSurface1)
                        .border(
                            width  = 1.dp,
                            color  = DeltaSeparator,
                            shape  = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        )
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(6.dp).clip(CircleShape).background(DeltaAccent)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text       = currentNode?.label ?: "Acciones rápidas",
                                color      = DeltaText2,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            "✕",
                            color    = DeltaText3,
                            fontSize = 16.sp,
                            modifier = Modifier.clickable {
                                flowExpanded  = false
                                currentNode   = null
                                currentParams = emptyMap()
                            }
                        )
                    }

                    val options = currentNode?.options ?: FlowModeEngine.getRootOptions()
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding        = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(options) { opt ->
                            DeltaFlowChip(opt) {
                                when {
                                    opt.nextNodeId != null -> {
                                        val merged = currentParams + opt.params
                                        scope.launch {
                                            currentNode   = FlowModeEngine.getNodeById(ctx, opt.nextNodeId, merged)
                                            currentParams = merged
                                        }
                                    }
                                    opt.command != null -> {
                                        scope.launch {
                                            val result = FlowModeEngine.executeCommand(ctx, opt.command)
                                            flowFeedback  = result.forUser ?: opt.label
                                            flowExpanded  = false
                                            currentNode   = null
                                            currentParams = emptyMap()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Barra de entrada ─────────────────────────────────────
            DeltaInputBar(
                input        = input,
                onInputChange = { input = it },
                state        = state.pipelineState,
                flowExpanded = flowExpanded,
                onFlowToggle = {
                    flowExpanded  = !flowExpanded
                    currentNode   = null
                    currentParams = emptyMap()
                },
                onFriendlyMode = {
                    val intent = Intent(ctx, FriendlyModeService::class.java).apply {
                        action = FriendlyModeService.ACTION_SHOW
                        putExtra(FriendlyModeService.EXTRA_CONTEXT_APP, "Inicio")
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                        ctx.startForegroundService(intent)
                    else
                        ctx.startService(intent)
                },
                onMic  = { vm.startListening() },
                onSend = {
                    if (input.isNotBlank()) {
                        vm.sendMessage(input)
                        input = ""
                    }
                },
                onStop = { vm.stopAll() }
            )
        }
    }
}

// ── Chat bubble ──────────────────────────────────────────────────
@Composable
private fun DeltaChatBubble(msg: com.doey.ChatMessage) {
    val isUser = msg.role == "user"
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart    = 18.dp,
                            topEnd      = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd   = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (isUser) DeltaAccent.copy(alpha = 0.18f)
                        else        DeltaSurface2
                    )
                    .border(
                        width = 1.dp,
                        color = if (isUser) DeltaAccent.copy(alpha = 0.30f) else DeltaSeparator,
                        shape = RoundedCornerShape(
                            topStart    = 18.dp,
                            topEnd      = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd   = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                MarkdownText(
                    text  = msg.text,
                    color = DeltaText1
                )
            }
        }
        if (!isUser && msg.respondedBy != null) {
            val label = if (msg.respondedBy == "IRIS") "⚡ IRIS" else "🤖 ${msg.respondedBy}"
            Text(
                text     = label,
                color    = DeltaText3,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 6.dp, top = 3.dp)
            )
        }
    }
}

// ── Estado vacío ─────────────────────────────────────────────────
@Composable
private fun DeltaEmptyState() {
    Box(
        Modifier.fillMaxWidth().padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(DeltaAccent.copy(alpha = 0.08f))
                    .border(1.dp, DeltaAccent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("δ", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = DeltaAccent)
            }
            Spacer(Modifier.height(16.dp))
            Text("Doey Delta", style = DoeyTypography.titleLarge, color = DeltaText1)
            Spacer(Modifier.height(6.dp))
            Text("¿En qué te ayudo hoy?", style = DoeyTypography.bodyMedium, color = DeltaText3)
        }
    }
}

// ── Chip de modo flujo ───────────────────────────────────────────
@Composable
private fun DeltaFlowChip(opt: FlowOption, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DeltaSurface2)
            .border(1.dp, DeltaSeparator, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (opt.icon.isNotBlank()) {
                Text(opt.icon, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
            }
            Text(opt.label, color = DeltaText1, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// También exportar con el alias viejo por si algún archivo lo usa
@Composable
fun FlowChip(opt: FlowOption, onClick: () -> Unit) = DeltaFlowChip(opt, onClick)

// ── Barra de input ───────────────────────────────────────────────
@Composable
private fun DeltaInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    state: PipelineState,
    flowExpanded: Boolean,
    onFlowToggle: () -> Unit,
    onFriendlyMode: () -> Unit,
    onMic: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit = {}
) {
    val isProcessing = state == PipelineState.PROCESSING || state == PipelineState.SPEAKING
    val isListening  = state == PipelineState.LISTENING

    Box(
        Modifier
            .fillMaxWidth()
            .background(DeltaSurface1)
            .border(1.dp, DeltaSeparator, RoundedCornerShape(0.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .imePadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // Botón modo flujo
            IconButton(onClick = onFlowToggle) {
                Text(
                    text     = if (flowExpanded) "✕" else "⚡",
                    fontSize = 18.sp,
                    color    = if (flowExpanded) DeltaText3 else DeltaAccent
                )
            }

            // Botón modo amigable
            IconButton(onClick = onFriendlyMode) {
                Text("🌿", fontSize = 18.sp)
            }

            // Campo de texto
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DeltaSurface2)
                    .border(1.dp, DeltaSeparator, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (input.isEmpty() && !isListening && !isProcessing) {
                    Text(
                        text     = if (isProcessing) "Procesando..." else "Escribe o habla con Doey...",
                        color    = DeltaText3,
                        fontSize = 14.sp
                    )
                }
                if (isListening) {
                    Text("🎙 Escuchando...", color = DeltaAccent, fontSize = 14.sp,
                        fontWeight = FontWeight.Medium)
                } else if (isProcessing) {
                    Text("⏳ Procesando...", color = DeltaText3, fontSize = 14.sp)
                } else {
                    androidx.compose.foundation.text.BasicTextField(
                        value = input,
                        onValueChange = onInputChange,
                        textStyle = DoeyTypography.bodyMedium.copy(color = DeltaText1),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(DeltaAccent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Botón mic (oculto mientras procesa)
            if (!isProcessing) {
                IconButton(onClick = onMic) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Micrófono",
                        tint = if (isListening) DeltaAccent else DeltaText3,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Botón Stop (durante procesamiento) o Enviar
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                        .clickable { onStop() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Detener",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (input.isNotBlank()) DeltaAccent else DeltaSurface3)
                        .clickable(enabled = input.isNotBlank()) { onSend() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = if (input.isNotBlank()) Color.White else DeltaText3,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
