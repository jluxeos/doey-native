package com.doey.ui

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.agent.SettingsStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onProfileChanged: () -> Unit = {}) {
    val ctx = LocalContext.current
    val settings = remember { vm.getSettings() }
    val scope = rememberCoroutineScope()
    
    var provider by remember { mutableStateOf("gemini") }
    var theme by remember { mutableStateOf("tau") }
    var bubblePosition by remember { mutableStateOf("right") }
    
    LaunchedEffect(Unit) {
        provider = settings.getProvider()
        theme = settings.getTheme()
        bubblePosition = settings.getBubblePosition()
    }

    val providers = listOf("gemini", "groq", "openrouter")
    val themes = listOf("tau", "blue", "green", "orange", "red")
    val themeColors = mapOf(
        "tau" to TauAccent,
        "blue" to TauBlue,
        "green" to TauGreen,
        "orange" to TauOrange,
        "red" to TauRed
    )

    Column(
        Modifier.fillMaxSize().background(TauBg).verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("Ajustes", fontWeight = FontWeight.Bold, color = TauText1) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1)
        )

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            
            // ── Sección: Inteligencia Artificial (Punto 6) ─────────────────────
            TauSettingsSection(title = "Cerebro de Doey", icon = Icons.Default.Psychology) {
                Text("Selecciona el proveedor de IA que Doey utilizará para procesar tus peticiones.", 
                    fontSize = 12.sp, color = TauText3)
                
                providers.forEach { p ->
                    val isSelected = provider == p
                    Surface(
                        onClick = {
                            provider = p
                            scope.launch { settings.setProvider(p) }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) TauAccent.copy(alpha = 0.1f) else TauSurface2,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, TauAccent) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                when(p) {
                                    "gemini" -> Icons.Default.AutoAwesome
                                    "groq" -> Icons.Default.Bolt
                                    else -> Icons.Default.Cloud
                                },
                                null, tint = if (isSelected) TauAccent else TauText3
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(p.replaceFirstChar { it.uppercase() }, 
                                    fontWeight = FontWeight.Bold, color = if (isSelected) TauAccent else TauText1)
                                Text(
                                    when(p) {
                                        "gemini" -> "2.5 Flash / Flashlite"
                                        "groq" -> "Llama 3 (Ultra rápido)"
                                        else -> "OpenRouter Free"
                                    },
                                    fontSize = 11.sp, color = TauText3
                                )
                            }
                        }
                    }
                }
            }

            // ── Sección: Apariencia (Punto 4: Selector de temas funcional) ─────
            TauSettingsSection(title = "Personalización", icon = Icons.Default.Palette) {
                Text("Tema de color del sistema", fontSize = 12.sp, color = TauText3)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    themes.forEach { t ->
                        val isSelected = theme == t
                        val color = themeColors[t] ?: TauAccent
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
                                    scope.launch { settings.setTheme(t) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                // Punto 4: Ajuste de posición de la burbuja
                Text("Posición de la burbuja", fontSize = 12.sp, color = TauText3)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("left", "right").forEach { pos ->
                        val isSelected = bubblePosition == pos
                        Surface(
                            onClick = {
                                bubblePosition = pos
                                scope.launch { settings.setBubblePosition(pos) }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) TauAccent.copy(alpha = 0.2f) else TauSurface2,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, TauAccent) else null
                        ) {
                            Text(if (pos == "left") "Izquierda" else "Derecha", 
                                fontSize = 12.sp, color = if (isSelected) TauAccentLight else TauText2,
                                modifier = Modifier.padding(vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
            shape = RoundedCornerShape(16.dp),
            color = TauSurface1,
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
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = doeyFieldColors()
    )
}

@Composable
fun TauSwitchRow(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(TauSurface2), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = TauAccentLight, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = TauText1, fontSize = 15.sp)
            Text(subtitle, fontSize = 12.sp, color = TauText3)
        }
        Switch(checked = checked, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = TauAccent))
    }
}
