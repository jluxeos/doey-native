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
        // TopAppBar eliminada de aquí para centralizarla en DoeyApp (Barra Superior Limpia)

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

@Composable
private fun EmptyState(isFlow: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(80.dp).clip(CircleShape)
                .background(if (isFlow) Color(0xFF2A1A4A) else Surface1Light),
            contentAlignment = Alignment.Center
        ) {
            DoeyIcon(modifier = Modifier, size = 40.dp,
                tint = if (isFlow) Color(0xFFBB86FC) else Purple)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Hola, soy Doey",
            fontSize = 24.sp, fontWeight = FontWeight.Bold,
            color = if (isFlow) Color(0xFFBB86FC) else Label1Light
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "¿En qué puedo ayudarte hoy?",
            fontSize = 15.sp, color = if (isFlow) Color(0xFF9E9E9E) else Label2Light,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage, isFlow: Boolean) {
    val isUser = msg.role == "user"
    val align  = if (isUser) Alignment.End else Alignment.Start
    val shape  = if (isUser) RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
                 else RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    
    val bgColor = if (isUser) {
        if (isFlow) Color(0xFF4A2A7A) else Purple
    } else {
        if (isFlow) Color(0xFF1A0A2E) else Surface1Light
    }
    
    val textColor = if (isUser) OnPurple else (if (isFlow) Color(0xFFEAE0F8) else Label1Light)

    Column(Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Surface(shape = shape, color = bgColor, border = if (!isUser && isFlow) BorderStroke(1.dp, Color(0xFF4A3A5E)) else null) {
            Column(Modifier.padding(12.dp)) {
                if (!isUser) {
                    Text("Doey", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, 
                         color = if (isFlow) Color(0xFFBB86FC) else Purple, modifier = Modifier.padding(bottom = 4.dp))
                }
                MarkdownText(
                    text      = msg.text,
                    color     = textColor,
                    fontSize  = 15.sp
                )
            }
        }
    }
}

