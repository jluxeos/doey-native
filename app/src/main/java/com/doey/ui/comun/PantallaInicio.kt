package com.doey.ui.comun

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.*
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
import com.doey.ui.core.*
import com.doey.MainViewModel
import com.doey.agente.FlowModeEngine
import com.doey.agente.FlowNode
import com.doey.agente.FlowOption
import com.doey.servicios.basico.FriendlyModeService
import android.content.Intent


@Composable
fun HomeScreen(vm: MainViewModel, nav: NavController) {
    val state     by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    var input     by remember { mutableStateOf("") }
    val scope     = rememberCoroutineScope()
    val settings  = remember { vm.getSettings() }
    var theme     by remember { mutableStateOf("DeepSeaBlue") }
    val ctx       = LocalContext.current

    // ── Estado del carrusel de acciones rápidas ────────────────────────────────
    var flowExpanded      by remember { mutableStateOf(false) }
    var currentNode       by remember { mutableStateOf<FlowNode?>(null) }
    var currentParams     by remember { mutableStateOf(mapOf<String, String>()) }
    var flowFeedback      by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        theme = settings.getTheme()
        updateGlassTheme(theme)
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    // Ocultar feedback tras 2 segundos
    LaunchedEffect(flowFeedback) {
        if (flowFeedback != null) {
            delay(2000)
            flowFeedback = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        GlassBackground(accentColor = TauAccent)

        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(32.dp))

            // ── Chat ──────────────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(state.messages) { msg ->
                    val isUser = msg.role == "user"
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            GlassCard(
                                modifier = Modifier.widthIn(max = 280.dp),
                                opacity = if (isUser) GlassOpacity else GlassOpacity * 0.5f,
                                blur = GlassBlur
                            ) {
                                Text(
                                    text = msg.text,
                                    color = TauText1,
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                        if (!isUser && msg.respondedBy != null) {
                            val label = if (msg.respondedBy == "IRIS")
                                "⚡ Respondió: IRIS (sin IA)"
                            else
                                "🤖 Respondió: ${msg.respondedBy}"
                            Text(
                                text = label,
                                color = TauText3,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 3.dp)
                            )
                        }
                    }
                }
            }

            // ── Feedback de acción rápida ─────────────────────────────────────
            AnimatedVisibility(
                visible = flowFeedback != null,
                enter = fadeIn() + slideInVertically(),
                exit  = fadeOut() + slideOutVertically()
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TauAccent.copy(alpha = 0.15f))
                        .border(1.dp, TauAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = flowFeedback ?: "",
                        color = TauAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── Carrusel de acciones rápidas (Modo Flujo integrado) ────────────
            AnimatedVisibility(
                visible = flowExpanded,
                enter = fadeIn() + expandVertically(),
                exit  = fadeOut() + shrinkVertically()
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {

                    // Migas de pan + botón cerrar
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentNode != null) {
                                Text(
                                    "⚡ ",
                                    fontSize = 13.sp,
                                    color = TauText3,
                                    modifier = Modifier.clickable {
                                        currentNode = null
                                        currentParams = emptyMap()
                                    }
                                )
                            }
                            Text(
                                text = currentNode?.label ?: "Acciones rápidas",
                                color = TauText2,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            "✕",
                            color = TauText3,
                            fontSize = 16.sp,
                            modifier = Modifier.clickable {
                                flowExpanded = false
                                currentNode  = null
                                currentParams = emptyMap()
                            }
                        )
                    }

                    // Chips de opciones
                    val options = currentNode?.options ?: FlowModeEngine.getRootOptions()
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(options) { opt ->
                            FlowChip(opt) {
                                when {
                                    // Navegar a sub-nodo
                                    opt.nextNodeId != null -> {
                                        val merged = currentParams + opt.params
                                        scope.launch {
                                            currentNode   = FlowModeEngine.getNodeById(ctx, opt.nextNodeId, merged)
                                            currentParams = merged
                                        }
                                    }
                                    // Ejecutar comando directo
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

            // ── Input area ────────────────────────────────────────────────────
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).imePadding()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    opacity = GlassOpacity,
                    blur = GlassBlur
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        // Botón ⚡ para abrir/cerrar el carrusel
                        IconButton(
                            onClick = {
                                flowExpanded  = !flowExpanded
                                currentNode   = null
                                currentParams = emptyMap()
                            }
                        ) {
                            Text(
                                text = if (flowExpanded) "✕" else "⚡",
                                fontSize = 18.sp,
                                color = if (flowExpanded) TauText3 else TauAccent
                            )
                        }

                        // Botón 🌿 Modo Friendly
                        IconButton(
                            onClick = {
                                val intent = Intent(ctx, FriendlyModeService::class.java).apply {
                                    action = FriendlyModeService.ACTION_SHOW
                                    putExtra(FriendlyModeService.EXTRA_CONTEXT_APP, "Inicio")
                                }
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    ctx.startForegroundService(intent)
                                } else {
                                    ctx.startService(intent)
                                }
                            }
                        ) {
                            Text("🌿", fontSize = 18.sp)
                        }

                        TextField(
                            value = input,
                            onValueChange = { input = it },
                            placeholder = { Text("Escribe algo...", color = TauText3) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TauText1,
                                unfocusedTextColor = TauText1
                            )
                        )
                        IconButton(
                            onClick = {
                                if (input.isNotBlank()) {
                                    vm.sendMessage(input)
                                    input = ""
                                }
                            },
                            modifier = Modifier.clip(CircleShape).background(TauAccent)
                        ) {
                            Icon(CustomIcons.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Chip individual de acción rápida ──────────────────────────────────────────
@Composable
private fun FlowChip(opt: FlowOption, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(TauSurface2.copy(alpha = 0.6f))
            .border(1.dp, TauAccent.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (opt.icon.isNotBlank()) {
                Text(opt.icon, fontSize = 16.sp)
            }
            Text(
                text = opt.label,
                color = TauText1,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            if (opt.nextNodeId != null) {
                Text("›", color = TauText3, fontSize = 14.sp)
            }
        }
    }
}
