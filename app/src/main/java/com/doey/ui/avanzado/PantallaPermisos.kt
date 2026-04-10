package com.doey.ui.avanzado

import com.doey.ui.core.*
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
