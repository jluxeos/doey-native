package com.doey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsScreen(vm: MainViewModel) {
    val scope = rememberCoroutineScope()
    var showAddSkillDialog by remember { mutableStateOf(false) }

    val defaultSkills = remember {
        listOf(
            DefaultSkill("Control de Medios", "Controla la música, pausa, siguiente y anterior en cualquier app.", Icons.Default.MusicNote),
            DefaultSkill("Ajustes del Sistema", "Cambia el brillo, volumen, Wi-Fi y modo no molestar.", Icons.Default.Settings),
            DefaultSkill("Navegación y Mapas", "Abre rutas, busca lugares cercanos y gasolineras.", Icons.Default.Navigation),
            DefaultSkill("Comunicación", "Envía mensajes por WhatsApp, Telegram o haz llamadas por voz.", Icons.Default.Chat),
            DefaultSkill("Accesibilidad", "Controla la pantalla y realiza acciones mediante comandos de voz.", Icons.Default.Accessibility),
            DefaultSkill("Calendario y Alarmas", "Gestiona tus eventos, recordatorios y despiértate a tiempo.", Icons.Default.Alarm)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habilidades de Doey", color = TauText1, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1)
            )
        },
        containerColor = TauBg
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Habilidades Integradas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TauAccent,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    "Estas funciones están siempre activas y no requieren configuración adicional.",
                    color = TauText2,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(defaultSkills) { skill ->
                DefaultSkillCard(skill)
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Personalización",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TauAccent,
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
                    colors = ButtonDefaults.buttonColors(containerColor = TauAccent)
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
                        // Simulación de guardado
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
            color = TauSurface1,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Añadir Skill Avanzada", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TauText1)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la Skill") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = doeyFieldColors()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Código Markdown") },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    maxLines = 10,
                    colors = doeyFieldColors()
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = TauAccent) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name, content) },
                        colors = ButtonDefaults.buttonColors(containerColor = TauAccent)
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
        color = TauSurface1,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(TauAccent.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = skill.icon,
                    contentDescription = null,
                    tint = TauAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = skill.name,
                    fontWeight = FontWeight.Bold,
                    color = TauText1,
                    fontSize = 16.sp
                )
                Text(
                    text = skill.description,
                    color = TauText3,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Activo",
                tint = TauGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CreateSkillInfoCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = TauAccent.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, TauAccent.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = TauAccent,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "¿Quieres añadir más?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TauText1,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Doey es capaz de aprender nuevas habilidades simplemente describiéndolas. No necesitas programar APIs complejas; gracias a su control de accesibilidad y razonamiento, puedes pedirle que aprenda a usar cualquier aplicación instalada.",
                color = TauText2,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Para crear una skill, solo dile al asistente:\n\"Aprende a usar [Nombre de App] para [Acción]\"",
                fontWeight = FontWeight.Medium,
                color = TauAccent,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(TauAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            )
        }
    }
}
