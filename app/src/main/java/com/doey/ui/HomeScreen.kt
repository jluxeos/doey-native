package com.doey.ui

import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.navigation.NavController
import com.doey.agent.FlowModeEngine
import com.doey.agent.FlowOption
import com.doey.agent.PipelineState
import com.doey.agent.FriendlyMessagesProvider
import com.doey.services.NowPlayingInfo
import com.doey.tools.ToolResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ── Fuente estilo iOS/CarPlay ──────────────────────────────────────────────────
private val CarPlayFont = FontFamily.Default

// ── Paleta de colores CarPlay / iOS ───────────────────────────────────────────
private val CPBackground    = Color(0xFF000000)
private val CPSurface       = Color(0xFF1C1C1E)
private val CPSurface2      = Color(0xFF2C2C2E)
private val CPSurface3      = Color(0xFF3A3A3C)
private val CPAccent        = Color(0xFF007AFF)   // Azul iOS
private val CPAccentGreen   = Color(0xFF34C759)   // Verde iOS
private val CPAccentRed     = Color(0xFFFF3B30)   // Rojo iOS
private val CPAccentOrange  = Color(0xFFFF9500)   // Naranja iOS
private val CPAccentPurple  = Color(0xFFAF52DE)   // Púrpura iOS
private val CPText          = Color(0xFFFFFFFF)
private val CPTextSecondary = Color(0xFF8E8E93)
private val CPTextTertiary  = Color(0xFF48484A)
private val CPSeparator     = Color(0xFF38383A)

