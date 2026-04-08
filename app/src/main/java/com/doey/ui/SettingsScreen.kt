package com.doey.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.DoeyApplication
import com.doey.agent.ProfileStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Constantes de opciones ─────────────────────────────────────────────────────
val PROVIDERS = listOf(
    "openrouter" to "OpenRouter (Recomendado)",
    "gemini"     to "Google Gemini",
    "groq"       to "Groq (Rápido)",
    "openai"     to "OpenAI",
    "custom"     to "Personalizado"
)

val LANGUAGES = listOf(
    "system" to "Automático (sistema)",
    "es-MX"  to "Español (México)",
    "es-ES"  to "Español (España)",
    "en-US"  to "English (US)",
    "en-GB"  to "English (UK)",
    "pt-BR"  to "Português (Brasil)",
    "fr-FR"  to "Français",
    "de-DE"  to "Deutsch",
    "ja-JP"  to "日本語",
    "zh-CN"  to "中文 (简体)"
)

val STT_MODES = listOf(
    "auto"    to "Automático",
    "google"  to "Google STT",
    "whisper" to "Whisper (offline)"
)

val THEMES = listOf(
    "tau"   to "Tau (Oscuro Premium)",
    "light" to "Claro",
    "auto"  to "Seguir sistema"
)

val BUBBLE_POSITIONS = listOf(
    "right"  to "Derecha",
    "left"   to "Izquierda",
    "bottom" to "Abajo"
)

