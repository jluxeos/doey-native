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
    
    // Hidden Settings (Exposed now)
    var friendlyMode     by remember { mutableStateOf(true) }
    var autoStartFriendly by remember { mutableStateOf(false) }
    var friendlyBarHeight by remember { mutableStateOf(72f) }
    var friendlyBarOpacity by remember { mutableStateOf(0.95f) }

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
        
        friendlyMode     = settings.getFriendlyModeEnabled()
        autoStartFriendly = settings.getAutoStartFriendly()
        friendlyBarHeight = settings.getFriendlyBarHeight()
        friendlyBarOpacity = settings.getFriendlyBarOpacity()
    }

    // Al cambiar proveedor, cargar su API key guardada
    LaunchedEffect(provider) {
        apiKey = settings.getApiKey(provider)
    }

    val providers = listOf("gemini", "groq", "openrouter", "openai", "custom")
    val themes    = listOf("tau", "blue", "green", "orange", "red")
    val languages = listOf(
        "system" to "Sistema (automático)",
        "es-MX"  to "Español (México)",
        "es-ES"  to "Español (España)",
        "en-US"  to "English (US)",
        "pt-BR"  to "Português (Brasil)"
    )

    Column(
        Modifier.fillMaxSize().background(TauBg).verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title  = { Text("Ajustes", fontWeight = FontWeight.Bold, color = TauText1) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1)
        )

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

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
                        color    = if (isSelected) TauAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
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
                DoeyTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = "API Key — ${provider.uppercase()}",
                    placeholder = "Pega tu API key aquí"
                )

                if (provider == "custom") {
                    DoeyTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        label = "URL Base",
                        placeholder = "https://api.tu-servidor.com/v1"
                    )
                }

                GlassButton(onClick = {
                    scope.launch {
                        settings.setApiKey(provider, apiKey)
                        if (provider == "custom") settings.setCustomModelUrl(customUrl)
                        showApiSaved = true
                        delay(2000)
                        showApiSaved = false
                    }
                }) {
                    Text(if (showApiSaved) "¡GUARDADO!" else "GUARDAR API KEY")
                }
            }

            // ── 2. Optimización (Configuraciones Ocultas) ──────────────────────
            TauSettingsSection(title = "Optimización Avanzada", icon = Icons.Default.Speed) {
                TauSwitchRow(
                    title = "Optimizador de Tokens",
                    subtitle = "Reduce el costo y latencia",
                    icon = Icons.Default.Savings,
                    checked = tokenOptimizer,
                    onToggle = { tokenOptimizer = it }
                )
                TauSwitchRow(
                    title = "Caché de System Prompt",
                    subtitle = "Ahorra ~1000 tokens por mensaje",
                    icon = Icons.Default.Storage,
                    checked = promptCache,
                    onToggle = { promptCache = it }
                )
                TauSwitchRow(
                    title = "Compresión de Historial",
                    subtitle = "Resume mensajes antiguos",
                    icon = Icons.Default.Compress,
                    checked = historyCompress,
                    onToggle = { historyCompress = it }
                )
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                
                Text("Límites de Procesamiento", fontWeight = FontWeight.Bold, color = TauText2, fontSize = 13.sp)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Iteraciones Máximas: $maxIterations", modifier = Modifier.weight(1f), color = TauText1)
                    Slider(
                        value = maxIterations.toFloat(),
                        onValueChange = { maxIterations = it.toInt() },
                        valueRange = 1f..20f,
                        modifier = Modifier.width(150.dp)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Historial Máximo: $maxHistory", modifier = Modifier.weight(1f), color = TauText1)
                    Slider(
                        value = maxHistory.toFloat(),
                        onValueChange = { maxHistory = it.toInt() },
                        valueRange = 5f..50f,
                        modifier = Modifier.width(150.dp)
                    )
                }
            }

            // ── 3. Interfaz Friendly (Configuraciones Ocultas) ──────────────────
            TauSettingsSection(title = "Modo Friendly (Oculto)", icon = Icons.Default.Spa) {
                TauSwitchRow(
                    title = "Habilitar Modo Friendly",
                    subtitle = "Mensajes de compañía mientras procesa",
                    icon = Icons.Default.ChatBubbleOutline,
                    checked = friendlyMode,
                    onToggle = { friendlyMode = it }
                )
                TauSwitchRow(
                    title = "Auto-inicio Friendly",
                    subtitle = "Inicia automáticamente al abrir",
                    icon = Icons.Default.AutoMode,
                    checked = autoStartFriendly,
                    onToggle = { autoStartFriendly = it }
                )
                
                Text("Altura de Barra: ${friendlyBarHeight.toInt()}dp", color = TauText1)
                Slider(
                    value = friendlyBarHeight,
                    onValueChange = { friendlyBarHeight = it },
                    valueRange = 40f..120f
                )
                
                Text("Opacidad: ${(friendlyBarOpacity * 100).toInt()}%", color = TauText1)
                Slider(
                    value = friendlyBarOpacity,
                    onValueChange = { friendlyBarOpacity = it },
                    valueRange = 0.5f..1.0f
                )
            }

            // ── 4. Apariencia ─────────────────────────────────────────────────
            TauSettingsSection(title = "Apariencia", icon = Icons.Default.Palette) {
                Text("Tema de Color", fontWeight = FontWeight.Bold, color = TauText1)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    themes.forEach { t ->
                        val isSelected = theme == t
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .background(when(t){
                                    "blue" -> TauBlue
                                    "green" -> TauGreen
                                    "orange" -> TauOrange
                                    "red" -> TauRed
                                    else -> TauAccent
                                })
                                .clickable { theme = t }
                                .border(if(isSelected) 3.dp else 0.dp, Color.White, CircleShape)
                        )
                    }
                }
                
                TauSwitchRow(
                    title = "Modo Experto",
                    subtitle = "Muestra más opciones en el menú",
                    icon = Icons.Default.Star,
                    checked = expertMode,
                    onToggle = { expertMode = it }
                )
                
                TauSwitchRow(
                    title = "Modo Debug",
                    subtitle = "Muestra logs técnicos",
                    icon = Icons.Default.BugReport,
                    checked = debugMode,
                    onToggle = { debugMode = it }
                )
            }

            // ── Botón Guardar Todo ────────────────────────────────────────────
            GlassButton(
                onClick = {
                    scope.launch {
                        vm.saveSettings(
                            provider, apiKey, model, customUrl, language, wakePhrase,
                            settings.getEnabledSkillsList(), settings.getSoul(),
                            settings.getPersonalMemory(), maxIterations, sttMode, expertMode
                        )
                        settings.setTheme(theme)
                        settings.setTokenOptimizerEnabled(tokenOptimizer)
                        settings.setSystemPromptCacheEnabled(promptCache)
                        settings.setHistoryCompressionEnabled(historyCompress)
                        settings.setDebugMode(debugMode)
                        settings.setFriendlyModeEnabled(friendlyMode)
                        settings.setAutoStartFriendly(autoStartFriendly)
                        settings.setFriendlyBarHeight(friendlyBarHeight)
                        settings.setFriendlyBarOpacity(friendlyBarOpacity)
                        
                        showSettingsSaved = true
                        onProfileChanged()
                        delay(2000)
                        showSettingsSaved = false
                    }
                },
                containerColor = TauAccentGlow
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text(if (showSettingsSaved) "¡AJUSTES APLICADOS!" else "APLICAR TODOS LOS CAMBIOS")
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
