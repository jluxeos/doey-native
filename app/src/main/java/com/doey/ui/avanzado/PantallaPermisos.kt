package com.doey.ui.avanzado

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.ui.core.*
import com.doey.servicios.basico.DoeyAccessibilityService
import com.doey.servicios.basico.NotificationAccessManager

@Composable
fun PermissionsScreen() {
    val ctx = LocalContext.current

    data class PermItem(val title: String, val desc: String, val granted: Boolean, val onGrant: () -> Unit)

    val items = listOf(
        PermItem("Servicio de Accesibilidad",
            "Necesario para la automatización de la interfaz (controlar otras apps).",
            DoeyAccessibilityService.isRunning()) {
            ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
        PermItem("Lector de Notificaciones",
            "Necesario para monitorear notificaciones entrantes y reaccionar automáticamente.",
            NotificationAccessManager.isAccessGranted(ctx)) {
            ctx.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
        PermItem("Micrófono",
            "Necesario para comandos de voz y detección de palabra de activación.",
            ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", ctx.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        }
    )

    Column(Modifier.fillMaxSize().background(TauBg)) {
        TopAppBar(
            title = { Text("Permisos", color = TauText1, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1)
        )

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items) { item ->
                ItemCard {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, color = TauText1, fontSize = 15.sp)
                            Text(item.desc, color = TauText3, fontSize = 12.sp)
                        }
                        Button(
                            onClick = item.onGrant,
                            enabled = !item.granted,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (item.granted) TauGreen.copy(0.2f) else TauAccent,
                                contentColor = if (item.granted) TauGreen else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (item.granted) "OK" else "Activar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