// ── Pantalla principal de Ajustes ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onProfileChanged: () -> Unit = {}) {
    val ctx      = LocalContext.current
    val state    by vm.uiState.collectAsState()
    val settings = vm.getSettings()
    val scope    = rememberCoroutineScope()
    val profileStore = remember { ProfileStore(ctx) }

    // ── Estado de ajustes ──────────────────────────────────────────────────────
    var provider         by remember { mutableStateOf("openrouter") }
    var apiKey           by remember { mutableStateOf("") }
    var googleApiKey     by remember { mutableStateOf("") }
    var model            by remember { mutableStateOf("openrouter/auto") }
    var customUrl        by remember { mutableStateOf("") }
    var language         by remember { mutableStateOf("system") }
    var wakePhrase       by remember { mutableStateOf("hey doey") }
    var wakeWordEnabled  by remember { mutableStateOf(false) }
    var soul             by remember { mutableStateOf("") }
    var personalMemory   by remember { mutableStateOf("") }
    var maxIterations    by remember { mutableStateOf(10) }
    var sttMode          by remember { mutableStateOf("auto") }
    var expertMode       by remember { mutableStateOf(false) }
    var showApiKey       by remember { mutableStateOf(false) }
    var showGoogleKey    by remember { mutableStateOf(false) }
    var enabledSkills    by remember { mutableStateOf(setOf<String>()) }
    var userProfile      by remember { mutableStateOf(profileStore.getUserProfile()) }
    var isLowPower       by remember { mutableStateOf(profileStore.isLowPowerMode()) }

    // Ajustes avanzados adicionales
    var maxHistoryMessages  by remember { mutableStateOf(20) }
    var bubblePosition      by remember { mutableStateOf("right") }
    var theme               by remember { mutableStateOf("tau") }
    var enableFriendlyMode  by remember { mutableStateOf(true) }
    var friendlyContextRead by remember { mutableStateOf(true) }
    var tokenOptimizer      by remember { mutableStateOf(true) }
    var systemPromptCache   by remember { mutableStateOf(true) }
    var historyCompression  by remember { mutableStateOf(true) }
    var debugMode           by remember { mutableStateOf(false) }
    var overlayEnabled      by remember { mutableStateOf(false) }
    var notifEnabled        by remember { mutableStateOf(false) }
    var autoStartFriendly   by remember { mutableStateOf(false) }

    val allSkills = DoeyApplication.instance.skillLoader.getAllSkills()

    LaunchedEffect(Unit) {
        provider            = settings.getProvider()
        apiKey              = settings.getApiKey(provider)
        googleApiKey        = settings.getCredential("google_api_key")
        model               = settings.getModel()
        customUrl           = settings.getCustomModelUrl()
        language            = settings.getLanguage()
        wakePhrase          = settings.getWakePhrase()
        wakeWordEnabled     = settings.getWakeWordEnabled()
        soul                = settings.getSoul()
        personalMemory      = settings.getPersonalMemory()
        maxIterations       = settings.getMaxIterations()
        sttMode             = settings.getSttMode()
        expertMode          = settings.getExpertMode()
        enabledSkills       = settings.getEnabledSkillsList().toSet()
        maxHistoryMessages  = settings.getMaxHistoryMessages()
        bubblePosition      = settings.getBubblePosition()
        theme               = settings.getTheme()
        enableFriendlyMode  = settings.getFriendlyModeEnabled()
        friendlyContextRead = settings.getFriendlyContextRead()
        tokenOptimizer      = settings.getTokenOptimizerEnabled()
        systemPromptCache   = settings.getSystemPromptCacheEnabled()
        historyCompression  = settings.getHistoryCompressionEnabled()
        debugMode           = settings.getDebugMode()
        overlayEnabled      = settings.getOverlayEnabled()
        notifEnabled        = settings.getNotifEnabled()
        autoStartFriendly   = settings.getAutoStartFriendly()
    }

    val googleKeyRequired = provider == "gemini"
    val googleKeyNote = if (googleKeyRequired)
        "✅ Requerida para el proveedor seleccionado (Gemini)."
    else "ℹ️ Opcional. Usada por skills de Google (Maps, Calendar, etc.)."

    Column(
        Modifier.fillMaxSize().background(TauBg).verticalScroll(rememberScrollState())
    ) {
        // ── TopBar ─────────────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Column {
                    Text("Ajustes", color = TauText1, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("23.4.9 Ultra · Tau Version", color = TauText3, fontSize = 10.sp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1),
            actions = {
                // Indicador de perfil actual
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (userProfile == ProfileStore.PROFILE_ADVANCED)
                        TauAccent.copy(alpha = 0.2f) else TauBlue.copy(alpha = 0.2f),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text(
                        if (userProfile == ProfileStore.PROFILE_ADVANCED) "⚡ Avanzado" else "✦ Básico",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (userProfile == ProfileStore.PROFILE_ADVANCED) TauAccentLight else TauBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        )

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── Sección: Perfil y Modo ─────────────────────────────────────────
            TauSettingsSection(
                title = "Perfil y Modo",
                icon  = Icons.Default.Person
            ) {
                // Selector de perfil
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileChip(
                        label    = "✦ Básico",
                        subtitle = "Simple y rápido",
                        selected = userProfile == ProfileStore.PROFILE_BASIC,
                        color    = TauBlue,
                        modifier = Modifier.weight(1f)
                    ) {
                        userProfile = ProfileStore.PROFILE_BASIC
                        profileStore.setUserProfile(ProfileStore.PROFILE_BASIC)
                        onProfileChanged()
                    }
                    ProfileChip(
                        label    = "⚡ Avanzado",
                        subtitle = "Control total",
                        selected = userProfile == ProfileStore.PROFILE_ADVANCED,
                        color    = TauAccent,
                        modifier = Modifier.weight(1f)
                    ) {
                        userProfile = ProfileStore.PROFILE_ADVANCED
                        profileStore.setUserProfile(ProfileStore.PROFILE_ADVANCED)
                        onProfileChanged()
                    }
                }
                Spacer(Modifier.height(4.dp))
                // Modo Experto (alias del perfil avanzado)
                TauSwitchRow(
                    title    = "Modo Experto",
                    subtitle = "Muestra todos los ajustes avanzados de IA",
                    icon     = Icons.Default.Science,
                    checked  = expertMode,
                    onToggle = { expertMode = it }
                )
                // Bajo consumo
                TauSwitchRow(
                    title    = "Modo Eco (Bajo Consumo)",
                    subtitle = "Reduce el uso de batería y tokens",
                    icon     = Icons.Default.BatteryChargingFull,
                    checked  = isLowPower,
                    onToggle = {
                        isLowPower = it
                        profileStore.setPerformanceMode(
                            if (it) ProfileStore.PERF_LOW_POWER else ProfileStore.PERF_HIGH
                        )
                        onProfileChanged()
                    }
                )
            }

            // ── Sección: Proveedor de IA ───────────────────────────────────────
            TauSettingsSection(
                title = "Proveedor de IA",
                icon  = Icons.Default.Psychology
            ) {
                DoeyDropdown(
                    label    = "Proveedor",
                    value    = PROVIDERS.find { it.first == provider }?.second ?: provider,
                    options  = PROVIDERS,
                    onSelect = {
                        provider = it
                        model = when (it) {
                            "openrouter" -> "openrouter/auto"
                            "gemini"     -> "gemini-2.5-flash-preview-04-17"
                            "groq"       -> "llama-3.3-70b-versatile"
                            "openai"     -> "gpt-4o"
                            else         -> model
                        }
                    }
                )
                DoeyTextField(
                    value         = apiKey,
                    onValueChange = { apiKey = it },
                    label         = "Clave API",
                    visual        = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing      = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null, tint = TauText3)
                        }
                    }
                )
                if (expertMode) {
                    DoeyTextField(
                        value         = model,
                        onValueChange = { model = it },
                        label         = "Modelo",
                        placeholder   = when (provider) {
                            "openrouter" -> "openrouter/auto"
                            "gemini"     -> "gemini-2.5-flash-preview-04-17"
                            "groq"       -> "llama-3.3-70b-versatile"
                            "openai"     -> "gpt-4o"
                            else         -> "openrouter/auto"
                        }
                    )
                    if (provider == "custom") {
                        DoeyTextField(
                            value         = customUrl,
                            onValueChange = { customUrl = it },
                            label         = "URL de API personalizada",
                            placeholder   = "https://tu-endpoint.com/v1/chat/completions"
                        )
                    }
                }
            }

            // ── Sección: Google API (solo experto) ─────────────────────────────
            AnimatedVisibility(visible = expertMode) {
                TauSettingsSection(title = "Google API Key", icon = Icons.Default.Key) {
                    Text(googleKeyNote,
                        color = if (googleKeyRequired) TauAccentLight else TauText3,
                        fontSize = 12.sp)
                    DoeyTextField(
                        value         = googleApiKey,
                        onValueChange = { googleApiKey = it },
                        label         = "Google API Key",
                        visual        = if (showGoogleKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailing      = {
                            IconButton(onClick = { showGoogleKey = !showGoogleKey }) {
                                Icon(if (showGoogleKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, tint = TauText3)
                            }
                        }
                    )
                }
            }

            // ── Sección: Optimización de IA ────────────────────────────────────
            TauSettingsSection(title = "Optimización de IA", icon = Icons.Default.Speed) {
                Text("Controla cómo Doey usa los tokens para ser más eficiente.",
                    color = TauText3, fontSize = 12.sp)
                TauSwitchRow(
                    title    = "Optimizador de Tokens",
                    subtitle = "Clasifica comandos y usa menos tokens en tareas simples",
                    icon     = Icons.Default.TrendingDown,
                    checked  = tokenOptimizer,
                    onToggle = { tokenOptimizer = it }
                )
                TauSwitchRow(
                    title    = "Cache de System Prompt",
                    subtitle = "Reutiliza el prompt del sistema entre mensajes",
                    icon     = Icons.Default.Memory,
                    checked  = systemPromptCache,
                    onToggle = { systemPromptCache = it }
                )
                TauSwitchRow(
                    title    = "Compresión de Historial",
                    subtitle = "Resume mensajes antiguos para ahorrar tokens",
                    icon     = Icons.Default.Compress,
                    checked  = historyCompression,
                    onToggle = { historyCompression = it }
                )
                if (expertMode) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Iteraciones máx. de herramientas: $maxIterations",
                                color = TauText1, fontSize = 14.sp)
                            Text("Más iteraciones = más capacidad pero más tokens",
                                color = TauText3, fontSize = 11.sp)
                        }
                        Slider(
                            value         = maxIterations.toFloat(),
                            onValueChange = { maxIterations = it.toInt() },
                            valueRange    = 1f..20f,
                            steps         = 18,
                            modifier      = Modifier.width(140.dp),
                            colors        = SliderDefaults.colors(thumbColor = TauAccent, activeTrackColor = TauAccent)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Mensajes en historial: $maxHistoryMessages",
                                color = TauText1, fontSize = 14.sp)
                            Text("Más historial = más contexto pero más tokens",
                                color = TauText3, fontSize = 11.sp)
                        }
                        Slider(
                            value         = maxHistoryMessages.toFloat(),
                            onValueChange = { maxHistoryMessages = it.toInt() },
                            valueRange    = 4f..50f,
                            steps         = 22,
                            modifier      = Modifier.width(140.dp),
                            colors        = SliderDefaults.colors(thumbColor = TauAccent, activeTrackColor = TauAccent)
                        )
                    }
                }
            }

            // ── Sección: Burbuja y Overlay ─────────────────────────────────────
            TauSettingsSection(title = "Burbuja Flotante", icon = Icons.Default.BubbleChart) {
                TauSwitchRow(
                    title    = "Burbuja flotante",
                    subtitle = "Muestra la burbuja de Doey sobre otras apps",
                    icon     = Icons.Default.Circle,
                    checked  = overlayEnabled,
                    onToggle = { overlayEnabled = it; vm.toggleOverlay(it) }
                )
                if (expertMode) {
                    DoeyDropdown(
                        label    = "Posición de la burbuja",
                        value    = BUBBLE_POSITIONS.find { it.first == bubblePosition }?.second ?: bubblePosition,
                        options  = BUBBLE_POSITIONS,
                        onSelect = { bubblePosition = it }
                    )
                }
            }

            // ── Sección: Modo Friendly ─────────────────────────────────────────
            TauSettingsSection(title = "Modo Friendly", icon = Icons.Default.Spa) {
                Text("Barra inferior que aparece al invocar Doey como asistente del sistema.",
                    color = TauText3, fontSize = 12.sp)
                TauSwitchRow(
                    title    = "Activar Modo Friendly",
                    subtitle = "Barra verde en la parte inferior de la pantalla",
                    icon     = Icons.Default.Spa,
                    checked  = enableFriendlyMode,
                    onToggle = { enableFriendlyMode = it }
                )
                TauSwitchRow(
                    title    = "Leer contexto de pantalla",
                    subtitle = "Doey lee la app activa para respuestas más inteligentes",
                    icon     = Icons.Default.Visibility,
                    checked  = friendlyContextRead,
                    onToggle = { friendlyContextRead = it }
                )
                if (expertMode) {
                    TauSwitchRow(
                        title    = "Iniciar automáticamente",
                        subtitle = "Inicia el Modo Friendly al arrancar el dispositivo",
                        icon     = Icons.Default.PlayCircle,
                        checked  = autoStartFriendly,
                        onToggle = { autoStartFriendly = it }
                    )
                }
            }

            // ── Sección: Notificaciones ────────────────────────────────────────
            TauSettingsSection(title = "Notificaciones", icon = Icons.Default.Notifications) {
                TauSwitchRow(
                    title    = "Acceso a notificaciones",
                    subtitle = "Doey puede leer y responder notificaciones",
                    icon     = Icons.Default.NotificationsActive,
                    checked  = notifEnabled,
                    onToggle = { notifEnabled = it }
                )
            }

            // ── Sección: Voz e Idioma ──────────────────────────────────────────
            TauSettingsSection(title = "Voz e Idioma", icon = Icons.Default.RecordVoiceOver) {
                DoeyDropdown(
                    label    = "Idioma",
                    value    = LANGUAGES.find { it.first == language }?.second ?: language,
                    options  = LANGUAGES,
                    onSelect = { language = it }
                )
                TauSwitchRow(
                    title    = "Activación por voz",
                    subtitle = "Llama a Doey diciendo la frase mágica",
                    icon     = Icons.Default.Mic,
                    checked  = wakeWordEnabled,
                    onToggle = { wakeWordEnabled = it; vm.toggleWakeWord() }
                )
                if (wakeWordEnabled) {
                    DoeyTextField(
                        value         = wakePhrase,
                        onValueChange = { wakePhrase = it },
                        label         = "Frase de activación",
                        placeholder   = "hey doey"
                    )
                }
                if (expertMode) {
                    DoeyDropdown(
                        label    = "Modo de reconocimiento de voz",
                        value    = STT_MODES.find { it.first == sttMode }?.second ?: sttMode,
                        options  = STT_MODES,
                        onSelect = { sttMode = it }
                    )
                }
            }

            // ── Sección: Personalidad y Memoria ───────────────────────────────
            TauSettingsSection(title = "Personalidad y Memoria", icon = Icons.Default.AutoAwesome) {
                DoeyTextField(
                    value         = soul,
                    onValueChange = { soul = it },
                    label         = "Personalidad (Soul)",
                    placeholder   = "Ej: Eres un asistente sarcástico pero eficiente...",
                    single        = false
                )
                DoeyTextField(
                    value         = personalMemory,
                    onValueChange = { personalMemory = it },
                    label         = "Memoria Personal",
                    placeholder   = "Ej: Mi nombre es Juan, me gusta el café...",
                    single        = false
                )
            }

            // ── Sección: Apariencia (solo experto) ────────────────────────────
            AnimatedVisibility(visible = expertMode) {
                TauSettingsSection(title = "Apariencia", icon = Icons.Default.Palette) {
                    DoeyDropdown(
                        label    = "Tema",
                        value    = THEMES.find { it.first == theme }?.second ?: theme,
                        options  = THEMES,
                        onSelect = { theme = it }
                    )
                }
            }

            // ── Sección: Skills (solo experto) ────────────────────────────────
            AnimatedVisibility(visible = expertMode) {
                TauSettingsSection(
                    title = "Habilidades (${enabledSkills.size}/${allSkills.size})",
                    icon  = Icons.Default.Extension
                ) {
                    allSkills.sortedBy { it.name }.forEach { skill ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = skill.name in enabledSkills,
                                onCheckedChange = { checked ->
                                    enabledSkills = if (checked) enabledSkills + skill.name else enabledSkills - skill.name
                                },
                                colors = CheckboxDefaults.colors(checkedColor = TauAccent)
                            )
                            Column {
                                Text(skill.name, color = TauText1, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(skill.description, color = TauText3, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // ── Sección: Debug (solo experto) ──────────────────────────────────
            AnimatedVisibility(visible = expertMode) {
                TauSettingsSection(title = "Depuración", icon = Icons.Default.BugReport) {
                    TauSwitchRow(
                        title    = "Modo Debug",
                        subtitle = "Muestra información técnica en los logs",
                        icon     = Icons.Default.Code,
                        checked  = debugMode,
                        onToggle = { debugMode = it }
                    )
                    Text("Los logs se pueden ver en la sección 'Logs' del menú.",
                        color = TauText3, fontSize = 11.sp)
                }
            }

            // ── Botón Guardar ─────────────────────────────────────────────────
            Button(
                onClick = {
                    vm.saveSettings(
                        provider, apiKey, model, customUrl, language, wakePhrase,
                        enabledSkills.toList(), soul, personalMemory, maxIterations, sttMode, expertMode
                    )
                    scope.launch {
                        settings.setCredential("google_api_key", googleApiKey)
                        settings.setMaxHistoryMessages(maxHistoryMessages)
                        settings.setBubblePosition(bubblePosition)
                        settings.setTheme(theme)
                        settings.setFriendlyModeEnabled(enableFriendlyMode)
                        settings.setFriendlyContextRead(friendlyContextRead)
                        settings.setTokenOptimizerEnabled(tokenOptimizer)
                        settings.setSystemPromptCacheEnabled(systemPromptCache)
                        settings.setHistoryCompressionEnabled(historyCompression)
                        settings.setDebugMode(debugMode)
                        settings.setOverlayEnabled(overlayEnabled)
                        settings.setNotifEnabled(notifEnabled)
                        settings.setAutoStartFriendly(autoStartFriendly)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = TauAccent)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Guardar Cambios", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Mensaje de confirmación
            AnimatedVisibility(visible = state.settingsSaved) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TauGreen.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = TauGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("¡Ajustes guardados correctamente!",
                            color = TauGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Componentes de UI reutilizables ───────────────────────────────────────────

@Composable
fun TauSettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = RoundedCornerShape(16.dp),
        color          = TauSurface1,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = TauAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = TauAccentLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            HorizontalDivider(color = TauSeparator)
            content()
        }
    }
}

// Alias para compatibilidad
@Composable
fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) =
    TauSettingsSection(title, Icons.Default.Settings, content)

@Composable
fun TauSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, null, tint = TauText3, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TauText1, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, color = TauText3, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = Color.White,
                checkedTrackColor  = TauAccent,
                uncheckedThumbColor = TauText3,
                uncheckedTrackColor = TauSurface3
            )
        )
    }
}

