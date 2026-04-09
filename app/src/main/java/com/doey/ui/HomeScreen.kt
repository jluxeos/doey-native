package com.doey.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.doey.agent.PipelineState
import com.doey.agent.ProfileStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel, nav: NavController) {
    val state by vm.uiState.collectAsState()
    val ctx = LocalContext.current
    val profileStore = remember { ProfileStore(ctx) }
    val isAdvanced = remember { profileStore.getUserProfile() == ProfileStore.PROFILE_ADVANCED }
    
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                isAdvanced = isAdvanced,
                isWakeWordActive = state.isWakeWordActive,
                onMenuClick = { /* Manejado por DoeyApp drawer */ },
                onNotifClick = { Toast.makeText(ctx, "Markdown consejos - próximamente", Toast.LENGTH_SHORT).show() }
            )
        },
        containerColor = TauBg
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(Modifier.weight(1f)) {
                if (state.messages.isEmpty()) {
                    EmptyChatPlaceholder(isAdvanced)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.messages) { msg ->
                            ChatBubble(msg)
                        }
                    }
                }
                
                if (state.pipelineState != PipelineState.IDLE) {
                    ProcessingIndicator(state.pipelineState, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
                }
            }

            ChatInputSection(
                input = input,
                onInputChange = { input = it },
                onSend = { 
                    if (input.isNotBlank()) {
                        vm.sendMessage(input)
                        input = ""
                    }
                },
                onMicClick = { if (state.isListening) vm.stopListening() else vm.startListening() },
                isListening = state.isListening
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    isAdvanced: Boolean,
    isWakeWordActive: Boolean,
    onMenuClick: () -> Unit,
    onNotifClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Doey", fontWeight = FontWeight.ExtraBold, color = TauText1, fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = if (isWakeWordActive) TauGreen else TauText3.copy(alpha = 0.3f),
                    modifier = Modifier.size(8.dp)
                ) {}
                if (isAdvanced) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = TauAccent.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            " NORMAL ",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TauAccentLight,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, "Menú", tint = TauText1)
            }
        },
        actions = {
            IconButton(onClick = onNotifClick) {
                BadgedBox(badge = { Badge(containerColor = TauAccent) { Text("1") } }) {
                    Icon(Icons.Default.Notifications, "Notificaciones", tint = TauText1)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = TauBg)
    )
}

@Composable
fun EmptyChatPlaceholder(isAdvanced: Boolean) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (isAdvanced) Icons.Default.Psychology else Icons.Default.SmartToy,
            null,
            modifier = Modifier.size(80.dp),
            tint = TauText3.copy(alpha = 0.2f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (isAdvanced) "Modo Avanzado Activo\nListo para tareas complejas" else "¡Hola! Soy Doey\n¿En qué puedo ayudarte hoy?",
            textAlign = TextAlign.Center,
            color = TauText3,
            fontSize = 16.sp
        )
    }
}

@Composable
fun ChatInputSection(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    isListening: Boolean
) {
    Surface(
        color = TauSurface1,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe algo...", color = TauText3) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = TauSurface2,
                    unfocusedContainerColor = TauSurface2,
                    focusedBorderColor = TauAccent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TauText1,
                    unfocusedTextColor = TauText1
                ),
                maxLines = 4
            )
            
            Spacer(Modifier.width(12.dp))
            
            val showSend = input.isNotBlank()
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isListening) TauRed else TauAccent)
                    .clickable { if (showSend) onSend() else onMicClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (showSend) Icons.Default.Send else if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) TauAccent else TauSurface2,
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = msg.text,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) Color.White else TauText1,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun ProcessingIndicator(pipelineState: PipelineState, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = TauSurface3.copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(1.dp, TauAccent.copy(alpha = 0.3f))
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = TauAccent)
            Spacer(Modifier.width(8.dp))
            Text(
                text = when(pipelineState) {
                    PipelineState.THINKING -> "Pensando..."
                    PipelineState.ACTING -> "Actuando..."
                    PipelineState.LISTENING -> "Escuchando..."
                    else -> "Procesando..."
                },
                fontSize = 11.sp,
                color = TauText2
            )
        }
    }
}
