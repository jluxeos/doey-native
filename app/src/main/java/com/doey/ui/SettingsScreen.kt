package com.doey.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.services.FriendlyModeService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: MainViewModel, onProfileChanged: () -> Unit = {}) {
    val ctx = LocalContext.current
    val settings = remember { vm.getSettings() }
    val scope = rememberCoroutineScope()
    val uiState by vm.uiState.collectAsState()
    val scheme = MaterialTheme.colorScheme

    var provider by remember { mutableStateOf("gemini") }
    var apiKey by remember { mutableStateOf("") }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var model by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }
    var theme by remember { mutableStateOf("tau") }
    var bubblePosition by remember { mutableStateOf("right") }
    var overlayEnabled by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf("system") }
    var wakePhrase by remember { mutableStateOf("hey doey") }
    var wakeWordEnabled by remember { mutableStateOf(false) }
    var drivingMode by remember { mutableStateOf(false) }
    var sttMode by remember { mutableStateOf("auto") }
    var maxIterations by remember { mutableStateOf(10) }
    var maxHistory by remember { mutableStateOf(20) }
    var expertMode by remember { mutableStateOf(false) }
    var tokenOptimizer by remember { mutableStateOf(true) }
    var promptCache by remember { mutableStateOf(true) }
    var historyCompress by remember { mutableStateOf(true) }
    var debugMode by remember { mutableStateOf(false) }
    var notifEnabled by remember { mutableStateOf(false) }
    var friendlyEnabled by remember { mutableStateOf(true) }
    var friendlyContextRead by remember { mutableStateOf(true) }
    var autoStartFriendly by remember { mutableStateOf(false) }
    var friendlyBarHeight by remember { mutableStateOf(72f) }
    var friendlyBarOpacity by remember { mutableStateOf(0.95f) }
    var soul by remember { mutableStateOf("") }
    var personalMemory by remember { mutableStateOf("") }

    var showVoice by remember { mutableStateOf(true) }
    var showSystem by remember { mutableStateOf(true) }
    var showFriendly by remember { mutableStateOf(false) }
    var showOptimization by remember { mutableStateOf(false) }
    var showHidden by remember { mutableStateOf(true) }
    var showSaved by remember { mutableStateOf(false) }
    var showApplied by remember { mutableStateOf(false) }

    val providers = listOf("gemini", "groq", "openrouter", "openai", "custom")
    val themeOptions = listOf(
        ThemeOption("tau", "Cristal neutro", Color(0xFF8B7BFF)),
        ThemeOption("blue", "Azul vidrio", Color(0xFF5AC8FA)),
        ThemeOption("green", "Verde vidrio", Color(0xFF32D74B)),
        ThemeOption("orange", "Ámbar vidrio", Color(0xFFFF9F0A)),
        ThemeOption("red", "Rojo vidrio", Color(0xFFFF453A))
    )
    val languages = listOf(
        "system" to "Sistema",
        "es-MX" to "Español MX",
        "es-ES" to "Español ES",
        "en-US" to "English US",
        "pt-BR" to "Português BR"
    )
    val sttModes = listOf(
        "auto" to "Auto",
        "fast" to "Rápido",
        "precise" to "Preciso"
    )

    LaunchedEffect(Unit) {
        provider = settings.getProvider()
        apiKey = settings.getApiKey(provider)
        model = settings.getModel()
        customUrl = settings.getCustomModelUrl()
        theme = settings.getTheme()
        bubblePosition = settings.getBubblePosition()
        overlayEnabled = settings.getOverlayEnabled()
        language = settings.getLanguage()
        wakePhrase = settings.getWakePhrase()
        wakeWordEnabled = settings.getWakeWordEnabled()
        drivingMode = settings.getDrivingMode()
        sttMode = settings.getSttMode()
        maxIterations = settings.getMaxIterations()
        maxHistory = settings.getMaxHistoryMessages()
        expertMode = settings.getExpertMode()
        tokenOptimizer = settings.getTokenOptimizerEnabled()
        promptCache = settings.getSystemPromptCacheEnabled()
        historyCompress = settings.getHistoryCompressionEnabled()
        debugMode = settings.getDebugMode()
        notifEnabled = settings.getNotifEnabled()
        friendlyEnabled = settings.getFriendlyModeEnabled()
        friendlyContextRead = settings.getFriendlyContextRead()
        autoStartFriendly = settings.getAutoStartFriendly()
        friendlyBarHeight = settings.getFriendlyBarHeight()
        friendlyBarOpacity = settings.getFriendlyBarOpacity()
        soul = settings.getSoul()
        personalMemory = settings.getPersonalMemory()
    }

    LaunchedEffect(provider) {
        apiKey = settings.getApiKey(provider)
    }

    fun syncFriendlyService() {
        if (friendlyEnabled) {
            val intent = Intent(ctx, FriendlyModeService::class.java).apply {
                action = FriendlyModeService.ACTION_SHOW
                putExtra(FriendlyModeService.EXTRA_CONTEXT_APP, "Ajustes")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(intent)
            else ctx.startService(intent)
        } else {
            ctx.stopService(Intent(ctx, FriendlyModeService::class.java))
        }
    }

    suspend fun persistCommonState() {
        settings.setApiKey(provider, apiKey.trim())
        settings.setProvider(provider)
        settings.setModel(model.trim())
        settings.setCustomModelUrl(customUrl.trim())
        settings.setTheme(theme)
        settings.setBubblePosition(bubblePosition)
        settings.setLanguage(language)
        settings.setWakePhrase(wakePhrase.trim())
        settings.setSttMode(sttMode)
        settings.setMaxIterations(maxIterations)
        settings.setMaxHistoryMessages(maxHistory)
        settings.setExpertMode(expertMode)
        settings.setTokenOptimizerEnabled(tokenOptimizer)
        settings.setSystemPromptCacheEnabled(promptCache)
        settings.setHistoryCompressionEnabled(historyCompress)
        settings.setDebugMode(debugMode)
        settings.setNotifEnabled(notifEnabled)
        settings.setFriendlyModeEnabled(friendlyEnabled)
        settings.setFriendlyContextRead(friendlyContextRead)
        settings.setAutoStartFriendly(autoStartFriendly)
        settings.setFriendlyBarHeight(friendlyBarHeight)
        settings.setFriendlyBarOpacity(friendlyBarOpacity)
        settings.setSoul(soul.trim())
        settings.setPersonalMemory(personalMemory.trim())
        vm.toggleOverlay(overlayEnabled)
        if (drivingMode != uiState.isDrivingMode) vm.toggleDrivingMode()
        if (wakeWordEnabled != uiState.isWakeWordActive) vm.toggleWakeWord()
        syncFriendlyService()
        onProfileChanged()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0B1020),
                        scheme.primary.copy(alpha = 0.18f),
                        scheme.secondary.copy(alpha = 0.08f),
                        Color(0xFF05070D)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassHeroCard(
                title = "Ajustes Glass",
                subtitle = "Ahora se muestran configuraciones que estaban persistidas pero dispersas u ocultas, incluyendo wake word, modo conducción, personalidad, memoria y modo Friendly.",
                accent = scheme.primary
            )

            GlassSettingsSection(
                title = "Motor IA",
                icon = Icons.Default.Psychology,
                accent = scheme.primary
            ) {
                Text(
                    text = "Proveedor, modelo y credenciales con apariencia tipo vidrio y acento variable.",
                    color = scheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                ProviderRow(providers = providers, selected = provider, accent = scheme.primary) { provider = it }
                GlassTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = "API Key",
                    placeholder = "Pega tu clave aquí",
                    accent = scheme.primary,
                    singleLine = true,
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(
                                if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = scheme.onSurfaceVariant
                            )
                        }
                    }
                )
                GlassTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = "Modelo",
                    placeholder = "Modelo por defecto o personalizado",
                    accent = scheme.primary,
                    singleLine = true,
                    leading = { Icon(Icons.Default.SmartToy, null, tint = scheme.onSurfaceVariant) }
                )
                AnimatedVisibility(visible = provider == "custom") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Spacer(Modifier.height(4.dp))
                        GlassTextField(
                            value = customUrl,
                            onValueChange = { customUrl = it },
                            label = "URL compatible con OpenAI",
                            placeholder = "https://tu-servidor.com/v1/chat/completions",
                            accent = scheme.primary,
                            singleLine = true,
                            leading = { Icon(Icons.Default.Cloud, null, tint = scheme.onSurfaceVariant) }
                        )
                    }
                }
            }

            GlassSettingsSection(
                title = "Acento y apariencia",
                icon = Icons.Default.Palette,
                accent = scheme.primary
            ) {
                Text(
                    text = "Se reemplazó la sensación Material rígida por una base neutra y translúcida con acento configurable.",
                    color = scheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                ThemeChipRow(options = themeOptions, selected = theme, accent = scheme.primary) { theme = it }
                GlassSegmentedRow(
                    title = "Burbuja flotante",
                    values = listOf("left" to "Izquierda", "right" to "Derecha"),
                    selected = bubblePosition,
                    accent = scheme.primary
                ) { bubblePosition = it }
                GlassToggleRow(
                    title = "Overlay flotante",
                    subtitle = if (overlayEnabled) "La burbuja puede mostrarse sobre otras apps" else "Desactivado",
                    icon = Icons.Default.BubbleChart,
                    checked = overlayEnabled,
                    accent = scheme.primary
                ) { overlayEnabled = it }
                AnimatedVisibility(visible = overlayEnabled && !android.provider.Settings.canDrawOverlays(ctx)) {
                    GlassInfoCard(
                        text = "Falta el permiso de superposición. Toca aquí para abrir el panel del sistema.",
                        accent = Color(0xFFFF9F0A),
                        modifier = Modifier.clickable {
                            val intent = Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${ctx.packageName}")
                            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            ctx.startActivity(intent)
                        }
                    )
                }
            }

            GlassSettingsSection(
                title = "Voz y contexto del sistema",
                icon = Icons.Default.RecordVoiceOver,
                accent = scheme.primary
            ) {
                GlassExpandableHeader(
                    title = "Mostrar controles de voz, wake word y conducción",
                    expanded = showVoice,
                    accent = scheme.primary
                ) { showVoice = !showVoice }
                AnimatedVisibility(visible = showVoice) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Spacer(Modifier.height(4.dp))
                        GlassSegmentedRow(
                            title = "Idioma",
                            values = languages,
                            selected = language,
                            accent = scheme.primary
                        ) { language = it }
                        GlassTextField(
                            value = wakePhrase,
                            onValueChange = { wakePhrase = it },
                            label = "Frase de activación",
                            placeholder = "hey doey",
                            accent = scheme.primary,
                            singleLine = true,
                            leading = { Icon(Icons.Default.Mic, null, tint = scheme.onSurfaceVariant) }
                        )
                        GlassSegmentedRow(
                            title = "Reconocimiento de voz",
                            values = sttModes,
                            selected = sttMode,
                            accent = scheme.primary
                        ) { sttMode = it }
                        GlassToggleRow(
                            title = "Wake word activa",
                            subtitle = "Antes estaba fuera de la UI principal; ahora se puede activar o desactivar desde aquí.",
                            icon = Icons.Default.SettingsVoice,
                            checked = wakeWordEnabled,
                            accent = scheme.primary
                        ) { wakeWordEnabled = it }
                        GlassToggleRow(
                            title = "Modo conducción",
                            subtitle = "También estaba persistido pero no expuesto en ajustes.",
                            icon = Icons.Default.DirectionsCar,
                            checked = drivingMode,
                            accent = scheme.primary
                        ) { drivingMode = it }
                    }
                }
            }

            GlassSettingsSection(
                title = "Configuración oculta del agente",
                icon = Icons.Default.TipsAndUpdates,
                accent = scheme.primary
            ) {
                GlassExpandableHeader(
                    title = "Mostrar personalidad, memoria y controles internos",
                    expanded = showHidden,
                    accent = scheme.primary
                ) { showHidden = !showHidden }
                AnimatedVisibility(visible = showHidden) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Spacer(Modifier.height(4.dp))
                        GlassTextField(
                            value = soul,
                            onValueChange = { soul = it },
                            label = "Personalidad / soul",
                            placeholder = "Describe el tono, rol o estilo interno del asistente",
                            accent = scheme.primary,
                            minLines = 4
                        )
                        GlassTextField(
                            value = personalMemory,
                            onValueChange = { personalMemory = it },
                            label = "Memoria personal",
                            placeholder = "Información persistente del usuario que el asistente debe recordar",
                            accent = scheme.primary,
                            minLines = 5
                        )
                        GlassInfoCard(
                            text = "Estos dos campos sí existían en almacenamiento y en el pipeline, pero no se mostraban desde la UI principal de ajustes.",
                            accent = scheme.primary
                        )
                    }
                }
            }

            GlassSettingsSection(
                title = "Modo Friendly y barras flotantes",
                icon = Icons.Default.Spa,
                accent = scheme.primary
            ) {
                GlassExpandableHeader(
                    title = "Integrado aquí para evitar ajustes repartidos en otra pantalla",
                    expanded = showFriendly,
                    accent = scheme.primary
                ) { showFriendly = !showFriendly }
                AnimatedVisibility(visible = showFriendly) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Spacer(Modifier.height(4.dp))
                        GlassToggleRow(
                            title = "Modo Friendly",
                            subtitle = "Barra asistente siempre visible",
                            icon = Icons.Default.Spa,
                            checked = friendlyEnabled,
                            accent = scheme.primary
                        ) { friendlyEnabled = it }
                        GlassToggleRow(
                            title = "Leer contexto de la app activa",
                            subtitle = "Permite que el modo Friendly use el contenido visible de la pantalla",
                            icon = Icons.Default.Visibility,
                            checked = friendlyContextRead,
                            accent = scheme.primary
                        ) { friendlyContextRead = it }
                        GlassToggleRow(
                            title = "Auto-iniciar al arrancar",
                            subtitle = "Mantiene el servicio listo desde el encendido",
                            icon = Icons.Default.AlarmOn,
                            checked = autoStartFriendly,
                            accent = scheme.primary
                        ) { autoStartFriendly = it }
                        GlassSliderRow(
                            title = "Altura de la barra",
                            value = friendlyBarHeight,
                            display = "${friendlyBarHeight.toInt()} dp",
                            valueRange = 56f..96f,
                            accent = scheme.primary
                        ) { friendlyBarHeight = it }
                        GlassSliderRow(
                            title = "Opacidad del vidrio",
                            value = friendlyBarOpacity,
                            display = "${(friendlyBarOpacity * 100).toInt()}%",
                            valueRange = 0.5f..1f,
                            accent = scheme.primary
                        ) { friendlyBarOpacity = it }
                    }
                }
            }

            GlassSettingsSection(
                title = "Optimización y depuración",
                icon = Icons.Default.Speed,
                accent = scheme.primary
            ) {
                GlassExpandableHeader(
                    title = "Mostrar consumo de tokens, límites y switches avanzados",
                    expanded = showOptimization,
                    accent = scheme.primary
                ) { showOptimization = !showOptimization }
                AnimatedVisibility(visible = showOptimization) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Spacer(Modifier.height(4.dp))
                        GlassToggleRow(
                            title = "Modo experto",
                            subtitle = "Muestra detalles técnicos y comportamiento más abierto",
                            icon = Icons.Default.Code,
                            checked = expertMode,
                            accent = scheme.primary
                        ) { expertMode = it }
                        GlassToggleRow(
                            title = "Optimizador de tokens",
                            subtitle = "Clasifica tareas para reducir coste en mensajes simples",
                            icon = Icons.Default.Speed,
                            checked = tokenOptimizer,
                            accent = scheme.primary
                        ) { tokenOptimizer = it }
                        GlassToggleRow(
                            title = "Caché del prompt del sistema",
                            subtitle = "Reutiliza instrucciones persistentes y reduce consumo",
                            icon = Icons.Default.Memory,
                            checked = promptCache,
                            accent = scheme.primary
                        ) { promptCache = it }
                        GlassToggleRow(
                            title = "Compresión de historial",
                            subtitle = "Resume conversaciones antiguas para ahorrar contexto",
                            icon = Icons.Default.Compress,
                            checked = historyCompress,
                            accent = scheme.primary
                        ) { historyCompress = it }
                        GlassToggleRow(
                            title = "Procesamiento de notificaciones",
                            subtitle = "Activa la ruta interna para leer y responder notificaciones",
                            icon = Icons.Default.Notifications,
                            checked = notifEnabled,
                            accent = scheme.primary
                        ) { notifEnabled = it }
                        GlassToggleRow(
                            title = "Modo debug",
                            subtitle = "Expone logs y diagnósticos detallados",
                            icon = Icons.Default.BugReport,
                            checked = debugMode,
                            accent = scheme.primary
                        ) { debugMode = it }
                        GlassStepperRow(
                            title = "Máximo de iteraciones",
                            subtitle = "Cuántas acciones puede ejecutar el agente por mensaje",
                            value = maxIterations,
                            min = 1,
                            max = 20,
                            accent = scheme.primary,
                            onChange = { maxIterations = it }
                        )
                        GlassStepperRow(
                            title = "Mensajes en historial",
                            subtitle = "Contexto total enviado al modelo",
                            value = maxHistory,
                            min = 4,
                            max = 50,
                            step = 2,
                            accent = scheme.primary,
                            onChange = { maxHistory = it }
                        )
                    }
                }
            }

            GlassActionRow(
                primaryText = "Guardar todo",
                secondaryText = "Guardar y reiniciar IA",
                accent = scheme.primary,
                onPrimary = {
                    scope.launch {
                        persistCommonState()
                        showSaved = true
                        delay(1800)
                        showSaved = false
                    }
                },
                onSecondary = {
                    scope.launch {
                        persistCommonState()
                        vm.saveSettings(
                            provider = provider,
                            apiKey = apiKey,
                            model = model,
                            customUrl = customUrl,
                            language = language,
                            wakePhrase = wakePhrase,
                            enabledSkills = settings.getEnabledSkillsList(),
                            soul = soul,
                            personalMemory = personalMemory,
                            maxIterations = maxIterations,
                            sttMode = sttMode,
                            expertMode = expertMode
                        )
                        showApplied = true
                        delay(2200)
                        showApplied = false
                    }
                }
            )

            AnimatedVisibility(visible = showSaved) {
                GlassInfoCard(text = "Ajustes guardados con la nueva capa Glass.", accent = Color(0xFF32D74B))
            }
            AnimatedVisibility(visible = showApplied) {
                GlassInfoCard(text = "Ajustes guardados y pipeline reiniciado.", accent = Color(0xFF32D74B))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private data class ThemeOption(val id: String, val label: String, val color: Color)

@Composable
private fun GlassHeroCard(title: String, subtitle: String, accent: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun GlassSettingsSection(
    title: String,
    icon: ImageVector,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun GlassExpandableHeader(title: String, expanded: Boolean, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, modifier = Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = accent
        )
    }
}

@Composable
private fun ProviderRow(
    providers: List<String>,
    selected: String,
    accent: Color,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        providers.forEach { provider ->
            val icon = when (provider) {
                "gemini" -> Icons.Default.AutoAwesome
                "groq" -> Icons.Default.Bolt
                "openai" -> Icons.Default.SmartToy
                "custom" -> Icons.Default.Code
                else -> Icons.Default.Cloud
            }
            val isSelected = selected == provider
            Surface(
                modifier = Modifier.clickable { onSelect(provider) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) accent.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, if (isSelected) accent else Color.White.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = if (isSelected) accent else Color.White.copy(alpha = 0.75f))
                    Spacer(Modifier.width(8.dp))
                    Text(provider.replaceFirstChar { it.uppercase() }, color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ThemeChipRow(
    options: List<ThemeOption>,
    selected: String,
    accent: Color,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEach { option ->
            val active = option.id == selected
            Surface(
                modifier = Modifier.clickable { onSelect(option.id) },
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, if (active) accent else Color.White.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(option.color)
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(option.label, color = Color.White, fontSize = 12.sp)
                    if (active) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Check, null, tint = accent, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassSegmentedRow(
    title: String,
    values: List<Pair<String, String>>,
    selected: String,
    accent: Color,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { (value, label) ->
                val active = value == selected
                Surface(
                    modifier = Modifier.weight(1f).clickable { onSelect(value) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (active) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, if (active) accent else Color.White.copy(alpha = 0.10f))
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(vertical = 11.dp, horizontal = 8.dp),
                        textAlign = TextAlign.Center,
                        color = if (active) Color.White else Color.White.copy(alpha = 0.72f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    accent: Color,
    singleLine: Boolean = false,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        singleLine = singleLine,
        minLines = minLines,
        visualTransformation = visualTransformation,
        leadingIcon = leading,
        trailingIcon = trailing,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.07f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
            focusedBorderColor = accent,
            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
            focusedLabelColor = accent,
            unfocusedLabelColor = Color.White.copy(alpha = 0.60f),
            cursorColor = accent,
            focusedPlaceholderColor = Color.White.copy(alpha = 0.45f),
            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.38f)
        )
    )
}

@Composable
private fun GlassToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    accent: Color,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedBorderColor = Color.White.copy(alpha = 0.22f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
            )
        )
    }
}

@Composable
private fun GlassStepperRow(
    title: String,
    subtitle: String,
    value: Int,
    min: Int,
    max: Int,
    accent: Color,
    step: Int = 1,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp)
        }
        IconButton(onClick = { if (value > min) onChange((value - step).coerceAtLeast(min)) }) {
            Icon(Icons.Default.Remove, null, tint = Color.White.copy(alpha = 0.72f))
        }
        Text(
            value.toString(),
            color = accent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = { if (value < max) onChange((value + step).coerceAtMost(max)) }) {
            Icon(Icons.Default.Add, null, tint = Color.White.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun GlassSliderRow(
    title: String,
    value: Float,
    display: String,
    valueRange: ClosedFloatingPointRange<Float>,
    accent: Color,
    onChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(display, color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.16f)
            )
        )
    }
}

@Composable
private fun GlassActionRow(
    primaryText: String,
    secondaryText: String,
    accent: Color,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onPrimary,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.90f), contentColor = Color.White)
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(8.dp))
            Text(primaryText, fontWeight = FontWeight.Bold)
        }
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onSecondary),
            shape = RoundedCornerShape(18.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Refresh, null, tint = accent)
                Spacer(Modifier.width(8.dp))
                Text(secondaryText, color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GlassInfoCard(text: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, null, tint = accent)
            Spacer(Modifier.width(10.dp))
            Text(text, color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}
