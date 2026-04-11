package com.doey.ui.avanzado

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.AplicacionDoey
import com.doey.agente.SkillInfo
import kotlinx.coroutines.launch
import com.doey.ui.core.*
import com.doey.MainViewModel


@Composable
fun SkillsScreen(vm: MainViewModel) {
    val state by vm.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var showAddSkillDialog by remember { mutableStateOf(false) }

    // Habilidades que ya vienen integradas y no dependen de APIs externas
    val defaultSkills = remember {
        listOf(
            DefaultSkill("Control de Medios", "Controla la música, pausa, siguiente y anterior en cualquier app.", CustomIcons.MusicNote),
            DefaultSkill("Ajustes del Sistema", "Cambia el brillo, volumen, Wi-Fi y modo no molestar.", CustomIcons.Settings),
            DefaultSkill("Navegación y Mapas", "Abre rutas, busca lugares cercanos y gasolineras.", CustomIcons.Navigation),
            DefaultSkill("Comunicación", "Envía mensajes por WhatsApp, Telegram o haz llamadas por voz.", CustomIcons.Chat),
            DefaultSkill("Accesibilidad", "Controla la pantalla y realiza acciones mediante comandos de voz.", CustomIcons.Accessibility),
            DefaultSkill("Calendario y Alarmas", "Gestiona tus eventos, recordatorios y despiértate a tiempo.", CustomIcons.Alarm)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habilidades de Doey", color = Label1Light, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1Light)
            )
        },
        containerColor = Surface0Light
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Sección: Habilidades Predeterminadas ──
            item {
                Text(
                    "Habilidades Integradas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Purple,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    "Estas funciones están siempre activas y no requieren configuración adicional.",
                    color = Label2Light,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(defaultSkills) { skill ->
                DefaultSkillCard(skill)
            }

            // ── Sección: Crear Skills ──
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Personalización",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Purple,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            item {
                CreateSkillInfoCard()
            }
            
            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showAddSkillDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    Text("Añadir Skill Avanzada", color = Color.White)
                }
            }
            
            item {
                Spacer(Modifier.height(24.dp))
            }
        }
        
        if (showAddSkillDialog) {
            AddSkillDialog(
                onDismiss = { showAddSkillDialog = false },
                onSave = { name, content ->
                    scope.launch {
                        vm.getSettings().saveCustomSkill(name, content)
                        showAddSkillDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun AddSkillDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Surface1Light,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Añadir Skill Avanzada", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Label1Light)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la Skill") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Código Markdown") },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    maxLines = 10
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = Purple) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name, content) },
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) { Text("Guardar", color = Color.White) }
                }
            }
        }
    }
}

data class DefaultSkill(val name: String, val description: String, val icon: ImageVector)

@Composable
fun DefaultSkillCard(skill: DefaultSkill) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Surface1Light,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Purple.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = skill.icon,
                    contentDescription = null,
                    tint = Purple,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = skill.name,
                    fontWeight = FontWeight.Bold,
                    color = Label1Light,
                    fontSize = 16.sp
                )
                Text(
                    text = skill.description,
                    color = Label3Light,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Icon(
                imageVector = CustomIcons.CheckCircle,
                contentDescription = "Activo",
                tint = Color(0xFF34C759),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CreateSkillInfoCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Purple.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Purple.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = CustomIcons.Code,
                contentDescription = null,
                tint = Purple,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "¿Quieres añadir más?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Label1Light,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Doey es capaz de aprender nuevas habilidades simplemente describiéndolas. No necesitas programar APIs complejas; gracias a su control de accesibilidad y razonamiento, puedes pedirle que aprenda a usar cualquier aplicación instalada.",
                color = Label2Light,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Para crear una skill, solo dile al asistente:\n\"Aprende a usar [Nombre de App] para [Acción]\"",
                fontWeight = FontWeight.Medium,
                color = Purple,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(Purple.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            )
        }
    }
}