@Composable
private fun ProfileChip(
    label: String,
    subtitle: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape    = RoundedCornerShape(12.dp),
        color    = if (selected) color.copy(alpha = 0.2f) else TauSurface2,
        border   = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = if (selected) color else TauText2,
                fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = TauText3, fontSize = 10.sp)
        }
    }
}

@Composable
fun DoeyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    visual: VisualTransformation = VisualTransformation.None,
    trailing: @Composable (() -> Unit)? = null,
    single: Boolean = true
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = { Text(label) },
        placeholder          = { Text(placeholder) },
        modifier             = Modifier.fillMaxWidth(),
        shape                = RoundedCornerShape(12.dp),
        visualTransformation = visual,
        trailingIcon         = trailing,
        singleLine           = single,
        colors               = doeyFieldColors()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoeyDropdown(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value         = value,
            onValueChange = {},
            readOnly      = true,
            label         = { Text(label) },
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier      = Modifier.fillMaxWidth().menuAnchor(),
            shape         = RoundedCornerShape(12.dp),
            colors        = doeyFieldColors()
        )
        ExposedDropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            modifier         = Modifier.background(TauSurface2)
        ) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text    = { Text(name, color = TauText1) },
                    onClick = { onSelect(id); expanded = false }
                )
            }
        }
    }
}
