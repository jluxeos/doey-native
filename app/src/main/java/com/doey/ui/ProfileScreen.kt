package com.doey.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.agent.ProfileStore
import com.doey.agent.SettingsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: MainViewModel) {
    val ctx          = LocalContext.current
    val profileStore = remember { ProfileStore(ctx) }
    val settings     = remember { vm.getSettings() }
    val scope        = rememberCoroutineScope()

    var userProfile     by remember { mutableStateOf(profileStore.getUserProfile()) }
    var performanceMode by remember { mutableStateOf(profileStore.getPerformanceMode()) }
    var userName        by remember { mutableStateOf(profileStore.getUserName()) }
    var showPerfSaved   by remember { mutableStateOf(false) }
    var theme           by remember { mutableStateOf("DeepSeaBlue") }

    // Estado real del asistente predeterminado
    var isDefaultAssistant by remember { mutableStateOf(false) }

    fun checkAssistantStatus() {
        val assistant = Settings.Secure.getString(ctx.contentResolver, "assistant")
        isDefaultAssistant = assistant?.contains(ctx.packageName) == true
    }

    LaunchedEffect(Unit) {
        theme = settings.getTheme()
        updateGlassTheme(theme)
        while (true) {
            checkAssistantStatus()
            delay(2000)
        }
    }

    Box(Modifier.fillMaxSize()) {
        GlassBackground(accentColor = TauAccent)

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            TopAppBar(
                title  = { Text("Mi Perfil", color = TauText1, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {

                // ── Asistente del Sistema ──────────────────────────────────────────
                TauSettingsSection(title = "Asistente del Sistema", icon = Icons.Default.Assistant) {
                    val statusColor = if (isDefaultAssistant) TauGreen else TauRed
                    val statusText  = if (isDefaultAssistant) "Doey es tu asistente principal" else "Doey NO es el asistente principal"

                    Surface(
                        shape    = RoundedCornerShape(16.dp),
                        color    = statusColor.copy(alpha = 0.1f),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isDefaultAssistant) Icons.Default.CheckCircle else Icons.Default.Error,
                                null, tint = statusColor, modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(statusText, color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Para que Doey funcione al mantener presionado el botón de inicio o mediante voz, debe estar configurado como la aplicación de asistencia predeterminada.",
                        color = TauText3, fontSize = 12.sp
                    )

                    Spacer(Modifier.height(12.dp))
                    GlassButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            ctx.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Assistant, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Configurar como Asistente", fontWeight = FontWeight.Bold)
                    }
                }

                // ── Mis Datos ──────────────────────────────────────────────────────
                TauSettingsSection(title = "Mis Datos", icon = Icons.Default.Badge) {
                    DoeyTextField(
                        value         = userName,
                        onValueChange = {
                            userName = it
                            profileStore.setUserName(it)
                        },
                        label       = "Tu Nombre",
                        placeholder = "¿Cómo quieres que te llame Doey?"
                    )
                }

                // ── Tipo de usuario ────────────────────────────────────────────────
                TauSettingsSection(title = "¿Cómo usas Doey?", icon = Icons.Default.Person) {
                    ProfileOptionRow(
                        icon       = Icons.Default.Elderly,
                        title      = "Modo Básico",
                        subtitle   = "Interfaz simple, comandos de voz",
                        isSelected = userProfile == ProfileStore.PROFILE_BASIC,
                        accentColor = TauBlue,
                        onClick    = {
                            userProfile = ProfileStore.PROFILE_BASIC
                            profileStore.setUserProfile(ProfileStore.PROFILE_BASIC)
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    ProfileOptionRow(
                        icon       = Icons.Default.Code,
                        title      = "Modo Avanzado",
                        subtitle   = "Skills, logs, automatizaciones",
                        isSelected = userProfile == ProfileStore.PROFILE_ADVANCED,
                        accentColor = TauAccent,
                        onClick    = {
                            userProfile = ProfileStore.PROFILE_ADVANCED
                            profileStore.setUserProfile(ProfileStore.PROFILE_ADVANCED)
                        }
                    )
                }

                // ── Modo de rendimiento ───────────────────────────────────────────
                TauSettingsSection(title = "Rendimiento", icon = Icons.Default.Speed) {
                    ProfileOptionRow(
                        icon       = Icons.Default.BatteryAlert,
                        title      = "Bajo Consumo",
                        subtitle   = "Máx. 6 iteraciones · Historial reducido",
                        isSelected = performanceMode == ProfileStore.PERF_LOW_POWER,
                        accentColor = TauOrange,
                        onClick    = {
                            performanceMode = ProfileStore.PERF_LOW_POWER
                            profileStore.setPerformanceMode(ProfileStore.PERF_LOW_POWER)
                            scope.launch {
                                settings.setMaxIterations(6)
                                settings.setMaxHistoryMessages(12)
                                settings.setTokenOptimizerEnabled(true)
                                settings.setHistoryCompressionEnabled(true)
                                vm.saveSettings(
                                    provider      = settings.getProvider(),
                                    apiKey        = settings.getApiKey(settings.getProvider()),
                                    model         = settings.getModel(),
                                    customUrl     = settings.getCustomModelUrl(),
                                    language      = settings.getLanguage(),
                                    wakePhrase    = settings.getWakePhrase(),
                                    enabledSkills = settings.getEnabledSkillsList(),
                                    soul          = settings.getSoul(),
                                    personalMemory = settings.getPersonalMemory(),
                                    maxIterations = 6,
                                    sttMode       = settings.getSttMode(),
                                    expertMode    = settings.getExpertMode()
                                )
                                showPerfSaved = true
                                delay(2500)
                                showPerfSaved = false
                            }
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    ProfileOptionRow(
                        icon       = Icons.Default.Speed,
                        title      = "Alto Rendimiento",
                        subtitle   = "Máx. 10 iteraciones · Historial completo",
                        isSelected = performanceMode == ProfileStore.PERF_HIGH,
                        accentColor = TauBlue,
                        onClick    = {
                            performanceMode = ProfileStore.PERF_HIGH
                            profileStore.setPerformanceMode(ProfileStore.PERF_HIGH)
                            scope.launch {
                                settings.setMaxIterations(10)
                                settings.setMaxHistoryMessages(20)
                                vm.saveSettings(
                                    provider      = settings.getProvider(),
                                    apiKey        = settings.getApiKey(settings.getProvider()),
                                    model         = settings.getModel(),
                                    customUrl     = settings.getCustomModelUrl(),
                                    language      = settings.getLanguage(),
                                    wakePhrase    = settings.getWakePhrase(),
                                    enabledSkills = settings.getEnabledSkillsList(),
                                    soul          = settings.getSoul(),
                                    personalMemory = settings.getPersonalMemory(),
                                    maxIterations = 10,
                                    sttMode       = settings.getSttMode(),
                                    expertMode    = settings.getExpertMode()
                                )
                                showPerfSaved = true
                                delay(2500)
                                showPerfSaved = false
                            }
                        }
                    )

                    AnimatedVisibility(visible = showPerfSaved) {
                        Text(
                            "Ajustes de rendimiento aplicados",
                            color = TauGreen,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                }

                // ── Permisos del Sistema ──────────────────────────────────────────
                TauSettingsSection(title = "Permisos del Sistema", icon = Icons.Default.Lock) {
                    PermissionRow(title = "Accesibilidad", icon = Icons.Default.Accessibility) {
                        ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    }
                    PermissionRow(title = "Notificaciones", icon = Icons.Default.Notifications) {
                        ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    }
                    PermissionRow(title = "Superposición", icon = Icons.Default.BubbleChart) {
                        ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${ctx.packageName}")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, icon: ImageVector, onGrant: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onGrant() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = TauAccent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(title, color = TauText1, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Default.OpenInNew, null, tint = TauText3, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun ProfileOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
            .border(if (isSelected) 1.dp else 0.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(if (isSelected) accentColor else Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (isSelected) Color.White else TauText3, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = if (isSelected) accentColor else TauText1, fontSize = 15.sp)
            Text(subtitle, fontSize = 12.sp, color = TauText3)
        }
        if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = accentColor, modifier = Modifier.size(20.dp))
    }
}
