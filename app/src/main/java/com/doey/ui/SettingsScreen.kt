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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val state    by vm.uiState.collectAsState()
    val settings = vm.getSettings()
    val scope    = rememberCoroutineScope()

    var provider       by remember { mutableStateOf("gemini") }
    var apiKey         by remember { mutableStateOf("") }
    var googleApiKey   by remember { mutableStateOf("") }
    var model          by remember { mutableStateOf("gemini-2.5-flash-preview-04-17") }
    var customUrl      by remember { mutableStateOf("") }
    var language       by remember { mutableStateOf("system") }
    var picovoiceKey   by remember { mutableStateOf("") }
    var soul           by remember { mutableStateOf("") }
    var personalMemory by remember { mutableStateOf("") }
    var maxIterations  by remember { mutableStateOf(10) }
    var sttMode        by remember { mutableStateOf("auto") }
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
        picovoiceKey   = settings.getPicovoiceKey()
        soul           = settings.getSoul()
        personalMemory = settings.getPersonalMemory()
        maxIterations  = settings.getMaxIterations()
        sttMode        = settings.getSttMode()
        enabledSkills  = settings.getEnabledSkillsList().toSet()
    }

    // Necesidad de Google API Key según provider
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

            // ── Proveedor de IA ────────────────────────────────────────────────
            SettingsCard("Proveedor de IA") {
                DoeyDropdown(
                    label    = "Proveedor",
                    value    = PROVIDERS.find { it.first == provider }?.second ?: provider,
                    options  = PROVIDERS,
                    onSelect = {
                        provider = it
                        model = when (it) {
                            "gemini" -> "gemini-2.5-flash-preview-04-17"
                            "groq"   -> "llama-3.3-70b-versatile"
                            "openai" -> "gpt-4o"
                            else     -> model
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
                DoeyTextField(
                    value         = model,
                    onValueChange = { model = it },
                    label         = "Modelo",
                    placeholder   = when (provider) {
                        "gemini" -> "gemini-2.5-flash-preview-04-17"
                        "groq"   -> "llama-3.3-70b-versatile"
                        else     -> "gpt-4o"
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

            // ── Google API Key ────────────────────────────────────────────────
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
                Text(
                    "Obtén tu clave en: console.cloud.google.com\n" +
                    "Activa: Gemini API, Maps, Calendar, etc. según tus skills.",
                    color    = Label3Light,
                    fontSize = 11.sp
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
                DoeyDropdown(
                    label    = "Modo de reconocimiento de voz",
                    value    = STT_MODES.find { it.first == sttMode }?.second ?: sttMode,
                    options  = STT_MODES,
                    onSelect = { sttMode = it }
                )
            }

            // ── Palabra de activación ─────────────────────────────────────────
            SettingsCard("Palabra de activación (Picovoice)") {
                DoeyTextField(
                    value         = picovoiceKey,
                    onValueChange = { picovoiceKey = it },
                    label         = "Clave de acceso Picovoice",
                    visual        = PasswordVisualTransformation()
                )
                Text(
                    "Clave gratuita en console.picovoice.ai\n" +
                    "Coloca un archivo .ppn en assets/ para una palabra personalizada.",
                    color = Label3Light, fontSize = 12.sp
                )
            }

            // ── Skills ────────────────────────────────────────────────────────
            SettingsCard("Habilidades (${enabledSkills.size}/${allSkills.size})") {
                allSkills.sortedBy { it.name }.forEach { skill ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = skill.name in enabledSkills,
                            onCheckedChange = { on ->
                                enabledSkills = if (on) enabledSkills + skill.name else enabledSkills - skill.name
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Purple)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(skill.name, color = Label1Light, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            if (skill.description.isNotBlank())
                                Text(skill.description, color = Label3Light, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }

            // ── Personalidad ──────────────────────────────────────────────────
            SettingsCard("Personalidad (SOUL)") {
                DoeyTextField(
                    value         = soul,
                    onValueChange = { soul = it },
                    label         = "Instrucciones de personalidad",
                    placeholder   = "Ej: Eres un asistente conciso y amigable…",
                    minLines      = 3,
                    maxLines      = 6
                )
            }

            // ── Memoria personal ──────────────────────────────────────────────
            SettingsCard("Memoria Personal") {
                DoeyTextField(
                    value         = personalMemory,
                    onValueChange = { personalMemory = it },
                    label         = "Datos personales (MEMORY.md)",
                    placeholder   = "- [nombre] Me llamo…\n- [familia] Mi pareja se llama…",
                    minLines      = 3,
                    maxLines      = 8
                )
            }

            // ── Guardar ───────────────────────────────────────────────────────
            Button(
                onClick = {
                    // Guardar Google API Key por separado (credencial)
                    scope.launch { settings.setCredential("google_api_key", googleApiKey) }
                    vm.saveSettings(
                        provider, apiKey, model, customUrl, language, picovoiceKey,
                        enabledSkills.toList(), soul, personalMemory, maxIterations, sttMode
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                Icon(Icons.Default.Save, null, tint = OnPurple)
                Spacer(Modifier.width(8.dp))
                Text("Guardar ajustes", color = OnPurple, fontWeight = FontWeight.Bold)
            }

            if (state.settingsSaved) {
                Text(
                    "✓ Ajustes guardados",
                    color       = Color(0xFF1A7A1A),
                    textAlign   = TextAlign.Center,
                    modifier    = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private val PROVIDERS = listOf(
    "gemini" to "Gemini (Google)",
    "groq"   to "Groq",
    "openai" to "OpenAI",
    "custom" to "Personalizado"
)

private val LANGUAGES = listOf(
    "system" to "Predeterminado del sistema",
    "es-ES"  to "Español (España)",
    "es-MX"  to "Español (México)",
    "en-US"  to "Inglés (EE. UU.)",
    "en-GB"  to "Inglés (Reino Unido)",
    "de-DE"  to "Alemán",
    "de-AT"  to "Alemán (Austria)",
    "fr-FR"  to "Francés",
    "it-IT"  to "Italiano",
    "pt-BR"  to "Portugués (Brasil)"
)

private val STT_MODES = listOf(
    "auto"    to "Automático (inteligente)",
    "offline" to "Solo en dispositivo",
    "online"  to "Solo en la nube"
)

@Composable
fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = Surface1Light) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Purple, fontSize = 14.sp)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoeyDropdown(
    label:    String,
    value:    String,
    options:  List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value         = value,
            onValueChange = {},
            readOnly      = true,
            label         = { Text(label) },
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier      = Modifier.menuAnchor().fillMaxWidth(),
            colors        = doeyFieldColors()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (code, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(code); expanded = false })
            }
        }
    }
}

@Composable
fun DoeyTextField(
    value:         String,
    onValueChange: (String) -> Unit,
    label:         String,
    placeholder:   String               = "",
    visual:        VisualTransformation = VisualTransformation.None,
    minLines:      Int                  = 1,
    maxLines:      Int                  = 1,
    trailing:      (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = { Text(label) },
        placeholder          = { Text(placeholder, color = Label3Light) },
        modifier             = Modifier.fillMaxWidth(),
        visualTransformation = visual,
        trailingIcon         = trailing,
        minLines             = minLines,
        maxLines             = maxLines,
        colors               = doeyFieldColors()
    )
}

@Composable
fun doeyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor        = Purple,
    unfocusedBorderColor      = Label3Light,
    focusedTextColor          = Label1Light,
    unfocusedTextColor        = Label1Light,
    focusedLabelColor         = Purple,
    unfocusedLabelColor       = Label3Light,
    cursorColor               = Purple,
    focusedPlaceholderColor   = Label3Light,
    unfocusedPlaceholderColor = Label3Light
)
