package com.doey.ui.avanzado

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.ui.core.*
import com.doey.MainViewModel


@Composable
fun SkillsScreen(vm: MainViewModel) {

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

            item {
                Spacer(Modifier.height(24.dp))
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

