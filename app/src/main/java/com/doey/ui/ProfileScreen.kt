package com.doey.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.agent.ProfileStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val profileStore = remember { ProfileStore(ctx) }

    var userProfile by remember { mutableStateOf(profileStore.getUserProfile()) }
    var performanceMode by remember { mutableStateOf(profileStore.getPerformanceMode()) }
    var overlayEnabled by remember { mutableStateOf(profileStore.isOverlayEnabled()) }
    var overlayAutoShow by remember { mutableStateOf(profileStore.isOverlayAutoShow()) }
    var showAssistantDialog by remember { mutableStateOf(false) }

    val canDrawOverlays = remember { Settings.canDrawOverlays(ctx) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Surface0Light)
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Text(
                    "Mi Perfil",
                    color = Label1Light,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1Light)
        )

        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Tipo de usuario ────────────────────────────────────────────────
            SettingsCard("¿Cómo usas Doey?") {
                Text(
                    "Selecciona el perfil que mejor te describe. Esto personaliza la interfaz y las funciones disponibles.",
                    fontSize = 13.sp,
                    color = Label3Light,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))

                ProfileOptionRow(
                    icon = Icons.Default.Elderly,
                    title = "Modo Básico",
                    subtitle = "Interfaz simple, comandos de voz, sin configuraciones complejas",
                    isSelected = userProfile == ProfileStore.PROFILE_BASIC,
                    accentColor = Color(0xFF4CAF50),
                    onClick = {
                        userProfile = ProfileStore.PROFILE_BASIC
                        profileStore.setUserProfile(ProfileStore.PROFILE_BASIC)
                    }
                )

                Spacer(Modifier.height(8.dp))

                ProfileOptionRow(
                    icon = Icons.Default.Code,
                    title = "Modo Avanzado",
                    subtitle = "Skills, logs, automatizaciones, configuración completa",
                    isSelected = userProfile == ProfileStore.PROFILE_ADVANCED,
                    accentColor = Purple,
                    onClick = {
                        userProfile = ProfileStore.PROFILE_ADVANCED
                        profileStore.setUserProfile(ProfileStore.PROFILE_ADVANCED)
                    }
                )
            }

            // ── Modo de rendimiento ────────────────────────────────────────────
            SettingsCard("Rendimiento del dispositivo") {
                Text(
                    "Optimiza Doey según las capacidades de tu teléfono.",
                    fontSize = 13.sp,
                    color = Label3Light,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))

                ProfileOptionRow(
                    icon = Icons.Default.BatteryAlert,
                    title = "Bajo Consumo",
                    subtitle = "Sin animaciones, menos procesos. Ideal para gama baja.",
                    isSelected = performanceMode == ProfileStore.PERF_LOW_POWER,
                    accentColor = Color(0xFFFFB300),
                    onClick = {
                        performanceMode = ProfileStore.PERF_LOW_POWER
                        profileStore.setPerformanceMode(ProfileStore.PERF_LOW_POWER)
                    }
                )

                Spacer(Modifier.height(8.dp))

                ProfileOptionRow(
                    icon = Icons.Default.Speed,
                    title = "Alto Rendimiento",
                    subtitle = "Animaciones completas, funciones avanzadas, mejor experiencia.",
                    isSelected = performanceMode == ProfileStore.PERF_HIGH,
                    accentColor = Color(0xFF00B0FF),
                    onClick = {
                        performanceMode = ProfileStore.PERF_HIGH
                        profileStore.setPerformanceMode(ProfileStore.PERF_HIGH)
                    }
                )
            }

            // ── Burbuja flotante ───────────────────────────────────────────────
            SettingsCard("Burbuja flotante") {
                if (!canDrawOverlays) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFF3E0)
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Permiso de superposición requerido",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF5D4037)
                                )
                                Text(
                                    "Para mostrar la burbuja sobre otras apps.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF795548)
                                )
                                Spacer(Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        try {
                                            ctx.startActivity(
                                                Intent(
                                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    android.net.Uri.parse("package:${ctx.packageName}")
                                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                            )
                                        } catch (_: Exception) {}
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Activar permiso", color = Purple, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Activar burbuja flotante",
                            fontWeight = FontWeight.SemiBold,
                            color = Label1Light,
                            fontSize = 15.sp
                        )
                        Text(
                            "Muestra Doey como burbuja sobre otras apps",
                            fontSize = 12.sp,
                            color = Label3Light
                        )
                    }
                    Switch(
                        checked = overlayEnabled && canDrawOverlays,
                        onCheckedChange = { enabled ->
                            if (enabled && !canDrawOverlays) {
                                try {
                                    ctx.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            android.net.Uri.parse("package:${ctx.packageName}")
                                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                    )
                                } catch (_: Exception) {}
                            } else {
                                overlayEnabled = enabled
                                profileStore.setOverlayEnabled(enabled)
                                if (enabled) {
                                    try {
                                        ctx.startForegroundService(
                                            Intent(ctx, com.doey.services.DoeyOverlayService::class.java).apply {
                                                action = com.doey.services.DoeyOverlayService.ACTION_SHOW
                                            }
                                        )
                                    } catch (_: Exception) {}
                                } else {
                                    try {
                                        ctx.stopService(Intent(ctx, com.doey.services.DoeyOverlayService::class.java))
                                    } catch (_: Exception) {}
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Purple)
                    )
                }

                if (overlayEnabled && canDrawOverlays) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Mostrar automáticamente",
                                fontWeight = FontWeight.Medium,
                                color = Label1Light,
                                fontSize = 14.sp
                            )
                            Text(
                                "Aparece al ejecutar acciones en otras apps",
                                fontSize = 12.sp,
                                color = Label3Light
                            )
                        }
                        Switch(
                            checked = overlayAutoShow,
                            onCheckedChange = {
                                overlayAutoShow = it
                                profileStore.setOverlayAutoShow(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Purple)
                        )
                    }
                }
            }

            // ── Asistente predeterminado ───────────────────────────────────────
            SettingsCard("Asistente del sistema") {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Purple.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Assistant,
                            null,
                            tint = Purple,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Configurar como asistente predeterminado",
                            fontWeight = FontWeight.SemiBold,
                            color = Label1Light,
                            fontSize = 15.sp
                        )
                        Text(
                            "Invoca Doey con el botón home largo o \"Hey Google\"",
                            fontSize = 12.sp,
                            color = Label3Light,
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        try {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            } else {
                                Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            }
                            ctx.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                ctx.startActivity(
                                    Intent(Settings.ACTION_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            } catch (_: Exception) {}
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Abrir ajustes de asistente", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "En Ajustes → Aplicaciones → Asistente digital, selecciona Doey como asistente predeterminado.",
                    fontSize = 12.sp,
                    color = Label3Light,
                    lineHeight = 16.sp
                )
            }

            // ── Información del perfil ─────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Surface1Light,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            tint = Purple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Perfil actual",
                            fontWeight = FontWeight.Bold,
                            color = Label1Light,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))

                    ProfileInfoRow(
                        "Tipo de usuario",
                        if (userProfile == ProfileStore.PROFILE_BASIC) "Básico (Modo simple)" else "Avanzado (Modo completo)"
                    )
                    ProfileInfoRow(
                        "Rendimiento",
                        if (performanceMode == ProfileStore.PERF_LOW_POWER) "Bajo consumo" else "Alto rendimiento"
                    )
                    ProfileInfoRow(
                        "Burbuja flotante",
                        if (overlayEnabled && canDrawOverlays) "Activada" else "Desactivada"
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else Label3Light.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.08f) else Color.Transparent
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Label3Light.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = if (isSelected) accentColor else Label3Light,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) accentColor else Label1Light,
                    fontSize = 15.sp
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = Label3Light,
                    lineHeight = 16.sp
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Label3Light)
        Text(value, fontSize = 13.sp, color = Label1Light, fontWeight = FontWeight.Medium)
    }
}
