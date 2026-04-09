package com.doey.ui

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.agent.ProfileStore
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val profileStore = remember { ProfileStore(ctx) }
    
    var userName by remember { mutableStateOf(profileStore.getUserName()) }
    var userAge by remember { mutableStateOf(profileStore.getUserAge()) }
    var usageLevel by remember { mutableStateOf(profileStore.getUsageLevel()) }
    var userProfile by remember { mutableStateOf(profileStore.getUserProfile()) }

    Column(
        Modifier.fillMaxSize().background(TauBg).verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("Mi Perfil", color = TauText1, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1)
        )

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            
            // DATOS PERSONALES
            TauSettingsSection(title = "Datos Personales", icon = Icons.Default.Badge) {
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it; profileStore.setUserName(it) },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = doeyFieldColors()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = userAge,
                    onValueChange = { userAge = it; profileStore.setUserAge(it) },
                    label = { Text("Edad") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = doeyFieldColors()
                )
                Spacer(Modifier.height(16.dp))
                Text("Nivel de uso tecnológico", fontSize = 12.sp, color = TauText3)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low" to "Bajo", "medium" to "Medio", "high" to "Alto").forEach { (id, label) ->
                        val sel = usageLevel == id
                        Surface(
                            onClick = { usageLevel = id; profileStore.setUsageLevel(id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (sel) TauAccent else TauSurface2
                        ) {
                            Text(label, color = if (sel) Color.White else TauText2, modifier = Modifier.padding(12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }

            // MODO DE USO
            TauSettingsSection(title = "Modo de Uso", icon = Icons.Default.SettingsSuggest) {
                ProfileOptionRow(
                    icon = Icons.Default.Elderly,
                    title = "Modo Básico",
                    subtitle = "Interfaz simplificada y guiada",
                    isSelected = userProfile == ProfileStore.PROFILE_BASIC,
                    onClick = { userProfile = ProfileStore.PROFILE_BASIC; profileStore.setUserProfile(ProfileStore.PROFILE_BASIC) }
                )
                Spacer(Modifier.height(8.dp))
                ProfileOptionRow(
                    icon = Icons.Default.Code,
                    title = "Modo Experto",
                    subtitle = "Control total y herramientas avanzadas",
                    isSelected = userProfile == ProfileStore.PROFILE_ADVANCED,
                    onClick = { userProfile = ProfileStore.PROFILE_ADVANCED; profileStore.setUserProfile(ProfileStore.PROFILE_ADVANCED) }
                )
            }
        }
    }
}

@Composable
fun ProfileOptionRow(icon: ImageVector, title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TauAccent.copy(alpha = 0.1f) else TauSurface2,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, TauAccent) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (isSelected) TauAccent else TauText3)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = if (isSelected) TauAccent else TauText1)
                Text(subtitle, fontSize = 11.sp, color = TauText3)
            }
        }
    }
}

@Composable
fun DoeyTextField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) }, placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        colors = doeyFieldColors(),
        shape = RoundedCornerShape(12.dp)
    )
}