enum class HomeMode { STANDARD, FLOW, DRIVE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel, nav: NavController) {
    val state     by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    var input     by remember { mutableStateOf("") }
    var voiceEnabled by remember { mutableStateOf(true) }
    var currentMode  by remember { mutableStateOf(HomeMode.STANDARD) }
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.isDrivingMode) {
        if (!state.isDrivingMode && currentMode == HomeMode.DRIVE) {
            currentMode = HomeMode.STANDARD
        } else if (state.isDrivingMode && currentMode != HomeMode.DRIVE) {
            currentMode = HomeMode.DRIVE
        }
    }

    var flowOptions by remember { mutableStateOf(FlowModeEngine.getRootOptions()) }
    var flowHistory by remember { mutableStateOf(listOf<List<FlowOption>>()) }
    var flowLabel   by remember { mutableStateOf("") }
    var flowLoading by remember { mutableStateOf(false) }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    when (currentMode) {
        HomeMode.DRIVE -> CarPlayModeContent(
            vm     = vm,
            state  = state,
            onExit = {
                vm.toggleDrivingMode()
                currentMode = HomeMode.STANDARD
            }
        )
        else -> StandardFlowContent(
            vm                  = vm,
            state               = state,
            currentMode         = currentMode,
            onModeChange        = { currentMode = it },
            input               = input,
            onInputChange       = { input = it },
            voiceEnabled        = voiceEnabled,
            onVoiceToggle       = { voiceEnabled = !voiceEnabled },
            listState           = listState,
            flowOptions         = flowOptions,
            flowHistory         = flowHistory,
            flowLabel           = flowLabel,
            flowLoading         = flowLoading,
            onFlowOptionsChange = { flowOptions = it },
            onFlowHistoryChange = { flowHistory = it },
            onFlowLabelChange   = { flowLabel = it },
            onFlowLoadingChange = { flowLoading = it },
            ctx                 = ctx,
            scope               = scope
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StandardFlowContent(
    vm: MainViewModel,
    state: MainUiState,
    currentMode: HomeMode,
    onModeChange: (HomeMode) -> Unit,
    input: String,
    onInputChange: (String) -> Unit,
    voiceEnabled: Boolean,
    onVoiceToggle: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    flowOptions: List<FlowOption>,
    flowHistory: List<List<FlowOption>>,
    flowLabel: String,
    flowLoading: Boolean,
    onFlowOptionsChange: (List<FlowOption>) -> Unit,
    onFlowHistoryChange: (List<List<FlowOption>>) -> Unit,
    onFlowLabelChange: (String) -> Unit,
    onFlowLoadingChange: (Boolean) -> Unit,
    ctx: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val isFlow = currentMode == HomeMode.FLOW
    val bgColor    = if (isFlow) Color(0xFF0D0717) else Surface0Light
    val surfaceBar = if (isFlow) Color(0xFF12082A) else Surface1Light
    val titleColor = if (isFlow) Color(0xFFD0BCFF) else Label1Light

    Column(Modifier.fillMaxSize().background(bgColor)) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DoeyIcon(modifier = Modifier, size = 24.dp,
                        tint = if (isFlow) Color(0xFFBB86FC) else Purple)
                    Spacer(Modifier.width(8.dp))
                    Text("Doey", fontWeight = FontWeight.Bold, color = titleColor)
                    if (state.isWakeWordActive) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(50),
                            color = if (isFlow) Color(0xFF1F3A1F) else Color(0xFFDCF5DC)) {
                            Text("  ● activo  ", fontSize = 10.sp,
                                color = if (isFlow) Color(0xFF66BB6A) else Color(0xFF1A7A1A),
                                modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            },
            actions = {
                // PUNTO 2: Botón para abrir manualmente el modo Friendly
                IconButton(onClick = {
                    try {
                        ctx.startService(Intent(ctx, com.doey.services.FriendlyModeService::class.java).apply {
                            action = "SHOW_BAR"
                        })
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Error al abrir modo Friendly", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.Spa, "Modo Friendly", tint = TauGreen)
                }
                
                IconButton(onClick = { vm.toggleWakeWord() }) {
                    Icon(if (state.isWakeWordActive) Icons.Default.RecordVoiceOver else Icons.Default.VoiceOverOff, 
                        "Palabra de activación",
                        tint = if (state.isWakeWordActive) (if (isFlow) Color(0xFFBB86FC) else Purple) else Label3Light)
                }
                IconButton(onClick = {
                    vm.toggleDrivingMode()
                    onModeChange(HomeMode.DRIVE)
                }) {
                    Icon(Icons.Default.DirectionsCar, "Modo Conducción",
                        tint = if (isFlow) Color(0xFF9E9E9E) else Label3Light)
                }
                IconButton(onClick = {
                    if (isFlow) onModeChange(HomeMode.STANDARD) else onModeChange(HomeMode.FLOW)
                }) {
                    Icon(Icons.Default.AccountTree, "Toggle Modo Flujo",
                        tint = if (isFlow) Color(0xFFBB86FC) else Label3Light)
                }
                IconButton(onClick = { vm.clearHistory() }) {
                    Icon(Icons.Default.Delete, "Borrar",
                        tint = if (isFlow) Color(0xFF9E9E9E) else Label3Light)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceBar)
        )

        AnimatedVisibility(isFlow, enter = expandVertically(), exit = shrinkVertically()) {
            Box(
                Modifier.fillMaxWidth().background(Color(0xFF1A0A2E))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF6200EE)) {
                        Text("⚡", fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("MODO FLUJO", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFBB86FC), letterSpacing = 2.sp)
                        Text("Sin internet · Acciones instantáneas", fontSize = 11.sp,
                            color = Color(0xFF9E9E9E), lineHeight = 14.sp)
                    }
                    TextButton(
                        onClick = { onModeChange(HomeMode.STANDARD) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("SALIR", color = Color(0xFFBB86FC),
                            fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        if (state.errorMessage != null) {
            LaunchedEffect(state.errorMessage) { delay(3000L); vm.clearError() }
            Surface(Modifier.fillMaxWidth(), color = Color(0xFFF9DEDC)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, null, tint = ErrorRed)
                    Spacer(Modifier.width(12.dp))
                    Text(state.errorMessage ?: "", color = Color(0xFF410002), modifier = Modifier.weight(1f), fontSize = 13.sp)
                    IconButton(onClick = { vm.clearError() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = ErrorRed)
                    }
                }
            }
        }

        LazyColumn(
            Modifier.weight(1f), listState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.messages.isEmpty() && state.pipelineState == PipelineState.IDLE) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp)
                    ) {
                        EmptyState(isFlow)
                    }
                }
            }
            items(state.messages, key = { msg: ChatMessage -> msg.id }) { msg ->
                ChatBubble(msg, isFlow)
            }
            if (state.partialSpeech.isNotBlank()) {
                item { PartialBubble(state.partialSpeech) }
            }
            if (state.pipelineState == PipelineState.PROCESSING) {
                item { FriendlyMessageBubble() }
            }
        }

        AnimatedVisibility(isFlow, enter = expandVertically(), exit = shrinkVertically()) {
            Surface(color = Color(0xFF12082A)) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (flowHistory.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    onFlowOptionsChange(flowHistory.last())
                                    onFlowHistoryChange(flowHistory.dropLast(1))
                                    onFlowLabelChange("")
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, null,
                                    tint = Color(0xFFBB86FC), modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Spacer(Modifier.width(32.dp))
                        }
                        Text(
                            if (flowLabel.isBlank()) "¿Qué hacemos?" else flowLabel,
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD0BCFF),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        if (flowLoading) {
                            CircularProgressIndicator(
                                Modifier.size(16.dp), strokeWidth = 2.dp,
                                color = Color(0xFFBB86FC))
                        }
                        if (flowHistory.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    onFlowOptionsChange(FlowModeEngine.getRootOptions())
                                    onFlowHistoryChange(emptyList())
                                    onFlowLabelChange("")
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Inicio", color = Color(0xFFBB86FC), fontSize = 11.sp)
                            }
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(flowOptions) { option ->
                            Surface(
                                modifier = Modifier.height(40.dp).clickable {
                                    scope.launch {
                                        when {
                                            option.command != null -> {
                                                onFlowLoadingChange(true)
                                                try {
                                                    val res = FlowModeEngine.executeCommand(ctx, option.command)
                                                    val msg = res.forUser
                                                        ?: if (res.isError) res.forLLM
                                                        else "✅ ${option.label}"
                                                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                                                    onFlowOptionsChange(FlowModeEngine.getRootOptions())
                                                    onFlowHistoryChange(emptyList())
                                                    onFlowLabelChange("")
                                                } catch (e: Exception) {
                                                    Toast.makeText(ctx,
                                                        "Error: ${e.message ?: "desconocido"}",
                                                        Toast.LENGTH_LONG).show()
                                                } finally {
                                                    onFlowLoadingChange(false)
                                                }
                                            }
                                            option.nextNodeId != null -> {
                                                onFlowLoadingChange(true)
                                                try {
                                                    val next = FlowModeEngine.getNodeById(
                                                        ctx, option.nextNodeId, option.params)
                                                    if (next != null) {
                                                        onFlowHistoryChange(flowHistory + listOf(flowOptions))
                                                        onFlowOptionsChange(next.options)
                                                        onFlowLabelChange(next.label)
                                                    }
                                                } finally {
                                                    onFlowLoadingChange(false)
                                                }
                                            }
                                        }
                                    }
                                },
                                shape  = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFF6200EE)),
                                color  = if (option.nextNodeId != null && option.command == null)
                                    Color(0xFF2A0A4A) else Color(0xFF1A0A2E)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (option.icon.isNotBlank()) {
                                        Text(option.icon, fontSize = 14.sp)
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(option.label, color = Color(0xFFD0BCFF),
                                        fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    if (option.nextNodeId != null && option.command == null) {
                                        Spacer(Modifier.width(2.dp))
                                        Text("›", color = Color(0xFFBB86FC),
                                            fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(color = surfaceBar) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = input,
                    onValueChange = onInputChange,
                    modifier      = Modifier.weight(1f),
                    placeholder   = {
                        Text("Escribe un mensaje…",
                            color = if (isFlow) Color(0xFF6B5E7E) else Label3Light,
                            fontSize = 14.sp)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = if (isFlow) Color(0xFFBB86FC) else Purple,
                        unfocusedBorderColor = if (isFlow) Color(0xFF4A3A5E) else Label3Light,
                        focusedTextColor     = if (isFlow) Color(0xFFEAE0F8) else Label1Light,
                        unfocusedTextColor   = if (isFlow) Color(0xFFEAE0F8) else Label1Light,
                        cursorColor          = if (isFlow) Color(0xFFBB86FC) else Purple
                    ),
                    singleLine = true,
                    shape      = RoundedCornerShape(24.dp),
                    enabled    = state.pipelineState != PipelineState.PROCESSING
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            vm.sendMessage(input)
                            onInputChange("")
                        } else {
                            when {
                                state.isListening -> vm.stopListening()
                                state.pipelineState == PipelineState.SPEAKING -> vm.stopSpeaking()
                                else -> vm.startListening()
                            }
                        }
                    },
                    enabled = state.pipelineState != PipelineState.PROCESSING,
                    colors  = IconButtonDefaults.filledIconButtonColors(
                        containerColor = when {
                            input.isNotBlank() -> (if (isFlow) Color(0xFF6200EE) else Purple)
                            state.isListening -> Color(0xFF1F3A1F)
                            state.pipelineState == PipelineState.SPEAKING -> Color(0xFF3A1A1A)
                            isFlow -> Color(0xFF4A2A7A)
                            else -> PurpleDark
                        }
                    )
                ) {
                    Icon(
                        when {
                            input.isNotBlank() -> Icons.Default.Send
                            state.isListening -> Icons.Default.MicOff
                            state.pipelineState == PipelineState.SPEAKING -> Icons.Default.Stop
                            else -> Icons.Default.Mic
                        },
                        contentDescription = "Enviar", tint = OnPurple
                    )
                }
                Spacer(Modifier.width(4.dp))
                FilledIconButton(
                    onClick  = onVoiceToggle,
                    enabled  = state.pipelineState != PipelineState.PROCESSING,
                    colors   = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (voiceEnabled)
                            (if (isFlow) Color(0xFF6200EE) else Purple)
                        else Color(0xFF9C27B0)
                    )
                ) {
                    Icon(
                        if (voiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Voz", tint = OnPurple
                    )
                }
            }
        }
    }
}
// (Rest of the file remains unchanged)