@Composable
private fun PartialBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, Purple.copy(alpha = 0.5f))
        ) {
            Text(
                text,
                Modifier.padding(12.dp),
                fontSize = 15.sp,
                color    = Label2Light,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
private fun FriendlyMessageBubble() {
    val msg = remember { FriendlyMessagesProvider.getWaitingMessage() }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
            color = Surface1Light.copy(alpha = 0.5f)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Purple)
                Spacer(Modifier.width(10.dp))
                Text(msg, fontSize = 13.sp, color = Label2Light)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarPlayModeContent(vm: MainViewModel, state: MainUiState, onExit: () -> Unit) {
    val ctx = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = CPBackground,
        topBar = { CarPlayStatusBar(onExit, state.isWakeWordActive, { vm.toggleWakeWord() }) }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Box(Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> CarPlayDashboard(vm, state, ctx)
                    1 -> CarPlayMusicScreen(vm, state)
                    2 -> CarPlayMapScreen(ctx)
                    3 -> CarPlayAssistantScreen(vm, state)
                    4 -> CarPlayAppsGrid(ctx)
                }
            }
            CarPlayDock(selectedTab = selectedTab, onTabChange = { selectedTab = it })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarPlayStatusBar(onExit: () -> Unit, isWakeWordActive: Boolean, onWakeWordToggle: () -> Unit) {
    val now = remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now.value = LocalDateTime.now(); delay(1000) } }

    Box(
        Modifier.fillMaxWidth().background(CPSurface)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            Text(
                now.value.format(DateTimeFormatter.ofPattern("HH:mm")),
                fontFamily = CarPlayFont,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = CPText
            )
            Spacer(Modifier.width(12.dp))
            Icon(
                if (isWakeWordActive) Icons.Default.RecordVoiceOver else Icons.Default.VoiceOverOff,
                null,
                tint = if (isWakeWordActive) CPAccentGreen else CPTextTertiary,
                modifier = Modifier.size(18.dp).clickable { onWakeWordToggle() }
            )
        }
        Row(
            Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.DirectionsCar, null, tint = CPAccent, modifier = Modifier.size(16.dp))
            Text(
                "CarPlay",
                fontFamily = CarPlayFont,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = CPTextSecondary,
                letterSpacing = 0.3.sp
            )
        }
        TextButton(
            onClick = onExit,
            modifier = Modifier.align(Alignment.CenterEnd),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text(
                "Salir",
                fontFamily = CarPlayFont,
                color = CPAccent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CarPlayDock(selectedTab: Int, onTabChange: (Int) -> Unit) {
    val tabs = listOf(
        Triple(Icons.Default.Dashboard,  "Inicio",    0),
        Triple(Icons.Default.MusicNote,  "Música",    1),
        Triple(Icons.Default.Map,        "Mapa",      2),
        Triple(Icons.Default.Mic,        "Asistente", 3),
        Triple(Icons.Default.Apps,       "Apps",      4)
    )

    Box(Modifier.fillMaxWidth().background(CPSurface).padding(vertical = 4.dp)) {
        HorizontalDivider(color = CPSeparator, thickness = 0.5.dp, modifier = Modifier.align(Alignment.TopCenter))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { (icon, label, idx) ->
                val selected = selectedTab == idx
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabChange(idx) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (selected) 44.dp else 40.dp)
                            .background(
                                if (selected) CPAccent.copy(alpha = 0.15f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon, null,
                            tint = if (selected) CPAccent else CPTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        label,
                        fontFamily = CarPlayFont,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) CPAccent else CPTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun CarPlayDashboard(
    vm: MainViewModel,
    state: MainUiState,
    ctx: android.content.Context
) {
    val now = remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now.value = LocalDateTime.now(); delay(1000) } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                Modifier.fillMaxWidth()
                    .background(CPSurface, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(
                        now.value.format(DateTimeFormatter.ofPattern("HH:mm")),
                        fontFamily = CarPlayFont,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Thin,
                        color = CPText,
                        letterSpacing = (-2).sp
                    )
                    Text(
                        now.value.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                        fontFamily = CarPlayFont,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = CPTextSecondary
                    )
                }
            }
        }

        if (!state.nowPlaying.isEmpty) {
            item {
                CarPlayMiniPlayer(nowPlaying = state.nowPlaying, vm = vm)
            }
        }

        item {
            Text(
                "Accesos rápidos",
                fontFamily = CarPlayFont,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CPTextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
        item {
            val quickActions = listOf(
                QuickAction("Maps",     Icons.Default.Navigation,    CPAccentGreen) {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                QuickAction("Llamar",   Icons.Default.Phone,         CPAccent) {
                    ctx.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                QuickAction("Spotify",  Icons.Default.MusicNote,     CPAccentOrange) {
                    ctx.startActivity(
                        ctx.packageManager.getLaunchIntentForPackage("com.spotify.music")
                            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                QuickAction("Mensajes", Icons.Default.Message,       CPAccentGreen) {
                    ctx.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                QuickAction("WhatsApp", Icons.Default.Chat,          CPAccentGreen) {
                    ctx.startActivity(
                        ctx.packageManager.getLaunchIntentForPackage("com.whatsapp")
                            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                QuickAction("Podcasts", Icons.Default.Podcasts,      CPAccentPurple) {
                    ctx.startActivity(
                        ctx.packageManager.getLaunchIntentForPackage("com.google.android.apps.podcasts")
                            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://podcasts.google.com")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )
            CPQuickActionsGrid(actions = quickActions)
        }

        item { CarPlaySystemStatus() }
    }
}

private data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val action: () -> Unit
)

@Composable
private fun CPQuickActionsGrid(actions: List<QuickAction>) {
    val rows = actions.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { action ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.4f)
                            .background(CPSurface, RoundedCornerShape(14.dp))
                            .clickable { action.action() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier.size(40.dp)
                                    .background(action.color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(action.icon, null, tint = action.color, modifier = Modifier.size(22.dp))
                            }
                            Text(action.label, fontFamily = CarPlayFont, fontSize = 11.sp, color = CPText)
                        }
                    }
                }
                if (row.size < 3) {
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun CarPlayMiniPlayer(nowPlaying: NowPlayingInfo, vm: MainViewModel) {
    Box(
        Modifier.fillMaxWidth()
            .background(CPSurface, RoundedCornerShape(16.dp))
            .clickable { /* Abrir pantalla música */ }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(CPSurface2, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (nowPlaying.artwork != null) {
                    Image(
                        bitmap = nowPlaying.artwork.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.MusicNote, null, tint = CPTextSecondary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    nowPlaying.title.ifBlank { "No se está reproduciendo nada" },
                    fontFamily = CarPlayFont, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = CPText, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    nowPlaying.artist.ifBlank { "Desconocido" },
                    fontFamily = CarPlayFont, fontSize = 13.sp, color = CPTextSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { vm.togglePlayback() }) {
                Icon(
                    if (nowPlaying.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null, tint = CPText, modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun CarPlaySystemStatus() {
    Row(
        Modifier.fillMaxWidth().background(CPSurface, RoundedCornerShape(16.dp)).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatusItem(Icons.Default.BatteryChargingFull, "85%", CPAccentGreen)
        StatusItem(Icons.Default.SignalCellular4Bar, "LTE", CPAccent)
        StatusItem(Icons.Default.Wifi, "Doey_5G", CPAccent)
    }
}

@Composable
private fun StatusItem(icon: ImageVector, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Text(label, fontFamily = CarPlayFont, fontSize = 13.sp, color = CPText)
    }
}

@Composable
private fun CarPlayMusicScreen(vm: MainViewModel, state: MainUiState) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Box(
            Modifier.fillMaxWidth().weight(1f)
                .background(CPSurface, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(160.dp).background(CPSurface2, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.nowPlaying.artwork != null) {
                        Image(
                            bitmap = state.nowPlaying.artwork.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, null, tint = CPTextTertiary, modifier = Modifier.size(64.dp))
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(state.nowPlaying.title.ifBlank { "Sin música" },
                    fontFamily = CarPlayFont, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CPText)
                Text(state.nowPlaying.artist.ifBlank { "Doey Assistant" },
                    fontFamily = CarPlayFont, fontSize = 16.sp, color = CPTextSecondary)
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.skipToPrevious() }) {
                Icon(Icons.Default.SkipPrevious, null, tint = CPText, modifier = Modifier.size(36.dp))
            }
            Box(
                Modifier.size(64.dp).background(CPAccent, CircleShape).clickable { vm.togglePlayback() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (state.nowPlaying.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null, tint = Color.White, modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = { vm.skipToNext() }) {
                Icon(Icons.Default.SkipNext, null, tint = CPText, modifier = Modifier.size(36.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Spotify"  to "com.spotify.music",
                "YT Music" to "com.google.android.apps.youtube.music",
                "YouTube"  to "com.google.android.youtube"
            ).forEach { (label, pkg) ->
                OutlinedButton(
                    onClick = {
                        ctx.startActivity(
                            ctx.packageManager.getLaunchIntentForPackage(pkg)
                                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, CPSurface3),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CPText),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(label, fontFamily = CarPlayFont, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CarPlayMapScreen(ctx: android.content.Context) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(CPSurface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Search, null, tint = CPTextSecondary, modifier = Modifier.size(18.dp))
            Text(
                "¿A dónde vas?",
                fontFamily = CarPlayFont,
                fontSize = 16.sp,
                color = CPTextSecondary,
                modifier = Modifier.weight(1f)
            )
        }

        Box(Modifier.weight(1f)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        loadUrl("https://www.openstreetmap.org/export/embed.html?bbox=-180,-90,180,90&layer=mapnik")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            Modifier.fillMaxWidth().background(CPSurface).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("Casa",       Icons.Default.Home,             "casa"),
                Triple("Trabajo",    Icons.Default.Work,             "trabajo"),
                Triple("Gasolinera", Icons.Default.LocalGasStation,  "gasolinera"),
                Triple("Hospital",   Icons.Default.LocalHospital,    "hospital")
            ).forEach { (label, icon, query) ->
                Column(
                    Modifier.weight(1f)
                        .background(CPSurface2, RoundedCornerShape(10.dp))
                        .clickable {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW,
                                    Uri.parse("google.navigation:q=$query"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(icon, null, tint = CPAccent, modifier = Modifier.size(18.dp))
                    Text(label, fontFamily = CarPlayFont, fontSize = 10.sp, color = CPText)
                }
            }
        }
    }
}

@Composable
private fun CarPlayAssistantScreen(vm: MainViewModel, state: MainUiState) {
    val isListening  = state.isListening
    val isProcessing = state.pipelineState == PipelineState.PROCESSING
    val isSpeaking   = state.pipelineState == PipelineState.SPEAKING
    val listState    = rememberLazyListState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (isListening) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LazyColumn(
            Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.messages.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier.size(80.dp).background(CPAccent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Mic, null, tint = CPAccent, modifier = Modifier.size(40.dp))
                        }
                        Text(
                            "Asistente de voz",
                            fontFamily = CarPlayFont,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CPText
                        )
                        Text(
                            "Toca el micrófono para hablar\ncon el asistente manos libres",
                            fontFamily = CarPlayFont,
                            fontSize = 14.sp,
                            color = CPTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
            items(state.messages, key = { msg: ChatMessage -> msg.id }) { msg ->
                CarPlayChatBubble(msg = msg)
            }
            if (state.partialSpeech.isNotBlank()) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box(
                            Modifier.background(CPSurface2, RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                state.partialSpeech,
                                fontFamily = CarPlayFont,
                                fontSize = 15.sp,
                                color = CPTextSecondary,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
            if (isProcessing) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(32.dp).background(CPAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("D", fontFamily = CarPlayFont, fontSize = 14.sp,
                                fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.background(CPSurface, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                                .padding(12.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(3) { i ->
                                    val dotScale by rememberInfiniteTransition(label = "dot$i")
                                        .animateFloat(
                                            initialValue = 0.6f, targetValue = 1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(400, delayMillis = i * 150),
                                                repeatMode = RepeatMode.Reverse
                                            ), label = "dot$i"
                                        )
                                    Box(
                                        Modifier.size((6 * dotScale).dp)
                                            .background(CPTextSecondary, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(isListening || isProcessing || isSpeaking) {
            Text(
                when {
                    isListening  -> "Escuchando…"
                    isProcessing -> "Procesando…"
                    isSpeaking   -> "Respondiendo…"
                    else         -> ""
                },
                fontFamily = CarPlayFont,
                fontSize = 14.sp,
                color = CPAccent,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            Modifier.size((80 * scale).dp)
                .background(
                    when {
                        isListening  -> CPAccentGreen
                        isSpeaking   -> CPAccentOrange
                        isProcessing -> CPAccentPurple
                        else         -> CPAccent
                    },
                    CircleShape
                )
                .clickable {
                    when {
                        isListening  -> vm.stopListening()
                        isSpeaking   -> vm.stopSpeaking()
                        !isProcessing -> vm.startListening()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                when {
                    isListening  -> Icons.Default.MicOff
                    isSpeaking   -> Icons.Default.Stop
                    else         -> Icons.Default.Mic
                },
                null, tint = Color.White, modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun CarPlayChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                Modifier.size(32.dp).background(CPAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("D", fontFamily = CarPlayFont, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.width(8.dp))
        }
        Box(
            Modifier.background(
                if (isUser) CPAccent else CPSurface,
                if (isUser) RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
                else RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
            ).padding(12.dp)
        ) {
            Text(
                text = msg.text,
                fontFamily = CarPlayFont,
                fontSize = 15.sp,
                color = if (isUser) Color.White else CPText
            )
        }
    }
}

@Composable
private fun CarPlayAppsGrid(ctx: android.content.Context) {
    val apps = listOf(
        Triple("Mapas",     Icons.Default.Map,           CPAccentGreen),
        Triple("Música",    Icons.Default.MusicNote,     CPAccentOrange),
        Triple("Teléfono",  Icons.Default.Phone,         CPAccent),
        Triple("Mensajes",  Icons.Default.Message,       CPAccentGreen),
        Triple("Radio",     Icons.Default.Radio,         CPAccentRed),
        Triple("Calendario",Icons.Default.CalendarToday, CPAccentRed),
        Triple("Ajustes",   Icons.Default.Settings,      CPTextSecondary),
        Triple("Spotify",   Icons.Default.PlayCircle,    CPAccentGreen)
    )

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp)) {
        val rows = apps.chunked(4)
        items(rows) { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { (label, icon, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier.size(60.dp).background(CPSurface, RoundedCornerShape(14.dp))
                                .clickable { /* Launch app */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
                        }
                        Text(label, fontFamily = CarPlayFont, fontSize = 12.sp, color = CPText)
                    }
                }
                if (row.size < 4) {
                    repeat(4 - row.size) { Spacer(Modifier.width(60.dp)) }
                }
            }
        }
    }
}
