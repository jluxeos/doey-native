package com.doey.ui

// AutoModeScreen.kt — Reemplazado por CarPlayModeContent en HomeScreen.kt
// Este archivo se mantiene vacío para no romper importaciones existentes.

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla de modo auto legacy — ya no se usa directamente.
 * El modo conducción ahora vive como CarPlayModeContent en HomeScreen.kt,
 * accesible desde el ícono del coche en la barra superior.
 */
@Composable
fun AutoModeScreen(vm: MainViewModel) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🚗", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Modo Conducción",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Toca el ícono 🚗 en la barra superior\npara activar el modo CarPlay.",
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
