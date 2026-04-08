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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val profileStore = remember { ProfileStore(ctx) }

    var userProfile by remember { mutableStateOf(profileStore.getUserProfile()) }
    var performanceMode by remember { mutableStateOf(profileStore.getPerformanceMode()) }
    var userName by remember { mutableStateOf(profileStore.getUserName()) }
    
    // FIX BUG-1: Estado real del asistente
    var isDefaultAssistant by remember { mutableStateOf(false) }

    // Función para verificar si Doey es el asistente predeterminado
    fun checkAssistantStatus() {
        val assistant = Settings.Secure.getString(ctx.contentResolver, "assistant")
        isDefaultAssistant = assistant?.contains(ctx.packageName) == true
    }

    LaunchedEffect(Unit) {
        while(true) {
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
            title = {
                Text(
                    "Mi Perfil",
                    color = TauText1,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1)
        )

        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Sección: Asistente del Sistema (FIX BUG-1) ──────────────────────
            TauSettingsSection(title = "Asistente del Sistema", icon = Icons.Default.Assistant) {
                val statusColor = if (isDefaultAssistant) TauGreen else TauRed
                val statusText  = if (isDefaultAssistant) "Doey es tu asistente principal" else "Doey NO es el asistente principal"
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (isDefaultAssistant) Icons.Default.CheckCircle else Icons.Default.Error, 
                            null, tint = statusColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(statusText, color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
                
                Text("Para que Doey funcione al mantener presionado el botón de inicio o mediante voz, debe estar configurado como la aplicación de asistencia predeterminada.",
                    color = TauText3, fontSize = 12.sp)
                
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ctx.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TauAccent)
                ) {
                    Text("Configurar como Asistente")
                }
            }

            // ── Sección: Mis Datos (Punto 11) ──────────────────────────────────
            TauSettingsSection(title = "Mis Datos", icon = Icons.Default.Badge) {
                DoeyTextField(
                    value = userName,
                    onValueChange = { 
                        userName = it
                        profileStore.setUserName(it)
                    },
                    label = "Tu Nombre",
                    placeholder = "¿Cómo quieres que te llame Doey?"
                )
            }

            // ── Tipo de usuario ────────────────────────────────────────────────
            TauSettingsSection(title = "¿Cómo usas Doey?", icon = Icons.Default.Person) {
                Text(
                    "Selecciona el perfil que mejor te describe. Esto personaliza la interfaz y las funciones disponibles.",
                    fontSize = 12.sp,
                    color = TauText3
                )
                Spacer(Modifier.height(8.dp))

                ProfileOptionRow(
                    icon = Icons.Default.Elderly,
                    title = "Modo Básico",
                    subtitle = "Interfaz simple, comandos de voz, sin configuraciones complejas",
                    isSelected = userProfile == ProfileStore.PROFILE_BASIC,
                    accentColor = TauBlue,
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
                    accentColor = TauAccent,
                    onClick = {
                        userProfile = ProfileStore.PROFILE_ADVANCED
                        profileStore.setUserProfile(ProfileStore.PROFILE_ADVANCED)
                    }
                )
            }

            // ── Modo de rendimiento ────────────────────────────────────────────
            TauSettingsSection(title = "Rendimiento", icon = Icons.Default.Speed) {
                Text(
                    "Optimiza Doey según las capacidades de tu teléfono.",
                    fontSize = 12.sp,
                    color = TauText3
                )
                Spacer(Modifier.height(8.dp))

                ProfileOptionRow(
                    icon = Icons.Default.BatteryAlert,
                    title = "Bajo Consumo",
                    subtitle = "Sin animaciones, menos procesos. Ideal para gama baja.",
                    isSelected = performanceMode == ProfileStore.PERF_LOW_POWER,
                    accentColor = TauOrange,
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
                    accentColor = TauBlue,
                    onClick = {
                        performanceMode = ProfileStore.PERF_HIGH
                        profileStore.setPerformanceMode(ProfileStore.PERF_HIGH)
                    }
                )
            }

            Spacer(Modifier.height(40.dp))
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
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.15f) else TauSurface2,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, accentColor) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accentColor else TauSurface3),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = if (isSelected) Color.White else TauText3,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) accentColor else TauText1,
                    fontSize = 15.sp
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = TauText3,
                    lineHeight = 16.sp
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    TauSettingsSection(title = title, icon = Icons.Default.Settings, content = content)
}
