package com.doey.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoModeScreen(vm: MainViewModel) {
    val state by vm.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        // ── Barra superior minimalista ────────────────────────────────────────
        TopAppBar(
            title = { Text("Modo Auto", color = Color.White, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0D0D)),
            actions = {
                IconButton(onClick = { /* Salir de Modo Auto */ }) {
                    Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
                }
            }
        )

        // ── Contenido principal según tab ─────────────────────────────────────
        when (selectedTab) {
            0 -> AutoModeMainScreen(vm)
            1 -> AutoModeNotificationsScreen()
            2 -> AutoModeMusicScreen()
            3 -> AutoModeShortcutsScreen(vm)
        }

        // ── Barra de navegación inferior ──────────────────────────────────────
        NavigationBar(
            containerColor = Color(0xFF0D0D0D),
            modifier = Modifier.fillMaxWidth()
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Dashboard, null) },
                label = { Text("Panel", fontSize = 10.sp) },
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF6200EE),
                    selectedTextColor = Color(0xFF6200EE),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Notifications, null) },
                label = { Text("Notif.", fontSize = 10.sp) },
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF6200EE),
                    selectedTextColor = Color(0xFF6200EE),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.MusicNote, null) },
                label = { Text("Música", fontSize = 10.sp) },
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF6200EE),
                    selectedTextColor = Color(0xFF6200EE),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Apps, null) },
                label = { Text("Accesos", fontSize = 10.sp) },
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF6200EE),
                    selectedTextColor = Color(0xFF6200EE),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}

@Composable
private fun AutoModeMainScreen(vm: MainViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // ── Reloj grande ──────────────────────────────────────────────────────
        val now = remember { mutableStateOf(LocalDateTime.now()) }
        LaunchedEffect(Unit) {
            while (true) {
                now.value = LocalDateTime.now()
                kotlinx.coroutines.delay(1000)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                now.value.format(DateTimeFormatter.ofPattern("HH:mm")),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                now.value.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // ── Indicadores de conectividad ───────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IndicatorItem("WiFi", Icons.Default.Wifi)
            IndicatorItem("Bluetooth", Icons.Default.Bluetooth)
            IndicatorItem("Batería", Icons.Default.BatteryFull)
            IndicatorItem("Volumen", Icons.Default.VolumeUp)
        }

        // ── Botones de control rápido ─────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Controles Rápidos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickControlButton("WiFi", Icons.Default.Wifi, Modifier.weight(1f))
                QuickControlButton("Bluetooth", Icons.Default.Bluetooth, Modifier.weight(1f))
                QuickControlButton("Silencio", Icons.Default.VolumeOff, Modifier.weight(1f))
            }
        }

        // ── Próxima alarma/evento ─────────────────────────────────────────────
        Surface(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp)),
            color = Color(0xFF2A2A2A),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Schedule, null, tint = Color(0xFF6200EE), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Próxima alarma", color = Color.Gray, fontSize = 12.sp)
                    Text("Mañana a las 07:00", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun AutoModeNotificationsScreen() {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(5) { index ->
            NotificationCard(
                app = listOf("WhatsApp", "Gmail", "Telegram", "Instagram", "Twitter")[index],
                title = listOf(
                    "Juan: ¿Cómo estás?",
                    "Nuevo mensaje de trabajo",
                    "Recordatorio importante",
                    "Nuevo like en tu foto",
                    "Nuevo tweet de seguidor"
                )[index],
                time = listOf("Hace 2 min", "Hace 5 min", "Hace 15 min", "Hace 30 min", "Hace 1 hora")[index]
            )
        }
    }
}

@Composable
private fun AutoModeMusicScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ── Carátula del álbum ────────────────────────────────────────────────
        Surface(
            Modifier
                .size(200.dp)
                .background(Color(0xFF6200EE), CircleShape),
            color = Color(0xFF6200EE),
            shape = CircleShape
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Información de la canción ─────────────────────────────────────────
        Text(
            "Canción en Reproducción",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            "Artista Desconocido",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(24.dp))

        // ── Controles de reproducción ─────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF6200EE), CircleShape)
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Barra de progreso ─────────────────────────────────────────────────
        Column(Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = 0.35f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color(0xFF6200EE),
                trackColor = Color(0xFF2A2A2A)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1:45", fontSize = 10.sp, color = Color.Gray)
                Text("5:00", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun AutoModeShortcutsScreen(vm: MainViewModel) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(8) { index ->
            ShortcutButton(
                label = listOf(
                    "Llamar a Casa",
                    "Enviar WhatsApp",
                    "Abrir Spotify",
                    "Navegar a Trabajo",
                    "Enviar Email",
                    "Reproducir Podcast",
                    "Activar WiFi",
                    "Leer Notificaciones"
                )[index],
                icon = listOf(
                    Icons.Default.Phone,
                    Icons.Default.Chat,
                    Icons.Default.MusicNote,
                    Icons.Default.Navigation,
                    Icons.Default.Email,
                    Icons.Default.Podcasts,
                    Icons.Default.Wifi,
                    Icons.Default.Notifications
                )[index]
            )
        }
    }
}

@Composable
private fun IndicatorItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Icon(icon, null, tint = Color(0xFF6200EE), modifier = Modifier.size(24.dp))
        Text(label, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun QuickControlButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Button(
        onClick = {},
        modifier = modifier
            .height(48.dp)
            .background(Color(0xFF3A3A3A), RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A3A))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(label, fontSize = 9.sp, color = Color.White, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun NotificationCard(app: String, title: String, time: String) {
    Surface(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp)),
        color = Color(0xFF2A2A2A),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                Modifier
                    .size(40.dp)
                    .background(Color(0xFF6200EE), CircleShape),
                color = Color(0xFF6200EE),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(app.first().toString(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app, fontSize = 12.sp, color = Color.Gray)
                Text(title, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
            }
            Text(time, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun ShortcutButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Button(
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(icon, null, tint = Color(0xFF6200EE), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(label, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}
