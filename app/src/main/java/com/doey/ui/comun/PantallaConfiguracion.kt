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
    val provider         = "gemini"
    var apiKey           by remember { mutableStateOf("") }
    var groqApiKey       by remember { mutableStateOf("") }
    var model            by remember { mutableStateOf("") }
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
    
    // (Glass controls eliminados en Delta — sin glassmorphism)

    // Cargar todos los ajustes al iniciar
    LaunchedEffect(Unit) {
        apiKey          = settings.getApiKey("gemini")
        groqApiKey      = settings.getApiKey("groq")
        model           = settings.getModel()
        theme           = settings.getTheme()
        maxIterations   = settings.getMaxIterations()
        maxHistory      = settings.getMaxHistoryMessages()
        expertMode      = settings.getExpertMode()
        tokenOptimizer  = settings.getTokenOptimizerEnabled()
        promptCache     = settings.getSystemPromptCacheEnabled()
        historyCompress = settings.getHistoryCompressionEnabled()
        debugMode       = settings.getDebugMode()
        
        updateGlassTheme(theme)
    }



    Box(Modifier.fillMaxSize()) {
        GlassBackground(accentColor = TauAccent)

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            TopAppBar(
                title  = { Text("Ajustes de Doey", fontWeight = FontWeight.Bold, color = TauText1) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {

                // ── 1. Cerebro de Doey — Gemini ──────────────────────────────────
                TauSettingsSection(title = "Cerebro de Doey", icon = CustomIcons.Psychology) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Icon(CustomIcons.AutoAwesome, null, tint = TauAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Google Gemini", fontWeight = FontWeight.Bold, color = TauAccent)
                            Text("El modelo con más límite gratuito — 1,500 req/día", fontSize = 12.sp, color = TauText3)
                        }
                    }

                    DoeyTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = "API Key de Gemini",
                        placeholder = "Obtén tu key gratis en aistudio.google.com"
                    )

                    Spacer(Modifier.height(8.dp))
                    DoeyTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = "Modelo (opcional)",
                        placeholder = "gemini-2.5-flash"
                    )
                    Text(
                        "gemini-2.5-flash = rápido y gratis  •  gemini-2.0-flash-lite = más ligero",
                        fontSize = 11.sp, color = TauText3, modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(Modifier.height(12.dp))
                    GlassButton(onClick = {
                        scope.launch {
                            settings.setApiKey("gemini", apiKey)
                            settings.setProvider("gemini")
                            settings.setModel(model.ifBlank { "gemini-2.5-flash" })
                            showApiSaved = true
                            delay(2000)
                            showApiSaved = false
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showApiSaved) "¡GUARDADO!" else "GUARDAR API KEY", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Fallback: Groq ────────────────────────────────────────────
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Icon(CustomIcons.Refresh, null, tint = TauAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Fallback: Groq (opcional)", fontWeight = FontWeight.Bold, color = TauAccent)
                            Text("Si Gemini falla, Doey usará Groq automáticamente", fontSize = 12.sp, color = TauText3)
                        }
                    }

                    DoeyTextField(
                        value = groqApiKey,
                        onValueChange = { groqApiKey = it },
                        label = "API Key de Groq",
                        placeholder = "Obtén tu key gratis en console.groq.com"
                    )
                    Text(
                        "llama-3.3-70b-versatile — gratuito y rápido como respaldo",
                        fontSize = 11.sp, color = TauText3, modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(Modifier.height(12.dp))
                    GlassButton(onClick = {
                        scope.launch {
                            settings.setApiKey("groq", groqApiKey)
                            showApiSaved = true
                            delay(2000)
                            showApiSaved = false
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showApiSaved) "¡GUARDADO!" else "GUARDAR API KEY FALLBACK", fontWeight = FontWeight.Bold)
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

                // ── 3. Apariencia — Tema Delta ──────────────────────────────────────
                TauSettingsSection(title = "Apariencia", icon = CustomIcons.Palette) {
                    Text("Color de acento", fontWeight = FontWeight.Bold, color = TauText1, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val themeList = listOf("NebulaPurple", "AuroraGreen", "SolarOrange", "CrimsonVoid", "OceanBlue")
                        themeList.forEach { name ->
                            val color = when(name) {
                                "NebulaPurple" -> DeltaColors.VioletPrimary
                                "AuroraGreen"  -> DeltaColors.EmeraldAccent
                                "SolarOrange"  -> DeltaColors.SunsetAccent
                                "CrimsonVoid"  -> DeltaColors.RoseAccent
                                else           -> DeltaColors.OceanAccent
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
                                        scope.launch { settings.setTheme(name) }
                                    }
                                    .border(if (isSelected) 3.dp else 0.dp, TauText1, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) Icon(CustomIcons.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                GlassButton(onClick = {
                    scope.launch {
                        settings.setProvider("gemini")
                        settings.setModel(model)
                        settings.setTheme(theme)
                        settings.setMaxIterations(maxIterations)
                        settings.setMaxHistoryMessages(maxHistory)
                        settings.setExpertMode(expertMode)
                        settings.setTokenOptimizerEnabled(tokenOptimizer)
                        settings.setSystemPromptCacheEnabled(promptCache)
                        settings.setHistoryCompressionEnabled(historyCompress)
                        settings.setDebugMode(debugMode)
                        
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
