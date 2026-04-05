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

    var provider      by remember { mutableStateOf("groq") }
    var apiKey        by remember { mutableStateOf("") }
    var model         by remember { mutableStateOf("llama-3.3-70b-versatile") }
    var customUrl     by remember { mutableStateOf("") }
    var language      by remember { mutableStateOf("system") }
    var picovoiceKey  by remember { mutableStateOf("") }
    var soul          by remember { mutableStateOf("") }
    var personalMemory by remember { mutableStateOf("") }
    var maxIterations by remember { mutableStateOf(10) }
    var sttMode       by remember { mutableStateOf("auto") }
    var showApiKey    by remember { mutableStateOf(false) }
    var enabledSkills by remember { mutableStateOf(setOf<String>()) }

    val allSkills = DoeyApplication.instance.skillLoader.getAllSkills()

    LaunchedEffect(Unit) {
        provider      = settings.getProvider()
        apiKey        = settings.getApiKey(provider)
        model         = settings.getModel()
        customUrl     = settings.getCustomModelUrl()
        language      = settings.getLanguage()
        picovoiceKey  = settings.getPicovoiceKey()
        soul          = settings.getSoul()
        personalMemory = settings.getPersonalMemory()
        maxIterations = settings.getMaxIterations()
        sttMode       = settings.getSttMode()
        enabledSkills = settings.getEnabledSkillsList().toSet()
    }

    Column(
        Modifier.fillMaxSize().background(Surface0).verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title  = { Text("Settings", color = Label1, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1)
        )

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── Proveedor ──────────────────────────────────────────────────────
            SettingsCard("AI Provider") {
                DoeyDropdown(
                    label    = "Provider",
                    value    = provider.replaceFirstChar { it.uppercase() },
                    options  = listOf("groq" to "Groq", "openai" to "OpenAI", "custom" to "Custom"),
                    onSelect = {
                        provider = it
                        if (it == "groq")   model = model.ifBlank { "llama-3.3-70b-versatile" }
                        if (it == "openai") model = model.ifBlank { "gpt-4o" }
                    }
                )
                DoeyTextField(
                    value    = apiKey,
                    onValueChange = { apiKey = it },
                    label    = "API Key",
                    visual   = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null, tint = Label3)
                        }
                    }
                )
                DoeyTextField(value = model, onValueChange = { model = it }, label = "Model",
                    placeholder = if (provider == "groq") "llama-3.3-70b-versatile" else "gpt-4o")
                if (provider == "custom") {
                    DoeyTextField(value = customUrl, onValueChange = { customUrl = it },
                        label = "Custom API URL",
                        placeholder = "https://your-endpoint.com/v1/chat/completions")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Max tool iterations: $maxIterations", color = Label1, modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Slider(value = maxIterations.toFloat(), onValueChange = { maxIterations = it.toInt() },
                        valueRange = 1f..20f, steps = 18, modifier = Modifier.width(140.dp),
                        colors = SliderDefaults.colors(thumbColor = Purple, activeTrackColor = Purple))
                }
            }

            // ── Speech ─────────────────────────────────────────────────────────
            SettingsCard("Speech & Language") {
                DoeyDropdown(
                    label    = "Language",
                    value    = LANGUAGES.find { it.first == language }?.second ?: language,
                    options  = LANGUAGES,
                    onSelect = { language = it }
                )
                DoeyDropdown(
                    label   = "STT Mode",
                    value   = STT_MODES.find { it.first == sttMode }?.second ?: sttMode,
                    options = STT_MODES,
                    onSelect = { sttMode = it }
                )
            }

            // ── Wake Word ──────────────────────────────────────────────────────
            SettingsCard("Wake Word (Picovoice)") {
                DoeyTextField(value = picovoiceKey, onValueChange = { picovoiceKey = it },
                    label = "Picovoice Access Key",
                    visual = PasswordVisualTransformation())
                Text("Free key at console.picovoice.ai\nPlace a .ppn file in assets/ for custom wake word.",
                    color = Label3, fontSize = 12.sp)
            }

            // ── Skills ─────────────────────────────────────────────────────────
            SettingsCard("Skills (${enabledSkills.size}/${allSkills.size})") {
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
                            Text(skill.name, color = Label1, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            if (skill.description.isNotBlank())
                                Text(skill.description, color = Label3, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }

            // ── Persona ────────────────────────────────────────────────────────
            SettingsCard("Persona (SOUL)") {
                DoeyTextField(value = soul, onValueChange = { soul = it },
                    label = "Persona instructions",
                    placeholder = "e.g. You are a concise, friendly assistant…",
                    minLines = 3, maxLines = 6)
            }

            // ── Memoria personal ───────────────────────────────────────────────
            SettingsCard("Personal Memory") {
                DoeyTextField(value = personalMemory, onValueChange = { personalMemory = it },
                    label = "Personal facts (MEMORY.md)",
                    placeholder = "- [name] My name is…\n- [family] My partner is…",
                    minLines = 3, maxLines = 8)
            }

            // ── Guardar ────────────────────────────────────────────────────────
            Button(
                onClick = {
                    vm.saveSettings(provider, apiKey, model, customUrl, language, picovoiceKey,
                        enabledSkills.toList(), soul, personalMemory, maxIterations, sttMode)
                },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                Icon(Icons.Default.Save, null, tint = OnPurple)
                Spacer(Modifier.width(8.dp))
                Text("Save Settings", color = OnPurple, fontWeight = FontWeight.Bold)
            }

            if (state.settingsSaved) {
                Text("✓ Settings saved", color = Color(0xFF79FF79),
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private val LANGUAGES = listOf(
    "system" to "System default", "en-US" to "English (US)", "en-GB" to "English (UK)",
    "de-DE" to "German", "de-AT" to "German (Austria)", "es-ES" to "Spanish",
    "es-MX" to "Spanish (Mexico)", "fr-FR" to "French", "it-IT" to "Italian",
    "pt-BR" to "Portuguese (Brazil)"
)

private val STT_MODES = listOf(
    "auto" to "Auto (smart)", "offline" to "On-device only", "online" to "Cloud only"
)

@Composable
fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = Surface1) {
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
            value          = value,
            onValueChange  = {},
            readOnly       = true,
            label          = { Text(label) },
            trailingIcon   = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier       = Modifier.menuAnchor().fillMaxWidth(),
            colors         = doeyFieldColors()
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
    placeholder:   String          = "",
    visual:        VisualTransformation = VisualTransformation.None,
    minLines:      Int             = 1,
    maxLines:      Int             = 1,
    trailing:      (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value          = value,
        onValueChange  = onValueChange,
        label          = { Text(label) },
        placeholder    = { Text(placeholder, color = Color(0xFF4A4458)) },
        modifier       = Modifier.fillMaxWidth(),
        visualTransformation = visual,
        trailingIcon   = trailing,
        minLines       = minLines,
        maxLines       = maxLines,
        colors         = doeyFieldColors()
    )
}

@Composable
fun doeyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Purple,   unfocusedBorderColor = Color(0xFF4A4458),
    focusedTextColor     = Label1,   unfocusedTextColor   = Label1,
    focusedLabelColor    = Purple,   unfocusedLabelColor  = Label3,
    cursorColor          = Purple,
    focusedPlaceholderColor   = Color(0xFF4A4458),
    unfocusedPlaceholderColor = Color(0xFF4A4458)
)
