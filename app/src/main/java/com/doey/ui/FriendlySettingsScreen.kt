package com.doey.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.agent.SettingsStore
import com.doey.services.FriendlyModeService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FriendlySettingsScreen(vm: MainViewModel) {
    val ctx      = LocalContext.current
    val settings = remember { SettingsStore(ctx) }
    val scope    = rememberCoroutineScope()

    var friendlyEnabled by remember { mutableStateOf(true) }
    var contextRead     by remember { mutableStateOf(true) }
    var autoStart       by remember { mutableStateOf(false) }
    // Fix #5: valores iniciales desde SettingsStore (no hardcodeados)
    var barHeight       by remember { mutableStateOf(72f) }
    var barOpacity      by remember { mutableStateOf(0.95f) }
    var showSaved       by remember { mutableStateOf(false) }
    val isRunning       = FriendlyModeService.isRunning

    // Fix #5: cargar todos los valores persistidos al iniciar
    LaunchedEffect(Unit) {
        friendlyEnabled = settings.getFriendlyModeEnabled()
        contextRead     = settings.getFriendlyContextRead()
        autoStart       = settings.getAutoStartFriendly()
        barHeight       = settings.getFriendlyBarHeight()
        barOpacity      = settings.getFriendlyBarOpacity()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TauBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF1B5E20).copy(alpha = 0.8f),
                                Color(0xFF2E7D32).copy(alpha = 0.6f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Spa, null,
                            tint     = Color(0xFF4CAF50),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Modo Friendly", color = TauText1, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Barra asistente siempre visible", color = TauText2, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isRunning) Color(0xFF4CAF50).copy(0.2f) else TauSurface3
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isRunning) Color(0xFF4CAF50) else TauText3,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isRunning) "Servicio activo" else "Servicio inactivo",
                                color    = if (isRunning) Color(0xFF4CAF50) else TauText3,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // ── Activación ──────────────────────────────────────────────────────
        FriendlySection(title = "Activación", icon = Icons.Default.PowerSettingsNew) {
            FriendlySwitchRow(
                title    = "Activar Modo Friendly",
                subtitle = "Muestra la barra asistente en la parte inferior",
                checked  = friendlyEnabled,
                onToggle = {
                    friendlyEnabled = it
                    if (it) {
                        val intent = Intent(ctx, FriendlyModeService::class.java).apply {
                            action = FriendlyModeService.ACTION_SHOW
                            putExtra(FriendlyModeService.EXTRA_CONTEXT_APP, "Ajustes")
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            ctx.startForegroundService(intent)
                        } else {
                            ctx.startService(intent)
                        }
                    } else {
                        ctx.stopService(Intent(ctx, FriendlyModeService::class.java))
                    }
                }
            )
            FriendlySwitchRow(
                title    = "Auto-iniciar al arrancar",
                subtitle = "Inicia el Modo Friendly cuando enciendas el dispositivo",
                checked  = autoStart,
                onToggle = { autoStart = it }
            )
        }

        // ── Comportamiento ──────────────────────────────────────────────────
        FriendlySection(title = "Comportamiento", icon = Icons.Default.Tune) {
            FriendlySwitchRow(
                title    = "Leer contexto de app activa",
                subtitle = "Doey lee el contenido de la pantalla al invocarla",
                checked  = contextRead,
                onToggle = { contextRead = it }
            )
        }

        // ── Apariencia — Fix #5: sliders ahora persisten ────────────────────
        FriendlySection(title = "Apariencia", icon = Icons.Default.Palette) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Altura de la barra", color = TauText2, fontSize = 13.sp)
                    Text("${barHeight.toInt()} dp", color = TauText3, fontSize = 12.sp)
                }
                Slider(
                    value         = barHeight,
                    onValueChange = { barHeight = it },
                    valueRange    = 56f..96f,
                    colors        = SliderDefaults.colors(
                        thumbColor       = Color(0xFF4CAF50),
                        activeTrackColor = Color(0xFF4CAF50)
                    )
                )
            }

            Spacer(Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Opacidad del fondo", color = TauText2, fontSize = 13.sp)
                    Text("${(barOpacity * 100).toInt()}%", color = TauText3, fontSize = 12.sp)
                }
                Slider(
                    value         = barOpacity,
                    onValueChange = { barOpacity = it },
                    valueRange    = 0.5f..1.0f,
                    colors        = SliderDefaults.colors(
                        thumbColor       = Color(0xFF4CAF50),
                        activeTrackColor = Color(0xFF4CAF50)
                    )
                )
            }
        }

        // ── Guardar ─────────────────────────────────────────────────────────
        Button(
            onClick = {
                scope.launch {
                    settings.setFriendlyModeEnabled(friendlyEnabled)
                    settings.setFriendlyContextRead(contextRead)
                    settings.setAutoStartFriendly(autoStart)
                    // Fix #5: persistir apariencia
                    settings.setFriendlyBarHeight(barHeight)
                    settings.setFriendlyBarOpacity(barOpacity)
                    showSaved = true
                    delay(2500)
                    showSaved = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(8.dp))
            Text("Guardar Ajustes Friendly", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        AnimatedVisibility(visible = showSaved) {
            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = Color(0xFF4CAF50).copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("¡Ajustes del Modo Friendly guardados!", color = Color(0xFF4CAF50), fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FriendlySection(
    title   : String,
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    content : @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = TauSurface1,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = TauText1, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(color = TauSurface3)
            content()
        }
    }
}

@Composable
private fun FriendlySwitchRow(
    title    : String,
    subtitle : String,
    checked  : Boolean,
    onToggle : (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title,    color = TauText1, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TauText3, fontSize = 12.sp)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4CAF50)
            )
        )
    }
}
