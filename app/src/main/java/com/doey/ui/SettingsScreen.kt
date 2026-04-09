package com.doey.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
fun SettingsScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val settings = remember { vm.getSettings() }
    val scope = rememberCoroutineScope()

    var provider by remember { mutableStateOf("gemini") }
    var apiKey by remember { mutableStateOf("") }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var model by remember { mutableStateOf("") }
    var showApiSaved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        provider = settings.getProvider()
        apiKey = settings.getApiKey(provider)
        model = settings.getModel()
    }

    LaunchedEffect(provider) {
        apiKey = settings.getApiKey(provider)
    }

    Column(
        Modifier.fillMaxSize().background(TauBg).verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("IA y Configuración", fontWeight = FontWeight.Bold, color = TauText1) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1)
        )

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            
            // SECCIÓN IA (Siempre visible)
            TauSettingsSection(title = "Cerebro de Doey", icon = Icons.Default.Psychology) {
                Text("Proveedor de IA", fontSize = 12.sp, color = TauText3)
                
                val providers = listOf("gemini" to "Gemini", "openai" to "ChatGPT", "groq" to "Groq", "openrouter" to "OpenRouter")
                providers.forEach { (id, label) ->
                    val isSelected = provider == id
                    Surface(
                        onClick = { provider = id },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) TauAccent.copy(alpha = 0.1f) else TauSurface2,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, TauAccent) else null,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isSelected, onClick = { provider = id }, colors = RadioButtonDefaults.colors(selectedColor = TauAccent))
                            Text(label, color = if (isSelected) TauAccent else TauText1, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("API Key", fontSize = 12.sp, color = TauText3)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TauText3)
                        }
                    },
                    colors = doeyFieldColors()
                )

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            settings.setApiKey(provider, apiKey)
                            settings.setProvider(provider)
                            settings.setModel(model)
                            vm.saveSettings(
                                provider = provider, apiKey = apiKey, model = model,
                                customUrl = "", language = "system", wakePhrase = "hey doey",
                                enabledSkills = emptyList(), soul = "", personalMemory = "",
                                maxIterations = 10, sttMode = "auto", expertMode = true
                            )
                            showApiSaved = true
                            delay(2000)
                            showApiSaved = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TauAccent)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Probar conexión y Guardar")
                }

                AnimatedVisibility(visible = showApiSaved) {
                    Text("¡Configuración guardada!", color = TauGreen, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }

            // SECCIÓN PERMISOS
            TauSettingsSection(title = "Permisos del Sistema", icon = Icons.Default.Lock) {
                PermissionItem(Icons.Default.Mic, "Micrófono")
                PermissionItem(Icons.Default.Notifications, "Notificaciones")
                PermissionItem(Icons.Default.Accessibility, "Accesibilidad")
            }
        }
    }
}

@Composable
fun PermissionItem(icon: ImageVector, label: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = TauSurface2
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = TauAccentLight, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
            Text(label, color = TauText1, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = TauText3)
        }
    }
}

@Composable
fun TauSettingsSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(icon, null, tint = TauAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = TauAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = TauSurface1,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) { content() }
        }
    }
}
