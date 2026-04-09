package com.doey.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onProfileChanged: () -> Unit = {}) {
    val ctx      = LocalContext.current
    val settings = remember { vm.getSettings() }
    val scope    = rememberCoroutineScope()

    // ── Estado de todos los ajustes ───────────────────────────────────────────
    var provider         by remember { mutableStateOf("gemini") }
    var apiKey           by remember { mutableStateOf("") }
    var apiKeyVisible    by remember { mutableStateOf(false) }
    var model            by remember { mutableStateOf("") }
    var customUrl        by remember { mutableStateOf("") }
    var theme            by remember { mutableStateOf("tau") }
    var bubblePosition   by remember { mutableStateOf("right") }
    var overlayEnabled   by remember { mutableStateOf(false) }
    var language         by remember { mutableStateOf("system") }
    var wakePhrase       by remember { mutableStateOf("hey doey") }
    var sttMode          by remember { mutableStateOf("auto") }
    var maxIterations    by remember { mutableStateOf(10) }
    var maxHistory       by remember { mutableStateOf(20) }
    var expertMode       by remember { mutableStateOf(false) }
    var tokenOptimizer   by remember { mutableStateOf(true) }
    var promptCache      by remember { mutableStateOf(true) }
    var historyCompress  by remember { mutableStateOf(true) }
    var debugMode        by remember { mutableStateOf(false) }
    var notifEnabled     by remember { mutableStateOf(false) }
    var showApiSaved     by remember { mutableStateOf(false) }
    var showSettingsSaved by remember { mutableStateOf(false) }
    var showAdvanced     by remember { mutableStateOf(false) }
    var showTokenSection by remember { mutableStateOf(false) }
    var showVoiceSection by remember { mutableStateOf(false) }

    // Cargar todos los ajustes al iniciar
    LaunchedEffect(Unit) {
        provider        = settings.getProvider()
        apiKey          = settings.getApiKey(provider)
        model           = settings.getModel()
        customUrl       = settings.getCustomModelUrl()
        theme           = settings.getTheme()
        bubblePosition  = settings.getBubblePosition()
        overlayEnabled  = settings.getOverlayEnabled()
        language        = settings.getLanguage()
        wakePhrase      = settings.getWakePhrase()
        sttMode         = settings.getSttMode()
        maxIterations   = settings.getMaxIterations()
        maxHistory      = settings.getMaxHistoryMessages()
        expertMode      = settings.getExpertMode()
        tokenOptimizer  = settings.getTokenOptimizerEnabled()
        promptCache     = settings.getSystemPromptCacheEnabled()
        historyCompress = settings.getHistoryCompressionEnabled()
        debugMode       = settings.getDebugMode()
        notifEnabled    = settings.getNotifEnabled()
    }

    // Al cambiar proveedor, cargar su API key guardada
    LaunchedEffect(provider) {
        apiKey = settings.getApiKey(provider)
    }

    val providers = listOf("gemini", "groq", "openrouter", "openai", "custom")
    val themes    = listOf("tau", "blue", "green", "orange", "red")
    val themeColors = mapOf(
        "tau"    to TauAccent,
        "blue"   to TauBlue,
        "green"  to TauGreen,
        "orange" to TauOrange,
        "red"    to TauRed
    )
    val languages = listOf(
        "system" to "Sistema (automático)",
        "es-MX"  to "Español (México)",
        "es-ES"  to "Español (España)",
        "en-US"  to "English (US)",
        "pt-BR"  to "Português (Brasil)"
    )
    val sttModes = listOf(
        "auto"   to "Automático",
        "fast"   to "Rápido (menos preciso)",
        "precise" to "Preciso (más lento)"
    )

    Column(
        Modifier.fillMaxSize().background(TauBg).verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title  = { Text("Ajustes", fontWeight = FontWeight.Bold, color = TauText1) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1)
        )

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── 1. Cerebro de Doey ────────────────────────────────────────────
            TauSettingsSection(title = "Cerebro de Doey", icon = Icons.Default.Psychology) {
                Text(
                    "Selecciona el proveedor de IA y proporciona tu API key.",
                    fontSize = 12.sp, color = TauText3
                )

                providers.forEach { p ->
                    val isSelected = provider == p
                    Surface(
                        onClick = {
                            provider = p
                            scope.launch { settings.setProvider(p) }
                        },
                        shape    = RoundedCornerShape(12.dp),
                        color    = if (isSelected) TauAccent.copy(alpha = 0.1f) else TauSurface2,
                        border   = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, TauAccent) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                when (p) {
                                    "gemini"    -> Icons.Default.AutoAwesome
                                    "groq"      -> Icons.Default.Bolt
                                    "openai"    -> Icons.Default.SmartToy
                                    "custom"    -> Icons.Default.Build
                                    else        -> Icons.Default.Cloud
                                },
                                null, tint = if (isSelected) TauAccent else TauText3
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    p.replaceFirstChar { it.uppercase() },
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) TauAccent else TauText1
                                )
                                Text(
                                    when (p) {
                                        "gemini"    -> "2.5 Flash · Recomendado"
                                        "groq"      -> "Llama 3 · Ultra rápido"
                                        "openai"    -> "GPT-4o · OpenAI"
                                        "custom"    -> "URL personalizada"
                                        else        -> "OpenRouter · Modelos gratuitos"
                                    },
                                    fontSize = 11.sp, color = TauText3
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "API Key — ${provider.replaceFirstChar { it.uppercase() }}",
                    fontSize = 12.sp, color = TauText3
                )
                OutlinedTextField(
                    value                = apiKey,
                    onValueChange        = { apiKey = it },
                    placeholder          = { Text("Pega tu API key aquí", fontSize = 12.sp) },
                    modifier             = Modifier.fillMaxWidth(),
                    shape                = RoundedCornerShape(12.dp),
                    singleLine           = true,
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None
                                          else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(
                                if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null, tint = TauText3
                            )
                        }
                    },
                    colors = doeyFieldColors()
                )

                // ── Modelo de IA (antes oculto) ───────────────────────────────
                Text("Modelo de IA", fontSize = 12.sp, color = TauText3)
                OutlinedTextField(
                    value         = model,
                    onValueChange = { model = it },
                    placeholder   = {
                        Text(
                            when (provider) {
                                "gemini"    -> "gemini-2.5-flash (por defecto)"
                                "groq"      -> "llama-3.1-70b-versatile (por defecto)"
                                "openai"    -> "gpt-4o (por defecto)"
                                "openrouter"-> "openrouter/auto (por defecto)"
                                else        -> "Nombre del modelo"
                            },
                            fontSize = 11.sp
                        )
                    },
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors    = doeyFieldColors()
                )

                // ── URL personalizada (solo para proveedor "custom") ──────────
                AnimatedVisibility(visible = provider == "custom") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("URL del servidor (OpenAI-compatible)", fontSize = 12.sp, color = TauText3)
                        OutlinedTextField(
                            value         = customUrl,
                            onValueChange = { customUrl = it },
                            placeholder   = { Text("https://tu-servidor.com/v1/chat/completions", fontSize = 11.sp) },
                            modifier      = Modifier.fillMaxWidth(),
                            shape         = RoundedCornerShape(12.dp),
                            singleLine    = true,
                            colors        = doeyFieldColors()
                        )
                    }
                }

                Button(
                    onClick = {
                        settings.setApiKey(provider, apiKey)
                        scope.launch {
                            settings.setProvider(provider)
                            settings.setModel(model)
                            settings.setCustomModelUrl(customUrl)
                            showApiSaved = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = TauAccent)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar Proveedor y Modelo", fontWeight = FontWeight.Bold)
                }

                // Botón de prueba de conexión
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            // Guardar primero
                            settings.setApiKey(provider, apiKey)
                            settings.setProvider(provider)
                            settings.setModel(model)
                            // Reiniciar pipeline con nuevos ajustes
                            vm.saveSettings(
                                provider = provider, apiKey = apiKey, model = model,
                                customUrl = customUrl, language = language,
                                wakePhrase = wakePhrase, enabledSkills = emptyList(),
                                soul = "", personalMemory = "", maxIterations = maxIterations,
                                sttMode = sttMode, expertMode = expertMode
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = TauAccentLight)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Aplicar y reiniciar IA")
                }

                AnimatedVisibility(visible = showApiSaved) {
                    LaunchedEffect(Unit) { delay(2000); showApiSaved = false }
                    Surface(
                        shape    = RoundedCornerShape(8.dp),
                        color    = TauGreen.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = TauGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("¡API Key y modelo guardados!", color = TauGreen, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── 2. Voz e Idioma (antes oculto) ───────────────────────────────
            TauSettingsSection(title = "Voz e Idioma", icon = Icons.Default.RecordVoiceOver) {

                // Encabezado expandible
                Row(
                    Modifier.fillMaxWidth().clickable { showVoiceSection = !showVoiceSection },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Idioma, frase de activación y reconocimiento de voz",
                        fontSize = 12.sp, color = TauText3, modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (showVoiceSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, tint = TauText3, modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = showVoiceSection) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Spacer(Modifier.height(4.dp))

                        // Idioma
                        Text("Idioma del asistente", fontSize = 12.sp, color = TauText3)
                        languages.forEach { (code, label) ->
                            val isSelected = language == code
                            Surface(
                                onClick  = {
                                    language = code
                                    scope.launch { settings.setLanguage(code) }
                                },
                                shape    = RoundedCornerShape(10.dp),
                                color    = if (isSelected) TauAccent.copy(0.1f) else TauSurface2,
                                border   = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, TauAccent) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Language, null, tint = if (isSelected) TauAccent else TauText3, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(label, color = if (isSelected) TauAccent else TauText1, fontSize = 13.sp)
                                }
                            }
                        }

                        // Frase de activación
                        Text("Frase de activación (Wake Word)", fontSize = 12.sp, color = TauText3)
                        OutlinedTextField(
                            value         = wakePhrase,
                            onValueChange = {
                                wakePhrase = it
                                scope.launch { settings.setWakePhrase(it) }
                            },
                            placeholder   = { Text("hey doey", fontSize = 12.sp) },
                            modifier      = Modifier.fillMaxWidth(),
                            shape         = RoundedCornerShape(12.dp),
                            singleLine    = true,
                            leadingIcon   = { Icon(Icons.Default.Mic, null, tint = TauText3) },
                            colors        = doeyFieldColors()
                        )

                        // Modo STT
                        Text("Modo de reconocimiento de voz", fontSize = 12.sp, color = TauText3)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sttModes.forEach { (code, label) ->
                                val isSelected = sttMode == code
                                Surface(
                                    onClick  = {
                                        sttMode = code
                                        scope.launch { settings.setSttMode(code) }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape    = RoundedCornerShape(8.dp),
                                    color    = if (isSelected) TauAccent.copy(0.2f) else TauSurface2,
                                    border   = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, TauAccent) else null
                                ) {
                                    Text(
                                        label,
                                        modifier  = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontSize  = 11.sp,
                                        color     = if (isSelected) TauAccent else TauText2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 3. Personalización ────────────────────────────────────────────
            TauSettingsSection(title = "Personalización", icon = Icons.Default.Palette) {
                Text("Tema de color del sistema", fontSize = 12.sp, color = TauText3)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    themes.forEach { t ->
                        val isSelected = theme == t
                        val color      = themeColors[t] ?: TauAccent
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable {
                                    theme = t
                                    scope.launch {
                                        settings.setTheme(t)
                                        onProfileChanged()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) Icon(
                                Icons.Default.Check, null,
                                tint = Color.White, modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text("Posición de la burbuja flotante", fontSize = 12.sp, color = TauText3)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("left", "right").forEach { pos ->
                        val isSelected = bubblePosition == pos
                        Surface(
                            onClick  = {
                                bubblePosition = pos
                                scope.launch { settings.setBubblePosition(pos) }
                            },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(8.dp),
                            color    = if (isSelected) TauAccent.copy(alpha = 0.2f) else TauSurface2,
                            border   = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, TauAccent) else null
                        ) {
                            Text(
                                if (pos == "left") "Izquierda" else "Derecha",
                                modifier  = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── 4. Burbuja flotante ───────────────────────────────────────────
            TauSettingsSection(title = "Burbuja flotante", icon = Icons.Default.BubbleChart) {
                Text(
                    "Activa la burbuja de Doey que flota sobre otras aplicaciones.",
                    fontSize = 12.sp, color = TauText3
                )
                TauSwitchRow(
                    title    = "Activar burbuja",
                    subtitle = if (overlayEnabled) "Burbuja visible" else "Burbuja desactivada",
                    icon     = Icons.Default.BubbleChart,
                    checked  = overlayEnabled,
                    onToggle = { enabled ->
                        overlayEnabled = enabled
                        vm.toggleOverlay(enabled)
                    }
                )
                AnimatedVisibility(visible = overlayEnabled && !android.provider.Settings.canDrawOverlays(ctx)) {
                    Surface(
                        shape    = RoundedCornerShape(8.dp),
                        color    = TauOrange.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${ctx.packageName}")
                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                ctx.startActivity(intent)
                            }
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = TauOrange, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Permiso de superposición requerido. Toca para concederlo.",
                                color = TauOrange, fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ── 5. Optimización de tokens (antes oculto) ─────────────────────
            TauSettingsSection(title = "Optimización de Tokens", icon = Icons.Default.TrendingDown) {

                // Encabezado expandible con resumen del estado
                Row(
                    Modifier.fillMaxWidth().clickable { showTokenSection = !showTokenSection },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Control del consumo de tokens de IA",
                            fontSize = 12.sp, color = TauText3
                        )
                        val activeCount = listOf(tokenOptimizer, promptCache, historyCompress).count { it }
                        Text(
                            "$activeCount/3 optimizaciones activas",
                            fontSize = 11.sp,
                            color = if (activeCount == 3) TauGreen else TauOrange
                        )
                    }
                    Icon(
                        if (showTokenSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, tint = TauText3, modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = showTokenSection) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Spacer(Modifier.height(4.dp))

                        // Optimizador de tokens principal
                        TauSwitchRow(
                            title    = "Optimizador de tokens",
                            subtitle = "Clasifica comandos y usa el mínimo de tokens necesario",
                            icon     = Icons.Default.TrendingDown,
                            checked  = tokenOptimizer,
                            onToggle = {
                                tokenOptimizer = it
                                scope.launch { settings.setTokenOptimizerEnabled(it) }
                            }
                        )

                        // Caché de system prompt
                        TauSwitchRow(
                            title    = "Caché de system prompt",
                            subtitle = "Reutiliza el prompt del sistema (ahorra ~500-2000 tokens)",
                            icon     = Icons.Default.Memory,
                            checked  = promptCache,
                            onToggle = {
                                promptCache = it
                                scope.launch { settings.setSystemPromptCacheEnabled(it) }
                            }
                        )

                        // Compresión de historial
                        TauSwitchRow(
                            title    = "Compresión de historial",
                            subtitle = "Resume mensajes antiguos para reducir tokens enviados",
                            icon     = Icons.Default.Compress,
                            checked  = historyCompress,
                            onToggle = {
                                historyCompress = it
                                scope.launch { settings.setHistoryCompressionEnabled(it) }
                            }
                        )

                        HorizontalDivider(color = TauSurface3)

                        // Máximo de iteraciones
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Máximo de iteraciones del agente", color = TauText1, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("Cuántas veces puede llamar herramientas por mensaje", color = TauText3, fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (maxIterations > 1) {
                                            maxIterations--
                                            scope.launch { settings.setMaxIterations(maxIterations) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Remove, null, tint = TauText3)
                                }
                                Text(
                                    "$maxIterations",
                                    color = TauAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.width(32.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                IconButton(
                                    onClick = {
                                        if (maxIterations < 20) {
                                            maxIterations++
                                            scope.launch { settings.setMaxIterations(maxIterations) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, null, tint = TauText3)
                                }
                            }
                        }

                        // Máximo de mensajes en historial
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Mensajes en historial", color = TauText1, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("Contexto de conversación enviado a la IA", color = TauText3, fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (maxHistory > 4) {
                                            maxHistory -= 2
                                            scope.launch { settings.setMaxHistoryMessages(maxHistory) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Remove, null, tint = TauText3)
                                }
                                Text(
                                    "$maxHistory",
                                    color = TauAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.width(32.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                IconButton(
                                    onClick = {
                                        if (maxHistory < 50) {
                                            maxHistory += 2
                                            scope.launch { settings.setMaxHistoryMessages(maxHistory) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, null, tint = TauText3)
                                }
                            }
                        }

                        // Tarjeta informativa
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TauAccent.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = TauAccentLight, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Con todas las optimizaciones activas, Doey puede reducir el consumo de tokens hasta un 70% en comandos simples.",
                                    color = TauText2, fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // ── 6. Ajustes avanzados (antes ocultos) ─────────────────────────
            TauSettingsSection(title = "Ajustes Avanzados", icon = Icons.Default.Tune) {

                Row(
                    Modifier.fillMaxWidth().clickable { showAdvanced = !showAdvanced },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Modo experto, notificaciones automáticas y debug",
                        fontSize = 12.sp, color = TauText3, modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, tint = TauText3, modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = showAdvanced) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Spacer(Modifier.height(4.dp))

                        // Modo experto
                        TauSwitchRow(
                            title    = "Modo Experto",
                            subtitle = "Doey muestra razonamiento interno y detalles técnicos",
                            icon     = Icons.Default.Code,
                            checked  = expertMode,
                            onToggle = {
                                expertMode = it
                                scope.launch {
                                    settings.setExpertMode(it)
                                    vm.saveSettings(
                                        provider = provider, apiKey = apiKey, model = model,
                                        customUrl = customUrl, language = language,
                                        wakePhrase = wakePhrase, enabledSkills = emptyList(),
                                        soul = "", personalMemory = "",
                                        maxIterations = maxIterations, sttMode = sttMode,
                                        expertMode = it
                                    )
                                }
                            }
                        )

                        // Notificaciones automáticas
                        TauSwitchRow(
                            title    = "Procesamiento de notificaciones",
                            subtitle = "Doey puede leer y responder notificaciones automáticamente",
                            icon     = Icons.Default.Notifications,
                            checked  = notifEnabled,
                            onToggle = {
                                notifEnabled = it
                                scope.launch { settings.setNotifEnabled(it) }
                            }
                        )

                        // Modo debug
                        TauSwitchRow(
                            title    = "Modo Debug",
                            subtitle = "Muestra logs detallados en la pantalla de Logs",
                            icon     = Icons.Default.BugReport,
                            checked  = debugMode,
                            onToggle = {
                                debugMode = it
                                scope.launch { settings.setDebugMode(it) }
                            }
                        )

                        HorizontalDivider(color = TauSurface3)

                        // Botón para guardar todos los ajustes avanzados y reiniciar pipeline
                        Button(
                            onClick = {
                                scope.launch {
                                    vm.saveSettings(
                                        provider      = provider,
                                        apiKey        = apiKey,
                                        model         = model,
                                        customUrl     = customUrl,
                                        language      = language,
                                        wakePhrase    = wakePhrase,
                                        enabledSkills = emptyList(),
                                        soul          = "",
                                        personalMemory = "",
                                        maxIterations = maxIterations,
                                        sttMode       = sttMode,
                                        expertMode    = expertMode
                                    )
                                    showSettingsSaved = true
                                    delay(2000)
                                    showSettingsSaved = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = TauBlue)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Guardar y reiniciar IA", fontWeight = FontWeight.Bold)
                        }

                        AnimatedVisibility(visible = showSettingsSaved) {
                            Surface(
                                shape    = RoundedCornerShape(8.dp),
                                color    = TauGreen.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = TauGreen, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("¡Ajustes aplicados y IA reiniciada!", color = TauGreen, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun TauSettingsSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = TauAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(title, fontWeight = FontWeight.ExtraBold, color = TauAccentLight, fontSize = 12.sp, letterSpacing = 1.sp)
        }
        Surface(
            shape    = RoundedCornerShape(16.dp),
            color    = TauSurface1,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}

@Composable
fun DoeyTextField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        placeholder   = { Text(placeholder) },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(12.dp),
        colors        = doeyFieldColors()
    )
}

@Composable
fun TauSwitchRow(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(TauSurface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = TauAccentLight, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title,    fontWeight = FontWeight.Bold, color = TauText1, fontSize = 15.sp)
            Text(subtitle, fontSize = 12.sp, color = TauText3)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(checkedThumbColor = TauAccent)
        )
    }
}
