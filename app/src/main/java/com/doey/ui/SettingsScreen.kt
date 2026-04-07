package com.doey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.DoeyApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val state    by vm.uiState.collectAsState()
    val settings = vm.getSettings()
    val scope    = rememberCoroutineScope()

    var provider       by remember { mutableStateOf("openrouter") }
    var apiKey         by remember { mutableStateOf("") }
    var googleApiKey   by remember { mutableStateOf("") }
    var model          by remember { mutableStateOf("openrouter/auto") }
    var customUrl      by remember { mutableStateOf("") }
    var language       by remember { mutableStateOf("system") }
    var wakePhrase     by remember { mutableStateOf("hey doey") }
    var wakeWordEnabled by remember { mutableStateOf(false) }
    var soul           by remember { mutableStateOf("") }
    var personalMemory by remember { mutableStateOf("") }
    var maxIterations  by remember { mutableStateOf(10) }
    var sttMode        by remember { mutableStateOf("auto") }
    var expertMode     by remember { mutableStateOf(false) }
    var showApiKey     by remember { mutableStateOf(false) }
    var showGoogleKey  by remember { mutableStateOf(false) }
    var enabledSkills  by remember { mutableStateOf(setOf<String>()) }

    val allSkills = DoeyApplication.instance.skillLoader.getAllSkills()

    LaunchedEffect(Unit) {
        provider       = settings.getProvider()
        apiKey         = settings.getApiKey(provider)
        googleApiKey   = settings.getCredential("google_api_key")
        model          = settings.getModel()
        customUrl      = settings.getCustomModelUrl()
        language       = settings.getLanguage()
        wakePhrase     = settings.getWakePhrase()
        wakeWordEnabled = settings.getWakeWordEnabled()
        soul           = settings.getSoul()
        personalMemory = settings.getPersonalMemory()
        maxIterations  = settings.getMaxIterations()
        sttMode        = settings.getSttMode()
        expertMode     = settings.getExpertMode()
        enabledSkills  = settings.getEnabledSkillsList().toSet()
    }

    val googleKeyRequired = provider in listOf("gemini")
    val googleKeyNote = when {
        googleKeyRequired -> "✅ Requerida para el proveedor seleccionado (Gemini)."
        else              -> "ℹ️ Opcional. Usada por skills de Google (Maps, Calendar, etc.)."
    }

    Column(
        Modifier.fillMaxSize().background(Surface0Light).verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title  = { Text("Ajustes", color = Label1Light, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1Light)
        )

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── Modo Experto Switch ──────────────────────────────────────────
            SettingsCard("Configuración de Interfaz") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Modo Experto",
                            color = Label1Light,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (expertMode) "Muestra todos los ajustes avanzados." else "Modo simplificado (sin claves API adicionales).",
                            color = Label3Light,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = expertMode,
                        onCheckedChange = { expertMode = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Purple)
                    )
                }
            }

            // ── Proveedor de IA ───────────────────────────────────────────────
            SettingsCard("Proveedor de IA") {
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
                    label         = "Clave API del proveedor",
                    visual        = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing      = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null, tint = Label3Light)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Iteraciones máx. de herramientas: $maxIterations",
                            color    = Label1Light,
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp
                        )
                        Slider(
                            value          = maxIterations.toFloat(),
                            onValueChange  = { maxIterations = it.toInt() },
                            valueRange     = 1f..20f,
                            steps          = 18,
                            modifier       = Modifier.width(140.dp),
                            colors         = SliderDefaults.colors(thumbColor = Purple, activeTrackColor = Purple)
                        )
                    }
                }
            }

            // ── Google API Key (Solo Experto) ─────────────────────────────────
            if (expertMode) {
                SettingsCard("Clave API de Google") {
                    Text(googleKeyNote, color = if (googleKeyRequired) Purple else Label3Light, fontSize = 12.sp)
                    DoeyTextField(
                        value         = googleApiKey,
                        onValueChange = { googleApiKey = it },
                        label         = "Google API Key",
                        visual        = if (showGoogleKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailing      = {
                            IconButton(onClick = { showGoogleKey = !showGoogleKey }) {
                                Icon(
                                    if (showGoogleKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, tint = Label3Light
                                )
                            }
                        }
                    )
                }
            }

            // ── Palabra de activación ─────────────────────────────────────────
            SettingsCard("Palabra de activación") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Activar por voz",
                            color = Label1Light,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Permite llamar a Doey diciendo la frase mágica.",
                            color = Label3Light,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = wakeWordEnabled,
                        onCheckedChange = { 
                            wakeWordEnabled = it
                            vm.toggleWakeWord()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Purple)
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    "Frase que activa a Doey. Usa algo corto y distinto, como \"hey doey\" o \"oye doey\".",
                    color    = Label3Light,
                    fontSize = 12.sp
                )
                DoeyTextField(
                    value         = wakePhrase,
                    onValueChange = { wakePhrase = it },
                    label         = "Frase de activación",
                    placeholder   = "hey doey"
                )
            }

            // ── Voz e Idioma ──────────────────────────────────────────────────
            SettingsCard("Voz e Idioma") {
                DoeyDropdown(
                    label    = "Idioma",
                    value    = LANGUAGES.find { it.first == language }?.second ?: language,
                    options  = LANGUAGES,
                    onSelect = { language = it }
                )
                if (expertMode) {
                    DoeyDropdown(
                        label    = "Modo de reconocimiento de voz",
                        value    = STT_MODES.find { it.first == sttMode }?.second ?: sttMode,
                        options  = STT_MODES,
                        onSelect = { sttMode = it }
                    )
                }
            }

            // ── Skills (Solo Experto) ─────────────────────────────────────────
            if (expertMode) {
                SettingsCard("Habilidades (${enabledSkills.size}/${allSkills.size})") {
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
                                colors = CheckboxDefaults.colors(checkedColor = Purple)
                            )
                            Column {
                                Text(skill.name, color = Label1Light, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(skill.description, color = Label3Light, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // ── Personalidad y Memoria ────────────────────────────────────────
            SettingsCard("Personalidad y Memoria") {
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

            // ── Botón Guardar ─────────────────────────────────────────────────
            Button(
                onClick = {
                    vm.saveSettings(
                        provider, apiKey, model, customUrl, language, wakePhrase,
                        enabledSkills.toList(), soul, personalMemory, maxIterations, sttMode, expertMode
                    )
                    scope.launch {
                        settings.setCredential("google_api_key", googleApiKey)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Guardar Cambios", fontWeight = FontWeight.Bold)
            }

            if (state.settingsSaved) {
                Text(
                    "¡Ajustes guardados correctamente!",
                    color     = Color(0xFF4CAF50),
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize  = 14.sp
                )
                LaunchedEffect(Unit) {
                    delay(3000)
                    // Resetear flag en el VM si fuera necesario, o simplemente dejar que pase el tiempo
                }
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = Surface1Light,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = Purple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            content()
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
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        placeholder   = { Text(placeholder) },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(12.dp),
        visualTransformation = visual,
        trailingIcon  = trailing,
        singleLine    = single,
        colors        = doeyFieldColors()
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
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
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
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text    = { Text(name) },
                    onClick = { onSelect(id); expanded = false }
                )
            }
        }
    }
}

private val PROVIDERS = listOf(
    "openrouter" to "OpenRouter (Recomendado)",
    "gemini"     to "Google Gemini",
    "groq"       to "Groq (Ultra rápido)",
    "openai"     to "OpenAI (GPT-4o)",
    "custom"     to "API Personalizada"
)

private val LANGUAGES = listOf(
    "system" to "Idioma del sistema",
    "es-ES"  to "Español (España)",
    "es-MX"  to "Español (México)",
    "en-US"  to "English (US)",
    "en-GB"  to "English (UK)",
    "pt-BR"  to "Português (Brasil)",
    "fr-FR"  to "Français",
    "de-DE"  to "Deutsch",
    "it-IT"  to "Italiano"
)

private val STT_MODES = listOf(
    "auto"    to "Automático (Nube + Local)",
    "offline" to "Solo Local (Privacidad)",
    "online"  to "Solo Nube (Precisión)"
)
