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

    var provider       by remember { mutableStateOf("gemini") }
    var apiKey         by remember { mutableStateOf("") }
    var apiKeyVisible  by remember { mutableStateOf(false) }
    var theme          by remember { mutableStateOf("tau") }
    var bubblePosition by remember { mutableStateOf("right") }
    var overlayEnabled by remember { mutableStateOf(false) }
    var showApiSaved   by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        provider       = settings.getProvider()
        apiKey         = settings.getApiKey(provider)
        theme          = settings.getTheme()
        bubblePosition = settings.getBubblePosition()
        overlayEnabled = settings.getOverlayEnabled()
    }

    // Fix #4: al cambiar proveedor carga su API key guardada
    LaunchedEffect(provider) {
        apiKey = settings.getApiKey(provider)
    }

    val providers = listOf("gemini", "groq", "openrouter")
    val themes    = listOf("tau", "blue", "green", "orange", "red")
    val themeColors = mapOf(
        "tau"    to TauAccent,
        "blue"   to TauBlue,
        "green"  to TauGreen,
        "orange" to TauOrange,
        "red"    to TauRed
    )

    Column(
        Modifier.fillMaxSize().background(TauBg).verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title  = { Text("Ajustes", fontWeight = FontWeight.Bold, color = TauText1) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1)
        )

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── Cerebro de Doey ───────────────────────────────────────────────
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
                                    "gemini" -> Icons.Default.AutoAwesome
                                    "groq"   -> Icons.Default.Bolt
                                    else     -> Icons.Default.Cloud
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
                                        "gemini" -> "2.5 Flash / Flashlite"
                                        "groq"   -> "Llama 3 (Ultra rápido)"
                                        else     -> "OpenRouter Free"
                                    },
                                    fontSize = 11.sp, color = TauText3
                                )
                            }
                        }
                    }
                }

                // Fix #4: campo API key del proveedor activo
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

                Button(
                    onClick = {
                        settings.setApiKey(provider, apiKey)
                        scope.launch {
                            settings.setProvider(provider)
                            showApiSaved = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = TauAccent)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar API Key", fontWeight = FontWeight.Bold)
                }

                AnimatedVisibility(visible = showApiSaved) {
                    LaunchedEffect(Unit) {
                        delay(2000)
                        showApiSaved = false
                    }
                    Surface(
                        shape    = RoundedCornerShape(8.dp),
                        color    = TauGreen.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = TauGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("¡API Key guardada!", color = TauGreen, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── Personalización ───────────────────────────────────────────────
            TauSettingsSection(title = "Personalización", icon = Icons.Default.Palette) {
                // Fix #2: tema notifica al DoeyApp para recomponer colores
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
                                        onProfileChanged() // propaga al DoeyApp → recompone tema
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
                                fontSize  = 12.sp,
                                color     = if (isSelected) TauAccentLight else TauText2,
                                modifier  = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── Burbuja flotante — Fix #3 ─────────────────────────────────────
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
                // Aviso si falta permiso de superposición
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
