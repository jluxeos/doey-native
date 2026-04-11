package com.doey.ui.comun

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.doey.ui.core.*
import com.doey.MainViewModel


@Composable
fun SettingsScreen(vm: MainViewModel, onProfileChanged: () -> Unit = {}) {
    val ctx      = LocalContext.current
    val settings = remember { vm.getSettings() }
    val scope    = rememberCoroutineScope()

    // ── Estado de todos los ajustes ───────────────────────────────────────────
    var provider         by remember { mutableStateOf("gemini") }
    var apiKey           by remember { mutableStateOf("") }
    var model            by remember { mutableStateOf("") }
    var customUrl        by remember { mutableStateOf("") }
    var theme            by remember { mutableStateOf("DeepSeaBlue") }
    var maxIterations    by remember { mutableStateOf(10) }
    var maxHistory       by remember { mutableStateOf(20) }
    var expertMode       by remember { mutableStateOf(false) }
    var tokenOptimizer   by remember { mutableStateOf(true) }
    var promptCache      by remember { mutableStateOf(true) }
    var historyCompress  by remember { mutableStateOf(true) }
    var debugMode        by remember { mutableStateOf(false) }
    var showApiSaved     by remember { mutableStateOf(false) }
    var showSettingsSaved by remember { mutableStateOf(false) }
    
    // Glass Controls
    var currentGlassOpacity by remember { mutableStateOf(GlassOpacity) }
    var currentGlassBlur    by remember { mutableStateOf(GlassBlur) }

    // Hidden Settings (Exposed)
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
        maxIterations   = settings.getMaxIterations()
        maxHistory      = settings.getMaxHistoryMessages()
        expertMode      = settings.getExpertMode()
        tokenOptimizer  = settings.getTokenOptimizerEnabled()
        promptCache     = settings.getSystemPromptCacheEnabled()
        historyCompress = settings.getHistoryCompressionEnabled()
        debugMode       = settings.getDebugMode()
        
        friendlyMode     = settings.getFriendlyModeEnabled()
        autoStartFriendly = settings.getAutoStartFriendly()
        friendlyBarHeight = settings.getFriendlyBarHeight()
        friendlyBarOpacity = settings.getFriendlyBarOpacity()
        currentGlassOpacity = settings.getGlassOpacity()
        currentGlassBlur = settings.getGlassBlur()
        
        updateGlassTheme(theme)
    }

    LaunchedEffect(provider) {
        apiKey = settings.getApiKey(provider)
    }

    val providers = listOf("gemini", "groq", "openrouter", "pollinations", "custom")

    Box(Modifier.fillMaxSize()) {
        GlassBackground(accentColor = TauAccent)

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            TopAppBar(
                title  = { Text("Ajustes de Doey", fontWeight = FontWeight.Bold, color = TauText1) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {

                // ── 1. Cerebro de Doey ────────────────────────────────────────────
                TauSettingsSection(title = "Cerebro de Doey", icon = CustomIcons.Psychology) {
                    providers.forEach { p ->
                        val isSelected = provider == p
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { provider = p }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (p) {
                                    "gemini"    -> CustomIcons.AutoAwesome
                                    "groq"      -> CustomIcons.Bolt
                                    "pollinations" -> CustomIcons.AutoAwesome
                                    "custom"    -> CustomIcons.Build
                                    else        -> CustomIcons.Cloud
                                },
                                null, tint = if (isSelected) TauAccent else TauText3, modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                when (p) {
                                "gemini"       -> "Gemini"
                                "groq"         -> "Groq"
                                "openrouter"   -> "OpenRouter"
                                "pollinations" -> "Pollinations (gratis)"
                                "custom"       -> "Personalizado"
                                else           -> p.replaceFirstChar { it.uppercase() }
                            },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TauAccent else TauText1,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) Icon(CustomIcons.CheckCircle, null, tint = TauAccent, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(Modifier.height(8.dp))
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

                    Spacer(Modifier.height(12.dp))
                    GlassButton(onClick = {
                        scope.launch {
                            settings.setApiKey(provider, apiKey)
                            if (provider == "custom") settings.setCustomModelUrl(customUrl)
                            showApiSaved = true
                            delay(2000)
                            showApiSaved = false
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showApiSaved) "¡GUARDADO!" else "GUARDAR API KEY", fontWeight = FontWeight.Bold)
                    }
                }

                // ── 2. Optimización Avanzada ──────────────────────────────────────
                TauSettingsSection(title = "Optimización Avanzada", icon = CustomIcons.Speed) {
                    TauSwitchRow(
                        title = "Optimizador de Tokens",
                        subtitle = "Reduce el costo y latencia",
                        icon = CustomIcons.Savings,
                        checked = tokenOptimizer,
                        onToggle = { tokenOptimizer = it }
                    )
                    TauSwitchRow(
                        title = "Caché de System Prompt",
                        subtitle = "Ahorra ~1000 tokens por mensaje",
                        icon = CustomIcons.Storage,
                        checked = promptCache,
                        onToggle = { promptCache = it }
                    )
                    TauSwitchRow(
                        title = "Compresión de Historial",
                        subtitle = "Resume mensajes antiguos",
                        icon = CustomIcons.Compress,
                        checked = historyCompress,
                        onToggle = { historyCompress = it }
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    Text("Límites de Procesamiento", fontWeight = FontWeight.Bold, color = TauText2, fontSize = 13.sp)
                    
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Iteraciones: $maxIterations", modifier = Modifier.weight(1f), color = TauText1, fontSize = 14.sp)
                            Slider(
                                value = maxIterations.toFloat(),
                                onValueChange = { maxIterations = it.toInt() },
                                valueRange = 1f..20f,
                                modifier = Modifier.width(140.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Historial: $maxHistory", modifier = Modifier.weight(1f), color = TauText1, fontSize = 14.sp)
                            Slider(
                                value = maxHistory.toFloat(),
                                onValueChange = { maxHistory = it.toInt() },
                                valueRange = 5f..50f,
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }

                // ── 3. Apariencia (Glass & Temas) ──────────────────────────────────
                TauSettingsSection(title = "Apariencia", icon = CustomIcons.Palette) {
                    Text("Temas Glass (Base Blanca)", fontWeight = FontWeight.Bold, color = TauText1, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val themeList = listOf("DeepSeaBlue", "NebulaPurple", "AuroraGreen", "SolarOrange", "CrimsonVoid")
                        themeList.forEach { name ->
                            val color = when(name) {
                                "NebulaPurple" -> GlassThemes.NebulaPurple
                                "AuroraGreen"  -> GlassThemes.AuroraGreen
                                "SolarOrange"  -> GlassThemes.SolarOrange
                                "CrimsonVoid"  -> GlassThemes.CrimsonVoid
                                else           -> GlassThemes.DeepSeaBlue
                            }
                            val isSelected = theme == name
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { 
                                        theme = name
                                        updateGlassTheme(name)
                                    }
                                    .border(if (isSelected) 3.dp else 0.dp, TauText1, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) Icon(CustomIcons.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("Configuración de Vidrio", fontWeight = FontWeight.Bold, color = TauText1, fontSize = 14.sp)
                    
                    Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Opacidad: ${(currentGlassOpacity * 100).toInt()}%", modifier = Modifier.weight(1f), color = TauText1, fontSize = 14.sp)
                        Slider(
                            value = currentGlassOpacity,
                            onValueChange = { 
                                currentGlassOpacity = it
                                GlassOpacity = it 
                                scope.launch { settings.setGlassOpacity(it) }
                            },
                            valueRange = 0.1f..1f,
                            modifier = Modifier.width(140.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Desenfoque: ${currentGlassBlur.toInt()}dp", modifier = Modifier.weight(1f), color = TauText1, fontSize = 14.sp)
                        Slider(
                            value = currentGlassBlur,
                            onValueChange = { 
                                currentGlassBlur = it
                                GlassBlur = it
                                scope.launch { settings.setGlassBlur(it) }
                            },
                            valueRange = 0f..50f,
                            modifier = Modifier.width(140.dp)
                        )
                    }
                    }
                }

                // ── 4. Modo Friendly (Hidden Exposed) ──────────────────────────────
                TauSettingsSection(title = "Modo Friendly", icon = CustomIcons.Spa) {
                    TauSwitchRow(
                        title = "Habilitar Modo Friendly",
                        subtitle = "Barra de compañía interactiva",
                        icon = CustomIcons.Visibility,
                        checked = friendlyMode,
                        onToggle = { friendlyMode = it }
                    )
                    TauSwitchRow(
                        title = "Inicio Automático",
                        subtitle = "Al encender el dispositivo",
                        icon = CustomIcons.PowerSettingsNew,
                        checked = autoStartFriendly,
                        onToggle = { autoStartFriendly = it }
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    Text("Personalización de Barra", fontWeight = FontWeight.Bold, color = TauText2, fontSize = 13.sp)
                    
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Altura: ${friendlyBarHeight.toInt()}dp", modifier = Modifier.weight(1f), color = TauText1, fontSize = 14.sp)
                            Slider(
                                value = friendlyBarHeight,
                                onValueChange = { friendlyBarHeight = it },
                                valueRange = 40f..120f,
                                modifier = Modifier.width(140.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Opacidad Barra: ${(friendlyBarOpacity * 100).toInt()}%", modifier = Modifier.weight(1f), color = TauText1, fontSize = 14.sp)
                            Slider(
                                value = friendlyBarOpacity,
                                onValueChange = { friendlyBarOpacity = it },
                                valueRange = 0.5f..1.0f,
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                GlassButton(onClick = {
                    scope.launch {
                        settings.setProvider(provider)
                        settings.setTheme(theme)
                        settings.setMaxIterations(maxIterations)
                        settings.setMaxHistoryMessages(maxHistory)
                        settings.setExpertMode(expertMode)
                        settings.setTokenOptimizerEnabled(tokenOptimizer)
                        settings.setSystemPromptCacheEnabled(promptCache)
                        settings.setHistoryCompressionEnabled(historyCompress)
                        settings.setDebugMode(debugMode)
                        
                        settings.setFriendlyModeEnabled(friendlyMode)
                        settings.setAutoStartFriendly(autoStartFriendly)
                        settings.setFriendlyBarHeight(friendlyBarHeight)
                        settings.setFriendlyBarOpacity(friendlyBarOpacity)
                        
                        showSettingsSaved = true
                        delay(2000)
                        showSettingsSaved = false
                        onProfileChanged()
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showSettingsSaved) "¡TODO GUARDADO!" else "GUARDAR TODOS LOS AJUSTES", fontWeight = FontWeight.Bold)
                }
                
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
