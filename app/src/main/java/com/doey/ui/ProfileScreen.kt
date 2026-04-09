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

    // Estado real del asistente predeterminado
    var isDefaultAssistant by remember { mutableStateOf(false) }

    fun checkAssistantStatus() {
        val assistant = Settings.Secure.getString(ctx.contentResolver, "assistant")
        isDefaultAssistant = assistant?.contains(ctx.packageName) == true
    }

    LaunchedEffect(Unit) {
        while (true) {
            checkAssistantStatus()
            delay(2000)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(TauBg)
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title  = { Text("Mi Perfil", color = TauText1, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1)
        )

        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Asistente del Sistema ──────────────────────────────────────────
            TauSettingsSection(title = "Asistente del Sistema", icon = Icons.Default.Assistant) {
                val statusColor = if (isDefaultAssistant) TauGreen else TauRed
                val statusText  = if (isDefaultAssistant)
                    "Doey es tu asistente principal"
                else
                    "Doey NO es el asistente principal"

                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = statusColor.copy(alpha = 0.1f),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isDefaultAssistant) Icons.Default.CheckCircle else Icons.Default.Error,
                            null, tint = statusColor, modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(statusText, color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Text(
                    "Para que Doey funcione al mantener presionado el botón de inicio o mediante voz, debe estar configurado como la aplicación de asistencia predeterminada.",
                    color = TauText3, fontSize = 12.sp
                )

                GlassButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        ctx.startActivity(intent)
                    }
                ) {
                    Icon(Icons.Default.Assistant, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Configurar como Asistente")
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
                Text(
                    "Doey usará este nombre para personalizar sus respuestas.",
                    color = TauText3, fontSize = 11.sp
                )
            }

            // ── Tipo de usuario ────────────────────────────────────────────────
            TauSettingsSection(title = "¿Cómo usas Doey?", icon = Icons.Default.Person) {
                Text(
                    "Selecciona el perfil que mejor te describe. Esto personaliza la interfaz y las funciones disponibles.",
                    fontSize = 12.sp, color = TauText3
                )
                Spacer(Modifier.height(8.dp))

                ProfileOptionRow(
                    icon       = Icons.Default.Elderly,
                    title      = "Modo Básico",
                    subtitle   = "Interfaz simple, comandos de voz, sin configuraciones complejas",
                    isSelected = userProfile == ProfileStore.PROFILE_BASIC,
                    accentColor = TauBlue,
                    onClick    = {
                        userProfile = ProfileStore.PROFILE_BASIC
                        profileStore.setUserProfile(ProfileStore.PROFILE_BASIC)
                    }
                )
                Spacer(Modifier.height(8.dp))
                ProfileOptionRow(
                    icon       = Icons.Default.Code,
                    title      = "Modo Avanzado",
                    subtitle   = "Skills, logs, automatizaciones, configuración completa",
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
                Text(
                    "Optimiza Doey según las capacidades de tu teléfono. Los cambios se aplican al reiniciar la IA.",
                    fontSize = 12.sp, color = TauText3
                )
                Spacer(Modifier.height(8.dp))

                ProfileOptionRow(
                    icon       = Icons.Default.BatteryAlert,
                    title      = "Bajo Consumo",
                    subtitle   = "Máx. 6 iteraciones · Historial reducido (12 msgs) · Menos tokens",
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
                                soul          = settings.getSoul().first(),
                                personalMemory = settings.getPersonalMemory().first(),
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
                Spacer(Modifier.height(8.dp))
                ProfileOptionRow(
                    icon       = Icons.Default.Speed,
                    title      = "Alto Rendimiento",
                    subtitle   = "Máx. 10 iteraciones · Historial completo (20 msgs) · Experiencia completa",
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
                                soul          = settings.getSoul().first(),
                                personalMemory = settings.getPersonalMemory().first(),
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
                    Surface(
                        shape    = RoundedCornerShape(8.dp),
                        color    = TauGreen.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Ajustes de rendimiento aplicados",
                            color = TauGreen,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // ── Permisos del Sistema ──────────────────────────────────────────
            TauSettingsSection(title = "Permisos del Sistema", icon = Icons.Default.Lock) {
                Text(
                    "Doey necesita estos permisos para funcionar correctamente.",
                    fontSize = 12.sp, color = TauText3
                )
                Spacer(Modifier.height(8.dp))

                PermissionRow(
                    title    = "Accesibilidad",
                    subtitle = "Para leer y controlar otras apps",
                    icon     = Icons.Default.Accessibility,
                    onGrant  = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        ctx.startActivity(intent)
                    }
                )
                PermissionRow(
                    title    = "Notificaciones",
                    subtitle = "Para leer y responder notificaciones",
                    icon     = Icons.Default.Notifications,
                    onGrant  = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        ctx.startActivity(intent)
                    }
                )
                PermissionRow(
                    title    = "Superposición",
                    subtitle = "Para la burbuja flotante",
                    icon     = Icons.Default.BubbleChart,
                    onGrant  = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${ctx.packageName}")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        ctx.startActivity(intent)
                    }
                )
                PermissionRow(
                    title    = "Batería sin restricciones",
                    subtitle = "Para que Doey no sea cerrado en segundo plano",
                    icon     = Icons.Default.BatteryFull,
                    onGrant  = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            ctx.startActivity(intent)
                        }
                    }
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PermissionRow(
    title: String, subtitle: String, icon: ImageVector, onGrant: () -> Unit
) {
    Surface(
        onClick  = onGrant,
        shape    = RoundedCornerShape(12.dp),
        color    = Color.White.copy(alpha = 0.05f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = TauAccentLight, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title,    color = TauText1, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = TauText3, fontSize = 11.sp)
            }
            Icon(Icons.Default.OpenInNew, null, tint = TauText3, modifier = Modifier.size(16.dp))
        }
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
    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(16.dp),
        color    = if (isSelected) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
        border   = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, accentColor) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (isSelected) accentColor else TauSurface3),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    tint     = if (isSelected) Color.White else TauText3,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title,    fontWeight = FontWeight.Bold, color = if (isSelected) accentColor else TauText1, fontSize = 15.sp)
                Text(subtitle, fontSize = 12.sp, color = TauText3, lineHeight = 16.sp)
            }
            if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = accentColor, modifier = Modifier.size(24.dp))
        }
    }
}
