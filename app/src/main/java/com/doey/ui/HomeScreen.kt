package com.doey.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.agent.FlowModeEngine
import com.doey.agent.FlowOption
import com.doey.agent.FriendlyMessagesProvider
import com.doey.agent.PipelineState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.navigation.NavController

// ── Enum de modo de pantalla ──────────────────────────────────────────────────

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

    // Sincronizar modo conducción con el estado persistido del VM
    // Solo al montar — no forzar true automáticamente
    LaunchedEffect(state.isDrivingMode) {
        if (!state.isDrivingMode && currentMode == HomeMode.DRIVE) {
            currentMode = HomeMode.STANDARD
        }
    }

    // Estado del carrusel Modo Flujo
    var flowOptions by remember { mutableStateOf(FlowModeEngine.getRootOptions()) }
    var flowHistory by remember { mutableStateOf(listOf<List<FlowOption>>()) }
    var flowLabel   by remember { mutableStateOf("") }
    var flowLoading by remember { mutableStateOf(false) }

    // Auto-scroll al último mensaje
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    // ── Dispatch de UI según modo ─────────────────────────────────────────────
    when (currentMode) {
        HomeMode.DRIVE -> DriveModeContent(
            vm       = vm,
            state    = state,
            onExit   = {
                vm.toggleDrivingMode()
                currentMode = HomeMode.STANDARD
            }
        )
        else -> StandardFlowContent(
            vm           = vm,
            state        = state,
            nav          = nav,
            currentMode  = currentMode,
            onModeChange = { currentMode = it },
            input        = input,
            onInputChange = { input = it },
            voiceEnabled = voiceEnabled,
            onVoiceToggle = { voiceEnabled = !voiceEnabled },
            listState    = listState,
            flowOptions  = flowOptions,
            flowHistory  = flowHistory,
            flowLabel    = flowLabel,
            flowLoading  = flowLoading,
            onFlowOptionsChange = { flowOptions = it },
            onFlowHistoryChange = { flowHistory = it },
            onFlowLabelChange   = { flowLabel = it },
            onFlowLoadingChange = { flowLoading = it },
            ctx          = ctx,
            scope        = scope
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MODO ESTÁNDAR + FLUJO  (misma pantalla, distintos banners/empty state)
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StandardFlowContent(
    vm: MainViewModel,
    state: MainUiState,
    nav: NavController,
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

    // Colores dinámicos según modo
    val bgColor     = if (isFlow) Color(0xFF0D0717) else Surface0Light
    val surfaceBar  = if (isFlow) Color(0xFF12082A) else Surface1Light
    val titleColor  = if (isFlow) Color(0xFFD0BCFF) else Label1Light

    Column(Modifier.fillMaxSize().background(bgColor)) {

        // ── TopAppBar ─────────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DoeyIcon(modifier = Modifier, size = 24.dp, tint = if (isFlow) Color(0xFFBB86FC) else Purple)
                    Spacer(Modifier.width(8.dp))
                    Text("Doey", fontWeight = FontWeight.Bold, color = titleColor)
                    if (state.isWakeWordActive) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(50), color = if (isFlow) Color(0xFF1F3A1F) else Color(0xFFDCF5DC)) {
                            Text("  ● activo  ", fontSize = 10.sp,
                                color = if (isFlow) Color(0xFF66BB6A) else Color(0xFF1A7A1A),
                                modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            },
            actions = {
                // Botón modo conducción
                IconButton(onClick = {
                    vm.toggleDrivingMode()
                    onModeChange(HomeMode.DRIVE)
                }) {
                    Icon(Icons.Default.DirectionsCar, "Modo Conducción",
                        tint = if (isFlow) Color(0xFF9E9E9E) else Label3Light)
                }
                // Botón modo flujo
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

        // ── Banner Modo Flujo (oscuro/dark) ───────────────────────────────────
        AnimatedVisibility(isFlow, enter = expandVertically(), exit = shrinkVertically()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A0A2E))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF6200EE)) {
                        Text("⚡", fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(text = "MODO FLUJO", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFBB86FC), letterSpacing = 2.sp)
                        Text("Sin internet · Acciones instantáneas", fontSize = 11.sp,
                            color = Color(0xFF9E9E9E), lineHeight = 14.sp)
                    }
                    TextButton(
                        onClick = { onModeChange(HomeMode.STANDARD) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("SALIR", color = Color(0xFFBB86FC), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // ── Banner de error ───────────────────────────────────────────────────
        if (state.errorMessage != null) {
            LaunchedEffect(state.errorMessage) { delay(3000L); vm.clearError() }
            Surface(Modifier.fillMaxWidth(), color = Color(0xFFF9DEDC)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(state.errorMessage!!, color = ErrorRed, modifier = Modifier.weight(1f), fontSize = 13.sp)
                    IconButton(onClick = { vm.clearError() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = ErrorRed)
                    }
                }
            }
        }

        // ── Lista de mensajes ─────────────────────────────────────────────────
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
            items(state.messages, key = { msg: ChatMessage -> msg.id }) { msg -> ChatBubble(msg, isFlow) }
            if (state.partialSpeech.isNotBlank()) {
                item { PartialBubble(state.partialSpeech) }
            }
            if (state.pipelineState == PipelineState.PROCESSING) {
                item { FriendlyMessageBubble() }
            }
        }

        // ── Carrusel Modo Flujo ───────────────────────────────────────────────
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
                                Icon(Icons.Default.ArrowBack, null, tint = Color(0xFFBB86FC), modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Spacer(Modifier.width(32.dp))
                        }
                        Text(
                            if (flowLabel.isBlank()) "¿Qué hacemos?" else flowLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD0BCFF),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        if (flowLoading) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFFBB86FC))
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
                                                        ?: if (res.isError) res.forLLM else "✅ ${option.label}"
                                                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                                                    onFlowOptionsChange(FlowModeEngine.getRootOptions())
                                                    onFlowHistoryChange(emptyList())
                                                    onFlowLabelChange("")
                                                } catch (e: Exception) {
                                                    Toast.makeText(ctx, "Error: ${e.message ?: "desconocido"}", Toast.LENGTH_LONG).show()
                                                } finally {
                                                    onFlowLoadingChange(false)
                                                }
                                            }
                                            option.nextNodeId != null -> {
                                                onFlowLoadingChange(true)
                                                try {
                                                    val next = FlowModeEngine.getNodeById(ctx, option.nextNodeId, option.params)
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
                                    Text(option.label, color = Color(0xFFD0BCFF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    if (option.nextNodeId != null && option.command == null) {
                                        Spacer(Modifier.width(2.dp))
                                        Text("›", color = Color(0xFFBB86FC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Barra de input ────────────────────────────────────────────────────
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
                        when {
                            state.isListening -> vm.stopListening()
                            state.pipelineState == PipelineState.SPEAKING -> vm.stopSpeaking()
                            else -> vm.startListening()
                        }
                    },
                    enabled = state.pipelineState != PipelineState.PROCESSING,
                    colors  = IconButtonDefaults.filledIconButtonColors(
                        containerColor = when {
                            state.isListening -> Color(0xFF1F3A1F)
                            state.pipelineState == PipelineState.SPEAKING -> Color(0xFF3A1A1A)
                            isFlow -> Color(0xFF4A2A7A)
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
                Spacer(Modifier.width(4.dp))
                FilledIconButton(
                    onClick  = {
                        val t = input.trim()
                        if (t.isNotBlank()) { onInputChange(""); vm.sendMessage(t, voiceEnabled) }
                    },
                    enabled  = input.isNotBlank() && state.pipelineState != PipelineState.PROCESSING,
                    colors   = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isFlow) Color(0xFF6200EE) else Purple
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar", tint = OnPurple)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MODO CONDUCCIÓN  (fusionado, reemplaza AutoModeScreen)
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriveModeContent(
    vm: MainViewModel,
    state: MainUiState,
    onExit: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {

        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF6200EE)) {
                        Text("🚗", fontSize = 14.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(text = "MODO CONDUCCIÓN", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF6200EE), letterSpacing = 2.sp)
                        Text("Interfaz vehicular activa", fontSize = 10.sp, color = Color(0xFF9E9E9E))
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0D0D)),
            actions = {
                TextButton(onClick = onExit, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Text("SALIR", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        )

        when (selectedTab) {
            0 -> DriveMainPanel(vm)
            1 -> DriveNotificationsPanel(state)
            2 -> DriveMusicPanel()
            3 -> DriveShortcutsPanel(vm)
        }

        NavigationBar(containerColor = Color(0xFF0D0D0D), modifier = Modifier.fillMaxWidth()) {
            listOf(
                Triple(Icons.Default.Dashboard, "Panel", 0),
                Triple(Icons.Default.Notifications, "Notif.", 1),
                Triple(Icons.Default.MusicNote, "Música", 2),
                Triple(Icons.Default.Apps, "Accesos", 3)
            ).forEach { (icon, label, idx) ->
                NavigationBarItem(
                    icon     = { Icon(icon, null) },
                    label    = { Text(label, fontSize = 10.sp) },
                    selected = selectedTab == idx,
                    onClick  = { selectedTab = idx },
                    colors   = NavigationBarItemDefaults.colors(
                        selectedIconColor   = Color(0xFF6200EE),
                        selectedTextColor   = Color(0xFF6200EE),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    }
}

@Composable
private fun DriveMainPanel(vm: MainViewModel) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        val now = remember { mutableStateOf(LocalDateTime.now()) }
        LaunchedEffect(Unit) {
            while (true) { now.value = LocalDateTime.now(); delay(1000) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(now.value.format(DateTimeFormatter.ofPattern("HH:mm")),
                fontSize = 72.sp, fontWeight = FontWeight.Bold,
                color = Color.White, textAlign = TextAlign.Center)
            Text(now.value.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                fontSize = 14.sp, color = Color.Gray,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp)).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DriveIndicator("WiFi",      Icons.Default.Wifi)
            DriveIndicator("Bluetooth", Icons.Default.Bluetooth)
            DriveIndicator("Batería",   Icons.Default.BatteryFull)
            DriveIndicator("Volumen",   Icons.Default.VolumeUp)
        }
        Column(
            Modifier.fillMaxWidth().background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Controles Rápidos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DriveQuickBtn("WiFi",      Icons.Default.Wifi,      Modifier.weight(1f))
                DriveQuickBtn("Bluetooth", Icons.Default.Bluetooth, Modifier.weight(1f))
                DriveQuickBtn("Silencio",  Icons.Default.VolumeOff, Modifier.weight(1f))
            }
        }
        Surface(
            Modifier.fillMaxWidth(),
            color = Color(0xFF2A2A2A), shape = RoundedCornerShape(12.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = Color(0xFF6200EE), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Próxima alarma", color = Color.Gray, fontSize = 12.sp)
                    Text("Mañana a las 07:00", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun DriveNotificationsPanel(state: MainUiState) {
    val messages = state.messages.takeLast(10)
    if (messages.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sin notificaciones recientes", color = Color.Gray, fontSize = 14.sp)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(messages) { msg: ChatMessage ->
            Surface(Modifier.fillMaxWidth(), color = Color(0xFF2A2A2A), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(40.dp), color = Color(0xFF6200EE), shape = CircleShape) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(if (msg.role == "user") "Tú" else "🤖", color = Color.White,
                                fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(msg.text.take(80) + if (msg.text.length > 80) "…" else "",
                        fontSize = 13.sp, color = Color.White, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DriveMusicPanel() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(Modifier.size(200.dp), color = Color(0xFF6200EE), shape = CircleShape) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.size(80.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Canción en Reproducción", fontSize = 18.sp, fontWeight = FontWeight.Bold,
            color = Color.White, textAlign = TextAlign.Center)
        Text("Artista Desconocido", fontSize = 14.sp, color = Color.Gray,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            IconButton(
                onClick = {},
                modifier = Modifier.size(64.dp).background(Color(0xFF6200EE), CircleShape)
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth()) {
            LinearProgressIndicator(progress = 0.35f,
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color(0xFF6200EE), trackColor = Color(0xFF2A2A2A))
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1:45", fontSize = 10.sp, color = Color.Gray)
                Text("5:00", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun DriveShortcutsPanel(vm: MainViewModel) {
    val shortcuts = listOf(
        Triple(Icons.Default.Phone,         "Llamar a Casa",          {}),
        Triple(Icons.Default.Chat,          "Enviar WhatsApp",        {}),
        Triple(Icons.Default.MusicNote,     "Abrir Spotify",          {}),
        Triple(Icons.Default.Navigation,    "Navegar al Trabajo",     {}),
        Triple(Icons.Default.Email,         "Enviar Email",           {}),
        Triple(Icons.Default.Podcasts,      "Reproducir Podcast",     {}),
        Triple(Icons.Default.Wifi,          "Activar WiFi",           {}),
        Triple(Icons.Default.Notifications, "Leer Notificaciones",    {})
    )
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(shortcuts) { shortcut ->
            val icon   = shortcut.first
            val label  = shortcut.second
            val action = shortcut.third
            Button(
                onClick = action,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start) {
                    Icon(icon, null, tint = Color(0xFF6200EE), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(label, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ── Helpers compartidos ───────────────────────────────────────────────────────

@Composable
private fun DriveIndicator(label: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
        Icon(icon, null, tint = Color(0xFF6200EE), modifier = Modifier.size(24.dp))
        Text(label, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun DriveQuickBtn(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Button(
        onClick = {},
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A3A))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(label, fontSize = 9.sp, color = Color.White, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage, isDark: Boolean = false) {
    val isUser = msg.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        if (!isUser) Text("🤖 ", fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp))
        Surface(
            shape = RoundedCornerShape(
                topStart    = if (isUser) 16.dp else 4.dp,
                topEnd      = if (isUser) 4.dp else 16.dp,
                bottomStart = 16.dp, bottomEnd = 16.dp
            ),
            color = when {
                isUser && isDark  -> Color(0xFF2A0A4A)
                isUser            -> Color(0xFFEADDFF)
                isDark            -> Color(0xFF1D1027)
                else              -> Surface1Light
            },
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            if (isUser) {
                Text(msg.text, Modifier.padding(12.dp),
                    color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D),
                    fontSize = 14.sp, lineHeight = 20.sp)
            } else {
                MarkdownText(text = msg.text,
                    color = if (isDark) Color(0xFFEAE0F8) else Label1Light,
                    fontSize = 14.sp, lineHeight = 20.sp,
                    modifier = Modifier.padding(12.dp))
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
    var message by remember { mutableStateOf(FriendlyMessagesProvider.getWaitingMessage()) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Text("🤖 ", fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp))
        Surface(shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp), color = Surface1Light) {
            Text(message, Modifier.padding(12.dp), color = Label2Light, fontSize = 14.sp, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
private fun EmptyState(isFlowMode: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (isFlowMode) {
            Text("⚡", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text("Acciones rápidas", fontWeight = FontWeight.Bold, color = Color(0xFFBB86FC), fontSize = 18.sp)
            Text("Toca un botón para ejecutar al instante.", color = Color(0xFF9489A4), fontSize = 14.sp, textAlign = TextAlign.Center)
            Text("Sin internet, sin esperas.", color = Color(0xFF6B5E7E), fontSize = 12.sp, textAlign = TextAlign.Center)
        } else {
            Text("🤖", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text("¡Hola, soy Doey!", fontWeight = FontWeight.Bold, color = Label1Light, fontSize = 18.sp)
            Text("Tu asistente de IA personal.", color = Label2Light, fontSize = 14.sp)
            Text("Toca el micrófono o escribe para comenzar.", color = Label3Light, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}
